package local.decl;

import local.impl.ObserverCmd;

public interface Observable<T> {
    public void addObserver(Observer observer);
    public void removeObserver(Observer observer);
    public void notifyObservers(T data);
    public void notifyObservers(T data, ObserverCmd cmd);
}
