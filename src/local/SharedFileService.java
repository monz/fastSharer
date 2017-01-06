package local;

import data.FileMetadata;
import data.SharedFile;
import local.decl.AddFileListener;
import local.impl.FileChecksumObserver;
import ui.controller.ChunkProgressController;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        if (listener == null) {
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
        sharedFiles.put(metadata.getFileId(), sharedFile);

        // register observers for shared file
        sharedFile.addObserver(new FileChecksumObserver());
        sharedFile.addObserver(new ChunkProgressController(metadata.getFileId()));

        // notify listeners
        fileListeners.forEach(l -> l.addedLocalFile(sharedFile));
    }

    public void addRemoteFile(SharedFile sharedFile) {
        // todo: implement
        if (sharedFile == null || sharedFile.getMetadata() == null) {
            return;
        }

        log.info(String.format("Added remote file: '%s' to shared files", sharedFile.getFilename()));

        // set file path
        sharedFile.setFilePath(Paths.get(downloadDirectory, sharedFile.getFilename()).toString());

        // todo: register download chunks observer, in Sharer.class?

        // todo: add to shared files map, if exists update shared file

        // todo: register file download finisher?

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

    boolean isFileShared(String fileId) {
        return sharedFiles.get(fileId) != null;
    }

    boolean isFileShared(File file) {
        // check whether a file with given path exists already
        return sharedFiles.values().stream().anyMatch(sf -> sf.getFilePath().equals(file.getAbsolutePath()));
    }
}
