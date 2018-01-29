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

package data;

import com.google.gson.annotations.Expose;
import local.ServiceLocator;
import local.decl.Observable;
import local.decl.Observer;
import local.impl.ObserverCmd;
import net.NetworkService;
import net.data.Pair;
import net.data.ReplicaNode;
import ui.controller.ChunkDownloadProgressController;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SharedFile implements Observable<FileMetadata> {
    private static final NetworkService NETWORK_SERVICE = (NetworkService) ServiceLocator.getInstance().getService(ServiceLocator.NETWORK_SERVICE);

    @Expose private FileMetadata metadata;
    @Expose private Map<UUID, ReplicaNode> replicaNodes = new HashMap<>(); // map<nodeId, list<chunkMD5>>

    private List<Observer<FileMetadata>> observers = new CopyOnWriteArrayList<>(); // prevents "ConcurrentModificationException" http://stackoverflow.com/questions/19197579/java-observer-pattern-how-to-remove-observers-during-updatenotify-loop-itera
    private boolean downloadActive;

    public SharedFile() {
        // empty constructor is required for GSON
        // without this constructor the class member wont get initialized
        // e.g. replica nodes is null
    }

    /**
     * Create a SharedFile object for local files
     * @param metadata
     */
    public SharedFile(FileMetadata metadata) {
        this.metadata = metadata;
    }

    public String getFilePath() {
        return metadata.getFilePath();
    }

    public void setFilePath(String filePath) {
        metadata.setFilePath(filePath);
    }

    public FileMetadata getMetadata() {
        return metadata;
    }

    public String getFilename() {
        return metadata.getFileName();
    }

    public String getChecksum() {
        return metadata.getChecksum();
    }

    synchronized public boolean isLocal() {
        boolean isLocal;
        int expectedChunkCount = Chunk.getChunkCount(metadata.getFileSize());
        int actualChunkCount = metadata.getChunks().size();
        if (actualChunkCount > 0 && actualChunkCount == expectedChunkCount) {
            isLocal = metadata.getChunks().stream().allMatch(Chunk::isLocal);
        } else {
            isLocal = false;
        }

        return isLocal;
    }

    public String getFileId() {
        return metadata.getFileId();
    }

    synchronized public Map<UUID, ReplicaNode> getReplicaNodes() {
        return replicaNodes;
    }

    synchronized public void addReplicaNode(UUID nodeId, ReplicaNode node) {
        Set<String> chunkChecksums = node.getChunks();
        // ignore invalid data
        // ignore unknown nodes
        if (nodeId == null
            || chunkChecksums == null
            || (NETWORK_SERVICE.getNode(nodeId) == null && ! NETWORK_SERVICE.getLocalNodeId().equals(nodeId))) {
            return;
        }
        ReplicaNode n = replicaNodes.putIfAbsent(nodeId, node);

        // add only new chunk checksums
        if (n != null) {
            Set<String> currentChecksums = n.getChunks();

            // remove null references and empty checksums
            chunkChecksums.removeIf(s ->  s == null || s.isEmpty());

            chunkChecksums.removeAll(currentChecksums); // remove duplicates
            if (chunkChecksums.size() > 0) {
                currentChecksums.addAll(chunkChecksums);
            }
        }
    }

    synchronized public void addReplicaNode(UUID nodeId, List<String> chunkChecksums, boolean isComplete) {
        addReplicaNode(nodeId, new ReplicaNode(nodeId, chunkChecksums, isComplete));
    }

    synchronized public List<Chunk> getChunksToDownload() {
        if (metadata.getChunks() == null || isLocal()) {
            return Collections.emptyList();
        }

        return metadata.getChunks().stream()
            .filter(c -> ! c.isLocal() && ! c.isDownloadActive() && c.getChecksum() != null)
            .collect(Collectors.toList());
    }

    synchronized public List<Chunk> getActiveDownloadingChunks() {
        if (metadata.getChunks() == null || isLocal()) {
            return Collections.emptyList();
        }

        return metadata.getChunks().stream()
            .filter(c -> c.isDownloadActive())
            .collect(Collectors.toList());
    }

    synchronized public List<UUID> getReplicaNodesByChunk(String chunkChecksum) {
        return replicaNodes.entrySet().stream()
            .filter(e -> e.getValue().contains(chunkChecksum))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    synchronized public List<String> getChunksOfReplicaNode(UUID nodeId) {
        return new ArrayList<>(replicaNodes.get(nodeId).getChunks());
    }

    public Chunk getChunk(String chunkChecksum) {
        return metadata.getChunks().stream()
            .filter(c -> c.getChecksum() != null && c.getChecksum().equals(chunkChecksum))
            .findFirst().orElse(null);
    }

    public long getFileSize() {
        return metadata.getFileSize();
    }

    synchronized public boolean activateDownload() {
        boolean success;
        if (downloadActive) {
            success = false;
        } else {
            downloadActive = true;
            success = true;
        }
        return success;
    }

    synchronized public boolean deactivateDownload() {
        boolean success;
        if (downloadActive) {
            downloadActive = false;
            success = true;
        } else {
            success = false;
        }
        return success;
    }

    synchronized public boolean isDownloadActive() {
        return downloadActive;
    }

    synchronized public void removeReplicaNode(UUID nodeId) {
        replicaNodes.remove(nodeId);
    }

    private List<UUID> getNextDownloadNodes() {
        // get nodes which share the chunks remaining for download
        // sorted by available chunk count, ascending
        // get random node within the 5 lowest number of available chunks
        return getChunksToDownload().stream()
            .flatMap(c -> getReplicaNodesByChunk(c.getChecksum()).stream())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public Pair<UUID, Chunk> getNextChunkToDownload(int round) {
        List<UUID> nodeIds = getNextDownloadNodes();
        if (nodeIds.size() == 0) {
            return null;
        }
        // chose node, round robin
        UUID nodeId;
        nodeId = nodeIds.get(round % nodeIds.size());

        // convert chunksToDownload to checksum list
        List<String> chunksToDownload = getChunksToDownload().stream()
            .map(Chunk::getChecksum)
            .collect(Collectors.toList());

        // randomly chose a chunk from this node which gets downloaded next
        // this chunk must be out of the list of chunks to download
        List<String> allNodeChunks = getChunksOfReplicaNode(nodeId);
        allNodeChunks.retainAll(chunksToDownload);
        Chunk chunk = null;
        if (allNodeChunks.size() > 0) {
            chunk = getChunk(allNodeChunks.get(new Random().nextInt(allNodeChunks.size())));
        }

        return new Pair<>(nodeId, chunk);
    }

    synchronized public void resetReplicaNodes() {
        replicaNodes.clear();
    }

    public List<String> getAllChunkChecksums() {
        return metadata.getChunks().stream().map(Chunk::getChecksum).collect(Collectors.toList());
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
    public void notifyObservers(FileMetadata data) {
        notifyObservers(data, ObserverCmd.ADD);
    }

    @Override
    public void notifyObservers(FileMetadata data, ObserverCmd cmd) {
        observers.forEach(o -> o.update(data, cmd));
    }
}
