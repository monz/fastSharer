package net;

import com.google.gson.reflect.TypeToken;
import data.SharedFile;
import net.data.ShareCommand;
import net.decl.Worker;
import net.impl.SharedListWorker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandDispatcher {
    private static final Logger log = Logger.getLogger(CommandDispatcher.class.getName());
    private ExecutorService executor;
    private BlockingDeque<Socket> queue;
    private Thread serve;

    public CommandDispatcher(int workerCount) {
        this.executor = Executors.newFixedThreadPool(workerCount);
        this.queue = new LinkedBlockingDeque<>();
        this.serve = new Thread(dispatcher);
    }

    private Runnable dispatcher = new Runnable() {
        @Override
        public void run() {
            for (;;) {
                try {
                    Socket s = queue.take();
                    ShareCommand cmd = readCommand(s);
                    if (cmd == null) {
                        log.warning("Could not read command type from message");
                        continue;
                    }
                    Worker w;
                    switch (cmd.getCmd()) {
//                        case DOWNLOAD_REQUEST:
//                            w = new DownloadRequestWorker(cmd.getData());
//                            break;
//                        case DOWNLOAD_REQUEST_RESULT:
//                            w = new DownloadResultWorker(cmd.getData());
//                            break;
//                        case DOWNLOAD_FILE:
//                            w = new DownloadFileWorker(cmd.getData());
//                            break;
                        case PUSH_SHARE_LIST:
                            //w = new SimplePrintWorker(cmd.serialize());
                            w = new SharedListWorker(cmd.getData());
                            break;
//                        case CHUNK_INFO_REQUEST:
//                            w = new ChunkInfoRequestWorker(cmd.getData());
//                            break;
//                        case CHUNK_INFO_REQUEST_RESULT:
//                            w = new ChunkInfoResultWorker(cmd.getData());
//                            break;
                        default:
                            log.info("Unknown command to dispatch: " + cmd.getCmd());
                            continue;
                    }
                    executor.execute(w);
                } catch (InterruptedException e) {
                    log.log(Level.WARNING, "Taking the next client connection was interrupted.", e);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Ooops!", e);
                }
            }
        }
    };

    public void start() {
        serve.start();
    }

    public void stop() {
        serve.interrupt();
    }

    public void add(Socket connection) {
        queue.offer(connection);
    }

    private ShareCommand readCommand(Socket client) {
        ShareCommand cmd;
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(client.getInputStream(), NetworkService.PROTOCOL_CHARSET));
            String line = r.readLine();

            log.info("CommandRead: " + line);

            Pattern p = Pattern.compile("\"cmd\":\"(\\w*)\"");
            Matcher m = p.matcher(line);

            ShareCommand.ShareCommandType cmdType;
            if (m.find()) {
                cmdType = ShareCommand.ShareCommandType.valueOf(m.group(1));
            } else {
                log.warning("No command in received message.");
                return null;
            }

            Type type;
            switch(cmdType) {
//                case DOWNLOAD_REQUEST:
//                    type = new TypeToken<ShareCommand<DownloadRequest>>() {}.getType();
//                    break;
//                case DOWNLOAD_REQUEST_RESULT:
//                    type = new TypeToken<ShareCommand<DownloadRequestResult>>() {}.getType();
//                    break;
//                case DOWNLOAD_FILE:
//                    type = new TypeToken<ShareCommand<DownloadRequestResult>>() {}.getType();
//                    break;
                case PUSH_SHARE_LIST:
                    type = new TypeToken<ShareCommand<SharedFile>>() {}.getType();
                    break;
//                case CHUNK_INFO_REQUEST:
//                    type = new TypeToken<ShareCommand<ChunkInfoRequest>>() {}.getType();
//                    break;
//                case CHUNK_INFO_REQUEST_RESULT:
//                    type = new TypeToken<ShareCommand<ChunkInfoRequestResult>>() {}.getType();
//                    break;
                default:
                    type = null;
                    break;
            }

            if (type == null) {
                cmd = null;
            } else {
                cmd = ShareCommand.deserialize(line, type);
            }

            client.close();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not received read command", e);
            return null;
        }
        return cmd;
    }
}
