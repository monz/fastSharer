package data;

import com.google.gson.annotations.Expose;
import local.decl.Observable;
import local.decl.Observer;
import local.impl.ObserverCmd;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SharedFile implements Observable<FileMetadata> {
    @Expose private FileMetadata metadata;
    private List<Observer<FileMetadata>> observers = new CopyOnWriteArrayList<>(); // prevents "ConcurrentModificationException" http://stackoverflow.com/questions/19197579/java-observer-pattern-how-to-remove-observers-during-updatenotify-loop-itera

    public SharedFile(FileMetadata metadata) {
        this.metadata = metadata;
    }

    public String getFilePath() {
        return metadata.getFilePath();
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

    public FileMetadata getMetadata() {
        return metadata;
    }
}
