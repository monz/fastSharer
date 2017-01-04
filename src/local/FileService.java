package local;

import data.FileMetadata;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileService {
    private static final Logger log = Logger.getLogger(FileService.class.getName());
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);
    private static final ChunkSumService CHUNK_SUM_SERVICE = (ChunkSumService) ServiceLocator.getInstance().getService(ServiceLocator.CHUNK_SUM_SERVICE);

    public void handleDroppedPaths(List<File> fileList) {
        for (File file : fileList) {
            log.info("Dropped file: " + file.getName());

            // check if file/directory exists
            if ( ! file.exists() ) {
                log.warning("File '" + file.getAbsolutePath() + "' does not exist.");
                continue;
            }

            if (isFileInList(file)) {
                log.info("File was not added, maybe already in list.");
                continue;
            }

            // todo: each in new thread
            FileMetadata metadata;
            try {
                 metadata = new FileMetadata(file.getAbsolutePath());
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not extract file metadata", e);
                continue;
            }

            // add to shared file list
            SHARED_FILE_SERVICE.addToSharedFiles(metadata);

            // start chunk checksum calculation
            CHUNK_SUM_SERVICE.addAll(metadata.getChunks());
        }
    }

    private boolean isFileInList(File file) {
        // todo: implement
        return false;
    }

}
