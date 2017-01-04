package local;

import data.FileMetadata;
import data.SharedFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SharedFileService implements Observable<SharedFile> {
    private static final Logger log = Logger.getLogger(SharedFileService.class.getName());

    private List<Observer> observers = new ArrayList<>();
    private Map<String, SharedFile> sharedFiles = new HashMap<>();

    public void addToSharedFiles(FileMetadata metadata) {
        if (metadata == null) {
            return;
        }
        log.info(String.format("Added: '%s' to shared files", metadata.getFileName()));

        SharedFile sharedFile = new SharedFile(metadata);
        sharedFiles.put(metadata.getFileId(), sharedFile);

        // update observers
        notifyObservers(sharedFile);
    }

    public String getFilePath(String fileId) {
        return sharedFiles.get(fileId).getFilePath();
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
        observers.forEach(o -> o.update(data));
    }
}
