package net;

import com.google.gson.TypeAdapter;
import local.ServiceLocator;
import local.SharedFileService;
import local.decl.Observable;
import local.decl.Observer;
import local.impl.ObserverCmd;
import net.data.Node;
import net.data.ShareCommand;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkService implements Observable<Node> {
    public static final Charset PROTOCOL_CHARSET = Charset.forName("UTF-8");

    private static final Logger log = Logger.getLogger(NetworkService.class.getName());
    private static final int SOCKET_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS);
    private static final UUID LOCAL_NODE_ID = UUID.randomUUID();

    private ExecutorService threadPool;
    private Map<UUID, Node> nodes;
    private int cmdPort;
    private int maxIncomingConnections;
    private int maxOutgoingConnections;

    private ArrayList<Observer<Node>> observers = new ArrayList<>();

    public NetworkService(int cmdPort, int maxIncomingConnections, int maxOutgoingConnections) {
        this.threadPool = Executors.newSingleThreadExecutor(); // todo: make thread count configurable
        this.nodes = new HashMap<>();
        this.cmdPort = cmdPort;
        this.maxIncomingConnections = maxIncomingConnections;
        this.maxOutgoingConnections = maxOutgoingConnections;
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

    synchronized private void sendCommand(ShareCommand cmd, Node n, TypeAdapter serializer) {
        threadPool.execute(() -> {
            // check if remote node was already discovered
            if (n == null || nodes.get(n.getId()) == null) {
                log.warning("Node '" + (n == null ? "null" : n.getId()) + "' not found.");
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
            // establish tcp connection
            for (String ip : ips) {
                try {
                    s = new Socket();
                    s.connect(new InetSocketAddress(ip, cmdPort), SOCKET_TIMEOUT);
                } catch(SocketTimeoutException e) {
                    log.log(Level.WARNING, "Could not send command: " + cmd.serialize(serializer) + " to: " + ip, e);
                    // remove unreachable nodes
                    removeNode(n);
                    continue;
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Could not create new connection with IP: " + ip + " and port: " + cmdPort, e);
                    // remove unreachable nodes
                    removeNode(n);
                    continue;
                }

                // send share command
                log.info("Send cmd: '" + cmd.serialize(serializer) + "' to IP: " + s.getInetAddress().getHostAddress());
                try {
                    out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), PROTOCOL_CHARSET));
                    out.write(cmd.serialize(serializer));
                    out.flush();
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Could not send command: " + cmd.serialize(serializer) + " to: " + s.getInetAddress().getHostAddress(), e);
                    // remove unreachable nodes
                    removeNode(n);
                    continue;
                } finally {
                    try {
                        if (s != null) {
                            s.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
        });
    }

    synchronized public boolean addNode(Node newNode) {
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
                ips.addAll(newNode.getIps());
                // update gui
                notifyObservers(node);
            }
            isNewNode =  false;
        } else {
            // set keepAlive timer
            newNode.setLastTimeSeen(System.currentTimeMillis());

            nodes.put(newNodeId, newNode);

            // update gui
            notifyObservers(newNode);

            isNewNode = true;
        }

        return isNewNode;
    }

    synchronized public void removeNode(Node node) { //TODO: rewrite to use parameter UUID nodeId
        log.info("Removed node '" + node.getId() + "'");

        // clear node from shared files
        SharedFileService sharedFileService = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);
        //sharedFileService.removeNodeFromReplicaNodes(node.getId());

        // todo: implement new services
        /*
        // remove all nodes from scheduleDownloadRequest queue
        List<DownloadRequestJob> badTasks = downloadQueue.stream()
                .filter(dlTask -> node.getNodeId().equals(dlTask.getNodeId()))
                .collect(Collectors.toList());
        log.info("Remove invalid node's tasks from scheduleDownloadRequest queue: " + badTasks);
        // reset chunks attributes
        badTasks.forEach(task -> task.getChunk().deactivateDownload());
        downloadQueue.removeAll(badTasks);

        */

        nodes.remove(node.getId());

        // update gui
        notifyObservers(node, ObserverCmd.DELETE);
    }

    synchronized public UUID getLocalNodeId() {
        return LOCAL_NODE_ID;
    }

    synchronized public Map<UUID, Node> getAllNodes() {
        return nodes;
    }

    synchronized public Node getNode(UUID nodeId) {
        return nodes.get(nodeId);
    }

    synchronized public int getActiveUploads() {
        // TODO: implement
        return -1;
    }

    synchronized public boolean allowNewConnection() {
        return getActiveUploads() <= maxOutgoingConnections;
    }

    @Override
    public void addObserver(Observer observer) {
        if (observers.contains(observer)) {
            return;
        }
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(Node data) {
        notifyObservers(data, ObserverCmd.ADD);
    }

    @Override
    public void notifyObservers(Node data, ObserverCmd cmd) {
        observers.forEach(o -> o.update(data, cmd));
    }
}