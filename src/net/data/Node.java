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

package net.data;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Node {
    private UUID id;
    private Set<String> ips;
    private long lastTimeSeen;
    private Socket socket;

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
        newNodeIps.removeAll(ips);
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
        return "IPs: " + String.join(", ", ips);
    }

    synchronized public Socket connect(String ip, int port, int timeout) throws IOException {
        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
        }
        return socket;
    }
}
