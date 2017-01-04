package data;

import local.decl.Observable;
import local.decl.Observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SharedFile implements Observable<FileMetadata> {
    private FileMetadata metadata;
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
        observers.forEach(o -> o.update(data));
    }

    public FileMetadata getMetadata() {
        return metadata;
    }
}
