package net;

import data.Chunk;
import data.SharedFile;
import local.ServiceLocator;
import local.decl.AddFileListener;
import net.data.DownloadRequest;
import net.data.ShareCommand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DownloadService implements AddFileListener {
    private static final Logger log = Logger.getLogger(DownloadService.class.getName());
    private static final NetworkService NETWORK_SERVICE = (NetworkService) ServiceLocator.getInstance().getService(ServiceLocator.NETWORK_SERVICE);

    private ExecutorService requester;
    private ExecutorService downloader;
    private Semaphore downloadToken;

    public DownloadService(int maxConcurrentDownloads) {
        this.requester = Executors.newSingleThreadExecutor();
        this.downloader = Executors.newFixedThreadPool(maxConcurrentDownloads);
        this.downloadToken = new Semaphore(maxConcurrentDownloads);
    }

    @Override
    public void addedLocalFile(SharedFile sharedFile) {
        // nothing to download
    }

    @Override
    public void addedRemoteFile(SharedFile sharedFile) {
        sharedFile.getMetadata().getChunks().stream()
            .filter(c -> ! c.isLocal())
            .forEach(c -> requester.execute(requestDownloads(sharedFile, c)));
    }

    public void addDownload() {
        downloader.execute(download());
    }

    private Runnable requestDownloads(SharedFile sharedFile, Chunk c) {
        return () -> {
            // take download token (blocking)
            try {
                downloadToken.acquire();
            } catch (InterruptedException e) {
                log.log(Level.WARNING, "Request download was interrupted", e);
                return;
            }
            String nodeId = getNextDownloadNodeId(sharedFile);
            ShareCommand<DownloadRequest> msg = new ShareCommand<>(ShareCommand.ShareCommandType.DOWNLOAD_REQUEST);
            msg.addData(new DownloadRequest(nodeId, c.getFileId(), c.getChecksum()));

            NETWORK_SERVICE.sendCommand(msg, NETWORK_SERVICE.getNode(UUID.fromString(nodeId)));

            log.info(String.format("Requested Chunk '%s', from file '%s'", c.getChecksum(), sharedFile.getFilename()));
        };
    }

    private Runnable download() {
        return () -> {
            // todo: surround all (in entire sharer project) runnable with try/catch and ...oops

            // todo: implement download

            // give back download token
            downloadToken.release();

            log.info(String.format("Download Chunk '%s' from file '%s'")); // todo: fill in parameter
        };
    }

    private String getNextDownloadNodeId(SharedFile sf) {
        // get nodes which share the chunks remaining for download
        return sf.getChunksToDownload().stream()
            .flatMap(c -> sf.getReplicaNodesByChunk(c.getChecksum()).stream())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .findFirst().orElse(null).toString();
    }
}
