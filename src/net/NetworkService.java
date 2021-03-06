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

import com.google.gson.TypeAdapter;
import local.ServiceLocator;
import local.SharedFileService;
import local.decl.NodeStateListener;
import net.data.Node;
import net.data.ShareCommand;
import net.decl.FailCallback;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkService {
    public static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");

    private static final Logger log = Logger.getLogger(NetworkService.class.getName());
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);

    private static final int SOCKET_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);
    private static final UUID LOCAL_NODE_ID = UUID.randomUUID();

    private ExecutorService threadPool;
    private Map<UUID, Node> nodes;
    private List<NodeStateListener> nodeStateListeners;
    private int cmdPort;

    public NetworkService(int cmdPort) {
        this.threadPool = Executors.newSingleThreadExecutor(); // todo: make thread count configurable
        this.nodes = new HashMap<>();
        this.nodeStateListeners = new ArrayList<>();
        this.cmdPort = cmdPort;
    }

    public void addNodeStateListener(NodeStateListener listener) {
        if (listener == null || nodeStateListeners.contains(listener)) {
            return;
        }
        nodeStateListeners.add(listener);
    }

    synchronized public void broadcast(ShareCommand<?> shareListMsg) {
        // broadcast shared files to all nodes
        log.info("Send shared files to following nodes: " + nodes.values());
        for (Node n : nodes.values()) {
            sendCommand(shareListMsg, n);
        }
    }

    synchronized public void sendCommand(ShareCommand cmd, Node n) {
        sendCommand(cmd, n, null);
    }

    synchronized public void sendCommand(ShareCommand cmd, Node n, FailCallback failCallback) {
        sendCommand(cmd, n, null, failCallback);
    }

    synchronized private void sendCommand(ShareCommand cmd, Node n, TypeAdapter serializer, FailCallback failCallback) {
        threadPool.execute(() -> {
            // check if remote node was already discovered
            if (n == null || nodes.get(n.getId()) == null) {
                log.warning("Node '" + (n == null ? "null" : n.getId()) + "' not found.");
                if (failCallback != null) failCallback.fail();
                return;
            }

            // send to all known ip addresses of each host
            Set<String> ips;
            if (n.getIps().size() > 0) {
                ips = n.getIps();
            } else {
                ips = nodes.get(n.getId()).getIps();
            }

            Socket s;
            BufferedWriter out;
            boolean successfullySend = false;
            // establish tcp connection
            for (String ip : ips) {
                try {
                    s = nodes.get(n.getId()).connect(ip, cmdPort, SOCKET_TIMEOUT);
                } catch(SocketTimeoutException e) {
                    log.log(Level.WARNING, "Could not send command: " + cmd.serialize(serializer) + " to: " + ip, e);
                    // remove unreachable nodes
                    removeNode(n);
                    successfullySend = false;
                    break;
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Could not create new connection with IP: " + ip + " and port: " + cmdPort, e);
                    // remove unreachable nodes
                    removeNode(n);
                    successfullySend = false;
                    break;
                }

                // send share command
                log.fine("Send cmd: '" + cmd.serialize(serializer) + "' to IP: " + s.getInetAddress().getHostAddress());
                log.info("Send cmd: '<cmdNotHere>' to IP: " + s.getInetAddress().getHostAddress());
                try {
                    out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), PROTOCOL_CHARSET));
                    out.write(cmd.serialize(serializer));
                    out.newLine();
                    out.flush();
                    successfullySend = true;
                    log.fine("Command was actually sent!");
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Could not send command: " + cmd.serialize(serializer) + " to: " + s.getInetAddress().getHostAddress(), e);
                    // remove unreachable nodes
                    removeNode(n);
                    successfullySend = false;
                    break;
                }
            }
            if (!successfullySend && failCallback != null) {
                failCallback.fail();
            }
        });
    }

    synchronized boolean addNode(Node newNode) {
        UUID newNodeId = newNode.getId();

        boolean isNewNode;
        if (nodes.containsKey(newNodeId)) {
            Node node = nodes.get(newNodeId);
            // set keepAlive timer
            node.setLastTimeSeen(System.currentTimeMillis());
            // update missing ip addresses
            Set<String> ips = node.getIps();
            if (ips.containsAll(newNode.getIps())) {
                log.info("Node already contains all ip addresses received");
                // gui update not required
            } else {
                node.addIps(newNode.getIps());
                // update gui
                nodeStateListeners.forEach(l -> l.addNode(node));
            }
            isNewNode =  false;
        } else {
            // set keepAlive timer
            newNode.setLastTimeSeen(System.currentTimeMillis());

            nodes.put(newNodeId, newNode);

            // update gui
            nodeStateListeners.forEach(l -> l.addNode(newNode));

            isNewNode = true;
        }

        return isNewNode;
    }

    synchronized void removeNode(Node node) {
        log.info("Removed node '" + node.getId() + "'");

        // clear node from shared files
        SHARED_FILE_SERVICE.removeNodeFromReplicaNodes(node.getId());

        nodes.remove(node.getId());

        // update gui
        nodeStateListeners.forEach(l -> l.removeNode(node));
    }

    public UUID getLocalNodeId() {
        return LOCAL_NODE_ID;
    }

    synchronized public Map<UUID, Node> getAllNodes() {
        return nodes;
    }

    synchronized public Node getNode(UUID nodeId) {
        return nodes.get(nodeId);
    }
}