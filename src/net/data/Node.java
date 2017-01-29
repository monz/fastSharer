package net.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Node {
    private UUID id;
    private Set<String> ips;
    private long lastTimeSeen;

    public Node(UUID id, String ip) {
        this.id = id;
        this.ips = new HashSet<>();
        this.ips.add(ip);
    }

    public UUID getId() {
        return id;
    }

    synchronized public Set<String> getIps() {
        return ips;
    }

    synchronized public void addIps(Set<String> newNodeIps) {
        if (newNodeIps == null || newNodeIps.isEmpty()) {
            return;
        }
        // remove duplicates
        newNodeIps.removeAll(newNodeIps);
        if (newNodeIps.size() > 0) {
            ips.addAll(newNodeIps);
        }
    }

    synchronized public long getLastTimeSeen() {
        return lastTimeSeen;
    }

    synchronized public void setLastTimeSeen(long lastTimeSeen) {
        this.lastTimeSeen = lastTimeSeen;
    }

    synchronized public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("IPs: ");
        s.append(String.join(", ", ips));

        return s.toString();
    }
}
