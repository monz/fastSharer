package local;

import data.Chunk;
import data.FileMetadata;
import data.SharedFile;
import local.decl.AddFileListener;
import local.impl.FileChecksumObserver;
import ui.controller.ChunkProgressController;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SharedFileService {
    private static final Logger log = Logger.getLogger(SharedFileService.class.getName());

    private Map<String, SharedFile> sharedFiles = new HashMap<>();
    private List<AddFileListener> fileListeners = new ArrayList<>();
    private String downloadDirectory;
    private String checksumAlgorithm;

    public SharedFileService(String downloadDirectory, String checksumAlgorithm) {
        this.downloadDirectory = downloadDirectory;
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public void addFileListener(AddFileListener listener) {
        if (listener == null || fileListeners.contains(listener)) {
            return;
        }
        fileListeners.add(listener);
    }

    synchronized public void addLocalFile(FileMetadata metadata) {
        if (metadata == null) {
            return;
        }
        log.info(String.format("Added local file: '%s' to shared files", metadata.getFileName()));

        SharedFile sharedFile = new SharedFile(metadata);

        // add to shared files
        sharedFiles.put(metadata.getFileId(), sharedFile);

        // register observers for shared file
        sharedFile.addObserver(new FileChecksumObserver(checksumAlgorithm));
        sharedFile.addObserver(new ChunkProgressController());

        // notify listeners
        fileListeners.forEach(l -> l.addedLocalFile(sharedFile));
    }

    synchronized public void addRemoteFile(SharedFile remoteSharedFile) {
        // check whether the object is valid
        if (remoteSharedFile == null || remoteSharedFile.getMetadata() == null) {
            log.info(String.format("Received remote file '%s' was not valid", remoteSharedFile.getFilename()));
            return;
        }

        // check whether the file is local (already downloaded)
        SharedFile localSharedFile = sharedFiles.get(remoteSharedFile.getFileId());
        if (localSharedFile != null && localSharedFile.isLocal()) {
            log.info(String.format("Received remote file '%s' was already downloaded", remoteSharedFile.getFilename()));
            return;
        }

        log.info(String.format("Added remote file: '%s' to shared files", remoteSharedFile.getFilename()));

        // set file path
        remoteSharedFile.setFilePath(Paths.get(downloadDirectory, remoteSharedFile.getFilename()).toString());

        // add to shared files map, if exists update shared file
        SharedFile updatedSharedFile = sharedFiles.merge(remoteSharedFile.getFileId(), remoteSharedFile, (sf1, sf2) -> {
            if (sf2 == null) {
                return sf1;
            }
            // merge replica nodes
            sf2.getReplicaNodes().forEach(sf1::addReplicaNode);

            // update shared file checksum in sf1
            sf1.getMetadata().setChecksum(sf2.getMetadata().getChecksum());

            // merge chunks
            List<Chunk> newChunks = sf2.getMetadata().getChunks();
            if (newChunks != null) {
                newChunks.removeAll(sf1.getMetadata().getChunks()); // remove duplicates
            }
            sf1.getMetadata().getChunks().addAll(newChunks);
            return sf1;
        });

        // add fileId to not downloaded chunks
        // clean up chunks without checksum
        if (updatedSharedFile != null) {
            updatedSharedFile.getMetadata().getChunks().retainAll(updatedSharedFile.getMetadata().getChunks().stream().filter(Chunk::hasChecksum).collect(Collectors.toList()));
            updatedSharedFile.getChunksToDownload().forEach(c -> c.setFileId(remoteSharedFile.getFileId()));
        }

        // notify listeners
        fileListeners.forEach(l -> l.addedRemoteFile(updatedSharedFile));
    }

    public String getDownloadDirectory() {
        return downloadDirectory;
    }

    public String getFilePath(String fileId) {
        return sharedFiles.get(fileId).getFilePath();
    }

    synchronized public SharedFile getFile(String fileId) {
        return sharedFiles.get(fileId);
    }

    synchronized public Map<String, SharedFile> getAll() {
        return sharedFiles;
    }

    synchronized boolean isFileShared(File file) {
        // check whether a file with given path exists already
        return sharedFiles.values().stream().anyMatch(sf -> sf.getFilename().equals(file.getName()));
    }

    synchronized public void removeNodeFromReplicaNodes(UUID nodeId) {
        sharedFiles.values().forEach(sf -> {
            sf.removeReplicaNode(nodeId);
        });
    }
}
