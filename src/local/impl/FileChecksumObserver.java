package local.impl;

import data.FileMetadata;
import data.SharedFile;
import local.ServiceLocator;
import local.SharedFileService;
import local.decl.Observer;
import util.FileHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Calculates the final checksum of a shared file after all chunks' checksums have been calculated
 * therefore observes the state of the shared file's chunks.
 */
public class FileChecksumObserver implements Observer<FileMetadata> {
    private static final Logger log = Logger.getLogger(FileChecksumObserver.class.getName());
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);

    @Override
    public void update(FileMetadata data) {
        SharedFile sharedFile = SHARED_FILE_SERVICE.getFile(data.getFileId());

        // check whether checksum is already calculated
        if (data.hasChecksum()) {
            sharedFile.removeObserver(this);
            return;
        }

        // check if all chunks checksums are calculated
        boolean allChecksums = data.getChunks().stream().allMatch(c -> c.hasChecksum());
        if (!allChecksums) {
            return;
        }

        // prepare message digest
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.WARNING, "Hash algorithm MD5 not found!", e);
            return;
        }

        // calculate final checksum
        data.getChunks().forEach(c -> {
            md.update(c.getChecksum().getBytes());
        });

        String finalChecksum = FileHelper.digestToString(md.digest());
        data.setChecksum(finalChecksum);

        log.info(String.format("Final checksum of file '%s': %s", data.getFileName(), finalChecksum));

        // remove this observer from observer list
        sharedFile.removeObserver(this);

        // update all remaining observer
        sharedFile.notifyObservers(data);
    }
}
