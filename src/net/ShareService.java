package net;

import data.Chunk;
import data.SharedFile;
import local.ChecksumService;
import local.ServiceLocator;
import local.SharedFileService;
import local.decl.AddFileListener;
import net.data.DownloadRequest;
import net.data.DownloadRequestResult;
import net.data.Node;
import net.data.ShareCommand;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ShareService implements AddFileListener {
    private static final Logger log = Logger.getLogger(ShareService.class.getName());
    private static final NetworkService NETWORK_SERVICE = (NetworkService) ServiceLocator.getInstance().getService(ServiceLocator.NETWORK_SERVICE);
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);
    private static final ChecksumService CHECKSUM_SERVICE = (ChecksumService) ServiceLocator.getInstance().getService(ServiceLocator.CHECKSUM_SERVICE);

    private static final int SOCKET_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS); // todo: load from config
    private static final int BUFFER_SIZE = 4096;
    private static final int DENY_DOWNLOAD = -1;

    private ExecutorService requester;
    private ExecutorService downloader;
    private ExecutorService uploader;
    private Semaphore downloadToken;
    private Semaphore uploadToken;

    private int maxConcurrentDownloads;
    private int maxConcurrentUploads;
    private String checksumAlgorithm;

    public ShareService(int maxConcurrentDownloads, int maxConcurrentUploads, String checksumAlgorithm) {
        this.requester = Executors.newSingleThreadExecutor();
        this.downloader = Executors.newFixedThreadPool(maxConcurrentDownloads);
        this.uploader = Executors.newFixedThreadPool(maxConcurrentUploads);
        this.downloadToken = new Semaphore(maxConcurrentDownloads);
        this.uploadToken = new Semaphore(maxConcurrentUploads);

        this.maxConcurrentDownloads = maxConcurrentDownloads;
        this.maxConcurrentUploads = maxConcurrentUploads;
        this.checksumAlgorithm = checksumAlgorithm;
    }

    @Override
    synchronized public void addedLocalFile(SharedFile sharedFile) {
        // nothing to download
    }

    @Override
    synchronized public void addedRemoteFile(SharedFile sharedFile) {
        // check if file already downloaded
        if (Files.exists(Paths.get(sharedFile.getFilePath()))) {
            if (sharedFile.getChecksum() != null) {
                if (CHECKSUM_SERVICE.compareChecksum(sharedFile, sharedFile.getChecksum())) {
                    // file is already downloaded
                    // cancel file download requests
                    sharedFile.setLocal(true);
                    return;
                } else {
                    // delete corrupt file
                    try {
                        Files.delete(Paths.get(sharedFile.getFilePath()));
                    } catch (IOException e) {
                        log.log(Level.WARNING, String.format("Could not delete corrupt file '%s'", sharedFile.getFilePath()), e);
                        return;
                    }
                }
            } else {
                // wait for checksum
                log.info(String.format("Could not check checksum of file '%s', waiting for checksum", sharedFile.getFilename()));
                return;
            }
        }

        // check if enough disk space left
        boolean enoughSpaceLeft;
        try {
            long availableDiskSpace = Files.getFileStore(Paths.get(SHARED_FILE_SERVICE.getDownloadDirectory())).getUsableSpace();
            enoughSpaceLeft = (availableDiskSpace - sharedFile.getFileSize()) > 0;
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not determine remaining disk space", e);
            return;
        }

        if (enoughSpaceLeft) {
            // add to download queue
            sharedFile.getMetadata().getChunks().stream()
                .filter(c -> !c.isLocal())
                .forEach(c -> requester.execute(requestDownloads(sharedFile, c)));
        } else {
            // do not add to download queue
            log.warning("Not enough disk space left to download file");
        }
    }

    synchronized public void addDownload(DownloadRequestResult downloadRequestResult) {
        downloader.execute(download(downloadRequestResult));
    }

    synchronized public void addUpload(DownloadRequest downloadRequest) {
        uploader.execute(upload(downloadRequest));
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
            msg.addData(new DownloadRequest(c.getFileId(), NETWORK_SERVICE.getLocalNodeId().toString(), c.getChecksum()));

            NETWORK_SERVICE.sendCommand(msg, NETWORK_SERVICE.getNode(UUID.fromString(nodeId)));

            log.info(String.format("Requested Chunk '%s', from file '%s'", c.getChecksum(), sharedFile.getFilename()));
        };
    }

    private Runnable download(DownloadRequestResult rr) {
        return () -> {
            // todo: surround all (in entire sharer project) runnable with try/catch and ...oops
            // check if download request was accepted
            if (rr.getDownloadPort() < 0) {
                log.warning(String.format("Download request of chunk %s was not accepted", rr.getChunkChecksum()));
                downloadToken.release();
                return;
            }

            log.info(String.format("Active downloads: %d", maxConcurrentDownloads - downloadToken.availablePermits()));
            log.info(String.format("Currently queued downloads: %d", ((ThreadPoolExecutor)downloader).getQueue().size()));

            Node node = NETWORK_SERVICE.getNode(UUID.fromString(rr.getNodeId()));
            // check connection on all ips
            Socket server = null;
            for (String ip : node.getIps()) {
                try {
                    server = new Socket(InetAddress.getByName(ip), rr.getDownloadPort());
                    server.setSoTimeout(SOCKET_TIMEOUT);
                    break;
                } catch (IOException e) {
                    log.log(Level.WARNING, "Could not establish connection to server", e);
                }
            }
            if (server == null) {
                downloadToken.release();
                return;
            }
            try {
                String checksum = receiveData(server, rr.getFileId(), rr.getChunkChecksum());

                SharedFile sharedFile = SHARED_FILE_SERVICE.getFile(rr.getFileId());
                Chunk chunk = sharedFile.getChunk(rr.getChunkChecksum());
                if (checksum.equals(rr.getChunkChecksum())) {
                    // finish download success
                    log.info(String.format("Download of chunk %s from file %s was successful", rr.getChunkChecksum(), rr.getFileId()));
                    chunk.setLocal(true);
                    // todo: notify listener, to show download progress
                } else {
                    // finish download failure
                    // checksum does not match
                    log.warning(String.format("Download of chunk %s from file %s was corrupt", rr.getChunkChecksum(), rr.getFileId()));
                    // reschedule download of chunk
                    requester.execute(requestDownloads(sharedFile, chunk));
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not receive data", e);
            }

            // give back download token
            downloadToken.release();

            log.info(String.format("Download Chunk '%s' from file '%s'", rr.getChunkChecksum(), rr.getFileId())); // todo: fill in parameter
        };
    }

    private String receiveData(Socket server, String fileId, String chunkChecksum) throws IOException {
        SharedFile sharedFile = SHARED_FILE_SERVICE.getFile(fileId);
        Chunk chunk = sharedFile.getChunk(chunkChecksum);

        RandomAccessFile outputFile = new RandomAccessFile(sharedFile.getFilePath(), "rw");

        if (chunk.getOffset() > 0) {
            outputFile.seek(chunk.getOffset());
        }

        // prepare message digest
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(checksumAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.WARNING, "Hash algorithm not found!", e);
            return null;
        }

        BufferedInputStream is = new BufferedInputStream(server.getInputStream());
        long remainingBytes = chunk.getSize();
        byte[] buf = new byte[BUFFER_SIZE];
        int len = is.read(buf);
        while (len != -1) {
            if ((remainingBytes - len) > 0) {
                md.update(buf, 0, len);
                outputFile.write(buf, 0, len);
                remainingBytes -= len;
                len = is.read(buf);
            } else {
                md.update(buf, 0, (int)remainingBytes);
                outputFile.write(buf, 0, (int)remainingBytes);
                break;
            }
        }
        outputFile.close();
        is.close();

        return ChecksumService.digestToString(md.digest());
    }

    private Runnable upload(DownloadRequest r) {
        return () -> {
            log.info(String.format("Active uploads: %d", maxConcurrentUploads - uploadToken.availablePermits()));
            log.info(String.format("Currently queued upload requests: %d", ((ThreadPoolExecutor)uploader).getQueue().size()));

            // take upload token, if not available deny request
            boolean acceptUpload = uploadToken.tryAcquire();
            boolean chunkIsLocal = SHARED_FILE_SERVICE.getFile(r.getFileId()).getMetadata().isChunkLocal(r.getChunkChecksum());

            ShareCommand<DownloadRequestResult> msg = new ShareCommand<>(ShareCommand.ShareCommandType.DOWNLOAD_REQUEST_RESULT);
            if (acceptUpload && chunkIsLocal) {
                // accept
                log.info("Accept download request: " + r.getFileId());

                ServerSocket s = openRandomPort();

                // send upload decision
                msg.addData(new DownloadRequestResult(
                    r.getFileId(), NETWORK_SERVICE.getLocalNodeId().toString(), r.getChunkChecksum(), s.getLocalPort()));
                NETWORK_SERVICE.sendCommand(msg, NETWORK_SERVICE.getNode(UUID.fromString(r.getNodeId())));

                // handle upload (blocking)
                Socket client = null;
                try {
                    client = s.accept();
                    try {
                        sendData(client, r.getFileId(), r.getChunkChecksum());
                    } catch (IOException e) {
                        log.log(Level.WARNING, "Could not send data to client", e);
                    }
                } catch (SocketTimeoutException e) {
                    log.log(Level.SEVERE, "Connection timed out for chunk: " + r.getChunkChecksum(), e);
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Could not accept new client.", e);
                } finally {
                    // close connection
                    if (client != null) {
                        try { client.close(); } catch (IOException e) { e.printStackTrace(); }
                    }
                    // close network connection
                    try { s.close(); } catch (IOException e) { e.printStackTrace(); }
                }

                // release upload token
                uploadToken.release();
            } else {
                // deny
                log.info("Deny scheduleDownloadRequest request: " + r.getFileId());

                // send upload decision
                msg.addData(new DownloadRequestResult(
                    r.getFileId(), NETWORK_SERVICE.getLocalNodeId().toString(), r.getChunkChecksum(), DENY_DOWNLOAD));
                NETWORK_SERVICE.sendCommand(msg, NETWORK_SERVICE.getNode(UUID.fromString(r.getNodeId())));
            }
        };
    }

    private void sendData(Socket client, String fileId, String chunkChecksum) throws IOException {
        SharedFile sharedFile = SHARED_FILE_SERVICE.getFile(fileId);
        Chunk chunk = sharedFile.getChunk(chunkChecksum);

        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sharedFile.getFilePath()));
        BufferedOutputStream bos = new BufferedOutputStream(client.getOutputStream());

        if (chunk.getOffset() > 0) {
            bis.skip(chunk.getOffset());
        }

        long count = 0;
        long remainingBytes = chunk.getSize();
        byte[] buf = new byte[BUFFER_SIZE];
        int len = bis.read(buf);
        while (len != -1) {
            count += len;
            if ((remainingBytes - len) > 0) {
                bos.write(buf, 0, len);
                remainingBytes -= len;
                len = bis.read(buf);
            } else {
                bos.write(buf, 0, (int)remainingBytes);
                break;
            }
        }
        bos.flush();
        log.info("Totally written bytes: " + count);

        bos.close();
        bis.close();
    }

    private ServerSocket openRandomPort() {
        try {
            ServerSocket s = new ServerSocket(0); // choose random port
            s.setSoTimeout(SOCKET_TIMEOUT);

            return s;
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not open socket for receiving data.", e);
            return null;
        }
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
