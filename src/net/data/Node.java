package net.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

public class Node {
    private static final Logger log = Logger.getLogger(Node.class.getName());

    private UUID id;
    private Set<String> ips;
    private long lastTimeSeen;
    private int usageCount;

    public Node(UUID id, String ip) {
        this.id = id;
        this.ips = new HashSet<>();
        this.ips.add(ip);
    }

    public Node(UUID id) {
        this.id = id;
        this.ips = new HashSet<>();
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

    synchronized public int incUsageCount() {
        usageCount++;
        log.info("Increment usage counter of node: " + id + " to: " + usageCount);

        return usageCount;
    }

    synchronized public int decUsageCount() {
        if (--usageCount < 0) {
            log.severe("Tried to decrement node's usage counter below zero (0)!");
            usageCount = 0;
        } else {
            log.info("Decremented usage counter of node: " + id + " to: " + usageCount);
        }
        return usageCount;
    }

    synchronized public int getUsageCount() {
        return usageCount;
    }

    synchronized public String toString() {
        StringBuffer s = new StringBuffer();
        s.append("IPs: ");
        s.append(String.join(", ", ips));

        return s.toString();
    }
}
