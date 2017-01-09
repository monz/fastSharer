package data;

import com.google.gson.annotations.Expose;
import local.ServiceLocator;
import local.decl.Observable;
import local.decl.Observer;
import local.impl.ObserverCmd;
import net.NetworkService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SharedFile implements Observable<FileMetadata> {
    private static final NetworkService NETWORK_SERVICE = (NetworkService) ServiceLocator.getInstance().getService(ServiceLocator.NETWORK_SERVICE);

    @Expose private FileMetadata metadata;
    @Expose private Map<UUID, List<String>> replicaNodes = new HashMap<>(); // map<nodeId, list<chunkMD5>>

    private boolean isLocal;

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
        this(metadata, true);
    }

    public SharedFile(FileMetadata metadata, boolean isLocal) {
        this.metadata = metadata;
        this.isLocal = isLocal;
    }

    synchronized public String getFilePath() {
        return metadata.getFilePath();
    }

    synchronized public void setFilePath(String filePath) {
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
        return isLocal;
    }

    synchronized public void setLocal(boolean local) {
        isLocal = local;
    }

    public String getFileId() {
        return metadata.getFileId();
    }

    synchronized public Map<UUID, List<String>> getReplicaNodes() {
        return replicaNodes;
    }

    synchronized public void addReplicaNode(UUID nodeId, List<String> chunkChecksums) {
        // ignore invalid data
        // ignore unknown nodes
        if (nodeId == null
            || chunkChecksums == null
            || chunkChecksums.size() < 1
            || (NETWORK_SERVICE.getNode(nodeId) == null && ! NETWORK_SERVICE.getLocalNodeId().equals(nodeId))) {
            return;
        }
        List<String> currentChecksums = replicaNodes.putIfAbsent(nodeId, chunkChecksums);

        // remove null references and empty checksums
        chunkChecksums.removeIf(s ->  s == null || s.isEmpty());

        // add only new chunk checksums
        if (currentChecksums != null) {
            chunkChecksums.removeAll(currentChecksums); // remove duplicates
            if (chunkChecksums.size() > 0) {
                currentChecksums.addAll(chunkChecksums);
            }
        }
    }

    synchronized public List<Chunk> getChunksToDownload() {
        if (metadata.getChunks() == null || isLocal) {
            return null;
        }

        return metadata.getChunks().stream()
            .filter(c -> ! c.isLocal())
            .collect(Collectors.toList());
    }

    synchronized public List<UUID> getReplicaNodesByChunk(String chunkChecksum) {
        return replicaNodes.entrySet().stream()
            .filter(e -> e.getValue().contains(chunkChecksum))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    synchronized public Chunk getChunk(String chunkChecksum) {
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

    public UUID getNextDownloadNodeId() {
        // get nodes which share the chunks remaining for download
        // sorted by available chunk count, ascending
        // get random node within the 5 lowest number of available chunks
        return getChunksToDownload().stream()
            .flatMap(c -> getReplicaNodesByChunk(c.getChecksum()).stream())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .limit(5) // todo: create constant
            .map(Map.Entry::getKey)
            .findAny().orElse(null);
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
