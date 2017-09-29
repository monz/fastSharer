/*
 * Copyright (c) 2017. Markus Monz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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