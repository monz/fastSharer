package net.decl;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Worker<T> implements Runnable {
    private static final Logger log = Logger.getLogger(Worker.class.getName());
    protected T data;

    public Worker(T data) {
        this.data = data;
    }

    abstract public void serve();

    @Override
    public void run() {
        try {
            serve();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Ooops!", e);
        }
    }
}
