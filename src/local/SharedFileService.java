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

public class SharedFileService {
    private static final Logger log = Logger.getLogger(SharedFileService.class.getName());

    private Map<String, SharedFile> sharedFiles = new HashMap<>();
    private List<AddFileListener> fileListeners = new ArrayList<>();
    private String downloadDirectory;

    public SharedFileService(String downloadDirectory) {
        this.downloadDirectory = downloadDirectory;
    }

    public void addFileListener(AddFileListener listener) {
        if (listener == null || fileListeners.contains(listener)) {
            return;
        }
        fileListeners.add(listener);
    }

    public void addLocalFile(FileMetadata metadata) {
        if (metadata == null) {
            return;
        }
        log.info(String.format("Added local file: '%s' to shared files", metadata.getFileName()));

        SharedFile sharedFile = new SharedFile(metadata);

        // add to shared files
        sharedFiles.put(metadata.getFileId(), sharedFile);

        // register observers for shared file
        sharedFile.addObserver(new FileChecksumObserver());
        sharedFile.addObserver(new ChunkProgressController(metadata.getFileId()));

        // notify listeners
        fileListeners.forEach(l -> l.addedLocalFile(sharedFile));
    }

    public void addRemoteFile(SharedFile sharedFile) {
        if (sharedFile == null || sharedFile.getMetadata() == null || sharedFiles.get(sharedFile.getFileId()) != null) {
            return;
        }

        log.info(String.format("Added remote file: '%s' to shared files", sharedFile.getFilename()));

        // set file path
        sharedFile.setFilePath(Paths.get(downloadDirectory, sharedFile.getFilename()).toString());

        // add to shared files map, if exists update shared file
        sharedFiles.merge(sharedFile.getFileId(), sharedFile, (sf1, sf2) -> {
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
                sf1.getMetadata().getChunks().removeAll(newChunks); // remove duplicates
            }
            sf1.getMetadata().getChunks().addAll(sf2.getMetadata().getChunks());
            return sf1;
        });

        // add fileId to not downloaded chunks
        sharedFile.getChunksToDownload().forEach(c -> c.setFileId(sharedFile.getFileId()));

        // notify listeners
        fileListeners.forEach(l -> l.addedRemoteFile(sharedFile));
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

    public boolean isFileShared(String fileId) {
        return sharedFiles.get(fileId) != null;
    }

    boolean isFileShared(File file) {
        // check whether a file with given path exists already
        return sharedFiles.values().stream().anyMatch(sf -> sf.getFilePath().equals(file.getAbsolutePath()));
    }

    public void removeNodeFromReplicaNodes(UUID nodeId) {
        sharedFiles.values().forEach(sf -> {
            sf.getReplicaNodes().remove(nodeId);
        });
    }
}
