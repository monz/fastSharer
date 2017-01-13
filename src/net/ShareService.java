package net;

import data.Chunk;
import data.SharedFile;
import local.ChecksumService;
import local.ServiceLocator;
import local.SharedFileService;
import local.decl.AddFileListener;
import local.impl.ObserverCmd;
import net.data.*;
import ui.controller.ChunkDownloadProgressController;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class ShareService implements AddFileListener {
    private static final Logger log = Logger.getLogger(ShareService.class.getName());
    private static final NetworkService NETWORK_SERVICE = (NetworkService) ServiceLocator.getInstance().getService(ServiceLocator.NETWORK_SERVICE);
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);
    private static final ChecksumService CHECKSUM_SERVICE = (ChecksumService) ServiceLocator.getInstance().getService(ServiceLocator.CHECKSUM_SERVICE);

    private static final int SOCKET_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS); // todo: load from config
    private static final int BUFFER_SIZE = 4096;
    private static final int DENY_DOWNLOAD = -1;
    private static final String LOCAL_NODE_ID = NETWORK_SERVICE.getLocalNodeId().toString();
    private static final String DOWNLOAD_EXTENSION = ".part";

    private ExecutorService requester;
    private ExecutorService downloader;
    private ExecutorService uploader;
    private Semaphore downloadToken;
    private Semaphore uploadToken;

    private int maxConcurrentDownloads;
    private int maxConcurrentUploads;
    private String checksumAlgorithm;
    private AtomicInteger downloadNodeRound;

    public ShareService(int maxConcurrentDownloads, int maxConcurrentUploads, String checksumAlgorithm) {
        this.requester = Executors.newSingleThreadExecutor();
        this.downloader = Executors.newFixedThreadPool(maxConcurrentDownloads);
        this.uploader = Executors.newFixedThreadPool(maxConcurrentUploads);
        this.downloadToken = new Semaphore(maxConcurrentDownloads);
        this.uploadToken = new Semaphore(maxConcurrentUploads);

        this.maxConcurrentDownloads = maxConcurrentDownloads;
        this.maxConcurrentUploads = maxConcurrentUploads;
        this.checksumAlgorithm = checksumAlgorithm;
        this.downloadNodeRound = new AtomicInteger();
    }

    @Override
    synchronized public void addedLocalFile(SharedFile sharedFile) {
        // nothing to download
    }

    @Override
    synchronized public void addedRemoteFile(SharedFile sharedFile) {
        // first time this remote file info gets read, activate shared file download
        // and check if file is already downloaded, and whether the file checksum
        // matches the transmitted file checksum
        if (sharedFile.activateDownload() && (! sharedFile.isLocal())) {
            // check if file already downloaded
            if (Files.exists(Paths.get(sharedFile.getFilePath()))) {
                if (sharedFile.getChecksum() != null) {
                    if (CHECKSUM_SERVICE.compareChecksum(sharedFile, sharedFile.getChecksum())) {
                        // file is already downloaded
                        // cancel file download requests
                        return;
                    } else {
                        // delete corrupt file
                        try {
                            log.info(String.format("Delete corrupt file '%s'", sharedFile.getFilePath()));
                            Files.delete(Paths.get(sharedFile.getFilePath()));
                        } catch (IOException e) {
                            log.log(Level.WARNING, String.format("Could not delete corrupt file '%s'", sharedFile.getFilePath()), e);
                            return;
                        } finally {
                            sharedFile.deactivateDownload();
                        }
                    }
                } else {
                    // wait for checksum
                    log.info(String.format("Could not check checksum of file '%s', waiting for checksum", sharedFile.getFilename()));
                    return;
                }
            }
        } else if (sharedFile.isDownloadActive()) {
            // check whether download was already scheduled
            return;
        }

        // check if enough disk space left
        boolean enoughSpaceLeft;
        try {
            long availableDiskSpace = Files.getFileStore(Paths.get(SHARED_FILE_SERVICE.getDownloadDirectory())).getUsableSpace();
            enoughSpaceLeft = (availableDiskSpace - sharedFile.getFileSize()) > 0;

            // add download observer, only once
            sharedFile.addObserver(new ChunkDownloadProgressController());
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not determine remaining disk space", e);
            return;
        }

        if (enoughSpaceLeft) {
            // add download job for each chunk
            IntStream.range(0, Chunk.getChunkCount(sharedFile.getFileSize()))
                .forEach(c -> requester.execute(requestDownloads(sharedFile)));
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

    private Runnable requestDownloads(SharedFile sharedFile) {
        return () -> {
            log.info(String.format("Remaining chunks to download: %d", sharedFile.getChunksToDownload().size()));

            // take download token (blocking)
            try {
                downloadToken.acquire();
            } catch (InterruptedException e) {
                log.log(Level.WARNING, "Request download was interrupted", e);
                return;
            }

            int round = downloadNodeRound.get() < maxConcurrentDownloads ? downloadNodeRound.getAndIncrement() : downloadNodeRound.getAndSet(0);
            Pair<UUID, Chunk> downloadInfo = sharedFile.getNextChunkToDownload(round);

            if (downloadInfo == null) {
                log.info("Choose next chunk to download failed");
                downloadFail(sharedFile, null);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return;
            }

            // mark chunk as currently downloading
            Chunk chunk = downloadInfo.getValue();
            if (!chunk.activateDownload()) {
                log.info(String.format("Download request of chunk %s canceled, already downloading", chunk.getChecksum()));
                downloadFail(sharedFile, chunk);
                return;
            }

            // send download request
            ShareCommand<DownloadRequest> msg = new ShareCommand<>(ShareCommand.ShareCommandType.DOWNLOAD_REQUEST);
            msg.addData(new DownloadRequest(chunk.getFileId(), LOCAL_NODE_ID, chunk.getChecksum()));

            Node node = NETWORK_SERVICE.getNode(downloadInfo.getKey());
            if (node == null) {
                log.warning(String.format("Could not find node for nodeId '%s'", downloadInfo.getKey()));
                // re-schedule download of chunk
                downloadFail(sharedFile, chunk);
                return;
            }
            NETWORK_SERVICE.sendCommand(msg, node);

            log.info(String.format("Requested Chunk '%s', from file '%s'", chunk.getChecksum(), sharedFile.getFilename()));
        };
    }

    private Runnable download(DownloadRequestResult rr) {
        return () -> {
            // todo: surround all (in entire sharer project) runnable with try/catch and ...oops

            SharedFile sharedFile = SHARED_FILE_SERVICE.getFile(rr.getFileId());
            Chunk chunk = sharedFile.getChunk(rr.getChunkChecksum());

            // check if download request was accepted
            if (rr.getDownloadPort() < 0) {
                log.warning(String.format("Download request of chunk %s was not accepted", rr.getChunkChecksum()));
                downloadFail(sharedFile, chunk);
                return;
            }

            log.info(String.format("Active downloads: %d", maxConcurrentDownloads - downloadToken.availablePermits()));

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
                downloadFail(sharedFile, chunk);
                return;
            }
            try {
                String checksum = receiveData(server, rr.getFileId(), rr.getChunkChecksum());

                if (checksum.equals(rr.getChunkChecksum())) {
                    // finish download success
                    downloadSuccess(sharedFile, chunk);
                } else {
                    // finish download failure
                    // checksum does not match
                    downloadFail(sharedFile, chunk);
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not receive data", e);
                downloadFail(sharedFile, chunk);
            }
        };
    }

    // if this method is synchronized, deadlock
    private void downloadSuccess(SharedFile sharedFile, Chunk chunk) {
        log.info(String.format("Download of chunk %s of file %s was successful", chunk.getChecksum(), chunk.getFileId()));
        chunk.setLocal(true);
        chunk.deactivateDownload();
        // check whether file was completely downloaded
        if (sharedFile.isLocal()) {
            // finish file download
            // rename file
            try {
                log.info(String.format("Rename file '%s' to finish download", sharedFile.getFilename()));
                Files.move(Paths.get(sharedFile.getFilePath()+ DOWNLOAD_EXTENSION), Paths.get(sharedFile.getFilePath()), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.log(Level.WARNING, String.format("Could not rename file '%s' to finish download", sharedFile.getFilename()), e);
            }
            sharedFile.deactivateDownload();
        } else {
            log.info(String.format("File '%s' is not finished yet, chunks to download %s", sharedFile.getFilename(), sharedFile.getChunksToDownload().size()));
        }
        downloadToken.release();

        // notify observer, to show download progress
        sharedFile.notifyObservers(sharedFile.getMetadata(), ObserverCmd.UPDATE);
    }

    synchronized private void downloadFail(SharedFile sharedFile, Chunk chunk) {
        log.info("failed download");
        if (chunk != null) {
            log.warning(String.format("Download of chunk %s of file %s failed", chunk.getChecksum(), chunk.getFileId()));
            chunk.deactivateDownload();
        }
        // reschedule download of chunk if file is not complete yet
        if (! sharedFile.isLocal()) {
            log.info("reschedule download");
            requester.execute(requestDownloads(sharedFile));
        } else {
            log.info("Do not reschedule chunk download");
        }
        downloadToken.release();
    }

    private String receiveData(Socket server, String fileId, String chunkChecksum) throws IOException {
        SharedFile sharedFile = SHARED_FILE_SERVICE.getFile(fileId);
        Chunk chunk = sharedFile.getChunk(chunkChecksum);

        RandomAccessFile outputFile = new RandomAccessFile(sharedFile.getFilePath() + DOWNLOAD_EXTENSION, "rw");

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

            // take upload token, if not available, deny request
            boolean acceptUpload = uploadToken.tryAcquire();
            boolean chunkIsLocal = SHARED_FILE_SERVICE.getFile(r.getFileId()).getMetadata().isChunkLocal(r.getChunkChecksum());

            ShareCommand<DownloadRequestResult> msg = new ShareCommand<>(ShareCommand.ShareCommandType.DOWNLOAD_REQUEST_RESULT);
            if (acceptUpload && chunkIsLocal) {
                // accept
                log.info("Accept download request: " + r.getFileId());

                ServerSocket s = openRandomPort();

                // send upload decision
                msg.addData(new DownloadRequestResult(
                    r.getFileId(), LOCAL_NODE_ID, r.getChunkChecksum(), s.getLocalPort()));
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
            } else {
                // deny
                log.info("Deny scheduleDownloadRequest request: " + r.getFileId());

                // send upload decision
                msg.addData(new DownloadRequestResult(
                    r.getFileId(), LOCAL_NODE_ID, r.getChunkChecksum(), DENY_DOWNLOAD));
                NETWORK_SERVICE.sendCommand(msg, NETWORK_SERVICE.getNode(UUID.fromString(r.getNodeId())));
            }

            // release upload token
            uploadToken.release();
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
}
