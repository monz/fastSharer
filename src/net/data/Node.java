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

    public Set<String> getIps() {
        return ips;
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
