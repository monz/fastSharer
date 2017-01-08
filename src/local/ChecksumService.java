package local;

import data.Chunk;
import data.SharedFile;
import util.FileHelper;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChecksumService {
    private static final Logger log = Logger.getLogger(ChecksumService.class.getName());
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);

    private ExecutorService executor;
    private String checksumAlgorithm;

    public ChecksumService(String checksumAlgorithm) {
        this.executor = Executors.newSingleThreadExecutor();
        this.checksumAlgorithm = checksumAlgorithm;
    }

    private Runnable handleChunk(Chunk c) {
        return () -> {
            // calculate checksum
            executor.execute(() -> {
                c.setChecksum(calculateChecksum(c));

                // update metadata observers
                SharedFile sharedFile = SHARED_FILE_SERVICE.getFile(c.getFileId());
                sharedFile.notifyObservers(sharedFile.getMetadata());
            });
        };
    }

    public boolean compareChecksum(SharedFile sharedFile, String checksum) {
        String fileChecksum = calculateChecksum(sharedFile);
        boolean isEqualChecksum = checksum.equals(fileChecksum);

        log.info("Check checksum, expected: " + checksum + " and actual: " + fileChecksum + ", success: " + isEqualChecksum);

        return isEqualChecksum;
    }

    private String calculateChecksum(SharedFile sharedFile) {
        List<String> checksums = new ArrayList<>();
        sharedFile.getMetadata().getChunks().forEach(c -> {
                checksums.add(calculateChecksum(c));
        });

        // prepare message digest
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(checksumAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.WARNING, "Hash algorithm not found!", e);
            return null;
        }

        checksums.forEach(s -> md.update(s.getBytes()));

        return FileHelper.digestToString(md.digest());
    }

    /**
     * SetChecksums calculate the checksum of given chunks async
     * Calculated checksum is stored in chunk
     * @param chunks
     */
    void setChecksums(Collection<Chunk> chunks) {
        chunks.forEach(c -> executor.execute(handleChunk(c)));
    }

    private String calculateChecksum(Chunk c) {
        String filePath = SHARED_FILE_SERVICE.getFilePath(c.getFileId());

        // open file
        BufferedInputStream is;
        try {
            is = new BufferedInputStream(new FileInputStream(filePath));
        } catch (FileNotFoundException e) {
            log.log(Level.WARNING, String.format("File '%s' not found!", filePath), e);
            return null;
        }

        // calculate chunk checksum
        String checksum;
        byte[] b = new byte[(int)c.getSize()];
        try {
            is.skip(c.getOffset());
            int len = is.read(b);

            if (len != c.getSize()) {
                // error occurred read insufficient bytes
                log.log(Level.SEVERE, "Could not read enough bytes from file!");
                return null;
            }

            // prepare message digest
            MessageDigest md;
            try {
                md = MessageDigest.getInstance(checksumAlgorithm);
            } catch (NoSuchAlgorithmException e) {
                log.log(Level.WARNING, "Hash algorithm not found!", e);
                return null;
            }

            md.update(b, 0, len);
            checksum = FileHelper.digestToString(md.digest());

        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not read bytes of file.", e);
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not close input Stream", e);
            }
        }

        log.info(String.format("Calculated checksum of file '%s': %s", filePath, checksum));
        return checksum;
    }
}
