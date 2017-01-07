package data;

import com.google.gson.annotations.Expose;
import local.decl.Observable;
import local.decl.Observer;
import local.impl.ObserverCmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class SharedFile implements Observable<FileMetadata> {
    @Expose private FileMetadata metadata;
    @Expose private Map<UUID, List<String>> replicaNodes = new HashMap<>(); // map<nodeId, list<chunkMD5>>

    private boolean isLocal;

    private List<Observer<FileMetadata>> observers = new CopyOnWriteArrayList<>(); // prevents "ConcurrentModificationException" http://stackoverflow.com/questions/19197579/java-observer-pattern-how-to-remove-observers-during-updatenotify-loop-itera

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

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    public String getFileId() {
        return metadata.getFileId();
    }

    public Map<UUID, List<String>> getReplicaNodes() {
        return replicaNodes;
    }

    public void addReplicaNode(UUID nodeId, List<String> chunkChecksums) {
        if (chunkChecksums == null || chunkChecksums.size() < 1) {
            return;
        }
        List<String> currentChecksums = replicaNodes.putIfAbsent(nodeId, chunkChecksums);

        // add only new chunk checksums
        if (currentChecksums != null) {
            chunkChecksums.removeAll(currentChecksums); // remove duplicates
            if (chunkChecksums.size() > 0) {
                currentChecksums.addAll(chunkChecksums);
            }
        }
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
