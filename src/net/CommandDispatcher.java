package net;

import com.google.gson.reflect.TypeToken;
import data.SharedFile;
import local.ServiceLocator;
import local.SharedFileService;
import net.data.DownloadRequest;
import net.data.DownloadRequestResult;
import net.data.ShareCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandDispatcher {
    private static final Logger log = Logger.getLogger(CommandDispatcher.class.getName());
    private static final ShareService SHARE_SERVICE = (ShareService) ServiceLocator.getInstance().getService(ServiceLocator.SHARE_SERVICE);
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);

    private Socket s;

    public CommandDispatcher(Socket s) {
        this.s = s;
    }

    public void dispatch() {
        for (;;) {
            try {
                if (s.isClosed()) {
                    return;
                }
                ShareCommand cmd = readCommand(s);
                if (cmd == null) {
                    log.warning("Could not read command type from message");
                    continue;
                }
                switch (cmd.getCmd()) {
                    case DOWNLOAD_REQUEST:
                        //log.info(String.format("Received download request for file '%s'", data.get(0).getFileId()));
                        cmd.getData().forEach(o -> SHARE_SERVICE.addUpload((DownloadRequest)o));
                        break;
                    case DOWNLOAD_REQUEST_RESULT:
                        //log.info(String.format("Received download request result for file '%s'", data.get(0).getFileId()));
                        cmd.getData().forEach(o -> SHARE_SERVICE.addDownload((DownloadRequestResult)o));
                        break;
                    case PUSH_SHARE_LIST:
                        //log.info(String.format("Received remote file: %s", data.get(0).getFilename()));
                        cmd.getData().stream().forEach(o -> SHARED_FILE_SERVICE.addRemoteFile((SharedFile)o));
                        break;
                    default:
                        log.info("Unknown command to dispatch: " + cmd.getCmd());
                        break;
                }
            } catch (Exception e) {
                log.log(Level.SEVERE, "Ooops!", e);
                try {
                    s.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return;
            }
        }
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
                case DOWNLOAD_REQUEST:
                    type = new TypeToken<ShareCommand<DownloadRequest>>() {}.getType();
                    break;
                case DOWNLOAD_REQUEST_RESULT:
                    type = new TypeToken<ShareCommand<DownloadRequestResult>>() {}.getType();
                    break;
                case PUSH_SHARE_LIST:
                    type = new TypeToken<ShareCommand<SharedFile>>() {}.getType();
                    break;
                default:
                    type = null;
                    break;
            }

            if (type == null) {
                cmd = null;
            } else {
                cmd = ShareCommand.deserialize(line, type);
            }

        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not received read command", e);
            try { client.close(); } catch (IOException e1) {}
            return null;
        }
        return cmd;
    }
}
