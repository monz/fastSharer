/*
 * Copyright (c) 2017. Markus Monz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net;

import local.ServiceLocator;
import net.data.Node;
import net.decl.Service;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DiscoveryService implements Service {
    public static final int HELLO_MSG_SIZE = 36;

    private static final Logger log = Logger.getLogger(DiscoveryService.class.getName());
    private static final NetworkService NETWORK_SERVICE = (NetworkService) ServiceLocator.getInstance().getService(ServiceLocator.NETWORK_SERVICE);
    private static final long CLEAN_TIMEOUT = TimeUnit.MILLISECONDS.convert(60, TimeUnit.SECONDS);

    private UUID id;
    private DatagramSocket s;
    private Thread receiver;
    private ScheduledExecutorService sender;
    private ScheduledExecutorService nodeCleaner;
    private List<String> localIps;

    private long initialDelay;
    private long period;

    public DiscoveryService(int servicePort, long initialDelay, long period) throws SocketException {
        this.s = new DatagramSocket(servicePort);
        this.s.setBroadcast(true);
        this.id = NETWORK_SERVICE.getLocalNodeId();
        this.initialDelay = initialDelay;
        this.period = period;
        this.receiver = new Thread(receiveHello);
        this.sender = Executors.newSingleThreadScheduledExecutor();
        this.nodeCleaner = Executors.newSingleThreadScheduledExecutor();
        this.localIps = extractLocalAddresses();
    }

    private List<String> extractLocalAddresses() {
        Enumeration<NetworkInterface> interfaces;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();

            List<String> ips = new ArrayList<>();
            while(interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue; // Don't want to broadcast to the loopback interface
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    ips.add(interfaceAddress.getAddress().getHostAddress());
                }
            }

            return ips;
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        try {
            receiver.start();
            sender.scheduleAtFixedRate(sendHello, initialDelay, period, TimeUnit.MILLISECONDS);
            nodeCleaner.scheduleAtFixedRate(cleanUpNodes, initialDelay, period, TimeUnit.MILLISECONDS);
        } catch (IllegalThreadStateException e) {
            // gets thrown when trying to start service twice
            log.log(Level.WARNING, "Could not start discovery service", e);
        }
    }

    @Override
    public void stop() {
        receiver.interrupt();
        sender.shutdown();
    }

    private Runnable receiveHello = new Runnable() {
        @Override
        public void run() {
            while (!receiver.isInterrupted()) {
                try {
                    Node node = extractNodeFromMessage();
                    if (node == null) {
                        continue;
                    }

                    if ( id.equals(node.getId()) ) {
                        // ignore own node id
                        continue;
                    } else {
                        // only add foreign nodes
                        NETWORK_SERVICE.addNode(node);

                        // if was new node, send all files shared to new node

                        // update gui
                        // due to overview observer
                    }
                    log.info("received: '" + node.getId() + "' from " + node.getIps());
                } catch (IOException e) {
                    log.log(Level.WARNING, "Could not extract node from discovery message.", e);
                    continue;
                }  catch (Exception e) {
                    log.log(Level.SEVERE, "Ooops!", e);
                }
            }
        }
    };

    private Runnable sendHello = new Runnable() {
        @Override
        public void run() {
            try {
                byte[] sendData = id.toString().getBytes();

                try {
                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

                    while (interfaces.hasMoreElements()) {
                        NetworkInterface networkInterface = interfaces.nextElement();

                        if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                            continue; // Don't want to broadcast to the loopback interface
                        }
                        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                            InetAddress broadcast = interfaceAddress.getBroadcast();

                            if (broadcast == null) {
                                continue;
                            }
                            // Send the broadcast package
                            try {
                                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcast, s.getLocalPort());
                                s.send(sendPacket);
                            } catch (IOException e) {
                                log.log(Level.SEVERE, "Could not send discovery message.", e);
                                continue;
                            }
                            log.info(">>> ShareCommandMessage packet sent to: " + broadcast.getHostAddress() + "; Interface: " + networkInterface.getDisplayName());
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

                log.info("Sent id: " + id);
            } catch (Exception e) {
                log.log(Level.SEVERE, "Ooops!", e);
            }
        }
    };

    private Runnable cleanUpNodes = () -> {
        try {
            // first filter nodes, afterwards remove from nodesList
            // do not iterate through node list while removing nodes, simultaneously
            // get expired nodes
            List<Node> expiredNodes = NETWORK_SERVICE.getAllNodes().values().stream()
                .filter(n -> {
                    boolean expired = (n.getLastTimeSeen() + CLEAN_TIMEOUT) < System.currentTimeMillis();
                    log.info("Expired: " + expired + " Node: " + n.getId() + " last seen: " + n.getLastTimeSeen() + " timeout in: " + (n.getLastTimeSeen() + CLEAN_TIMEOUT - System.currentTimeMillis()));
                    return expired;
                }).collect(Collectors.toList());

            // remove nodes
            expiredNodes.forEach(NETWORK_SERVICE::removeNode);
        }catch (Exception e) {
            log.log(Level.SEVERE, "Ooops!", e);
        }
    };

    private Node extractNodeFromMessage() throws IOException {
        byte[] helloMsg = new byte[HELLO_MSG_SIZE];

        DatagramPacket packet = new DatagramPacket(helloMsg, HELLO_MSG_SIZE);
        s.receive(packet);

        // for debug: if message was received from local ip, skip node
        Node node = null;
        if (! localIps.contains(packet.getAddress().getHostAddress())) {
            node = new Node(UUID.fromString(byteToString(helloMsg)), packet.getAddress().getHostAddress());
        }

        return node;
    }

    private String byteToString(byte[] msg) {
        return new String(msg, StandardCharsets.US_ASCII);
    }
}
