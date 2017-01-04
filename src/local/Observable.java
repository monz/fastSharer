package local;

public interface Observable<T> {
    public void addObserver(Observer observer);
    public void removeObserver(Observer observer);
    public void notifyObservers(T data);
}
