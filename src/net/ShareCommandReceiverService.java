package net;

import net.decl.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShareCommandReceiverService implements Service {
    private static final Logger log = Logger.getLogger(ShareCommandReceiverService.class.getName());
    private ServerSocket s;
    private Thread receiver;

    public ShareCommandReceiverService(int servicePort) throws IOException {
        this.s = new ServerSocket(servicePort);
        this.receiver = new Thread(acceptShareCommands);
    }

    @Override
    public void start() {
        try {
            receiver.start();
        } catch (IllegalThreadStateException e) {
            // gets thrown when trying to start service twice
            log.log(Level.WARNING, "Could not start share command receiver service", e);
        }
    }

    @Override
    public void stop() {
        receiver.interrupt();
        try {
            s.close();
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not stop share command receiver service", e);
        }
    }

    private Runnable acceptShareCommands = () -> {
        for (;;) {
            try {
                Socket connection = s.accept();

                CommandDispatcher dispatcher = new CommandDispatcher(connection);
                new Thread(dispatcher::dispatch).start();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Ooops!", e);
            }
        }
    };
}