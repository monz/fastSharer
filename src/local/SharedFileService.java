package local;

import data.FileMetadata;
import data.SharedFile;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SharedFileService implements Observable<SharedFile> {
    private static final Logger log = Logger.getLogger(SharedFileService.class.getName());

    private List<Observer> observers = new ArrayList<>();
    private List<SharedFile> sharedFiles = new ArrayList<>();

    public void addToSharedFiles(FileMetadata metadata) {
        if (metadata == null) {
            return;
        }
        log.info(String.format("Added: '%s' to shared files", metadata.getFileName()));

        SharedFile sharedFile = new SharedFile(metadata);
        sharedFiles.add(sharedFile);

        // update observers
        notifyObservers(sharedFile);
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
