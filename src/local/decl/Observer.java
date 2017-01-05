package local.decl;

import local.impl.ObserverCmd;

public interface Observer<T> {
    public void update(T data, ObserverCmd cmd);
}
