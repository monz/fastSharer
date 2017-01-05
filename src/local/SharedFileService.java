package local;

import data.FileMetadata;
import data.SharedFile;
import local.decl.Observable;
import local.decl.Observer;
import local.impl.FileChecksumObserver;
import local.impl.ObserverCmd;
import ui.controller.ChunkProgressController;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SharedFileService implements Observable<SharedFile> {
    private static final Logger log = Logger.getLogger(SharedFileService.class.getName());

    private List<Observer<SharedFile>> observers = new ArrayList<>(); // on "ConcurrentModificationException" use CopyOnWriteArrayList, see SharedFile
    private Map<String, SharedFile> sharedFiles = new HashMap<>();

    public void addToSharedFiles(FileMetadata metadata) {
        if (metadata == null) {
            return;
        }
        log.info(String.format("Added: '%s' to shared files", metadata.getFileName()));

        SharedFile sharedFile = new SharedFile(metadata);
        sharedFiles.put(metadata.getFileId(), sharedFile);

        // register observers for shared file
        sharedFile.addObserver(new FileChecksumObserver());
        sharedFile.addObserver(new ChunkProgressController(metadata.getFileId()));

        // update "shared files service" observers
        notifyObservers(sharedFile);
    }

    public String getFilePath(String fileId) {
        return sharedFiles.get(fileId).getFilePath();
    }

    synchronized public SharedFile getFile(String fileId) {
        return sharedFiles.get(fileId);
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
    public void notifyObservers(SharedFile data) {
        notifyObservers(data, ObserverCmd.ADD);
    }

    @Override
    public void notifyObservers(SharedFile data, ObserverCmd cmd) {
        observers.forEach(o -> o.update(data, cmd));
    }

    boolean isFileShared(String fileId) {
        return sharedFiles.get(fileId) != null;
    }

    boolean isFileShared(File file) {
        // check whether a file with given path exists already
        return sharedFiles.values().stream().anyMatch(sf -> sf.getFilePath().equals(file.getAbsolutePath()));
    }
}
