package local;

import data.Chunk;
import util.FileHelper;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChunkSumService {
    private static final Logger log = Logger.getLogger(ChunkSumService.class.getName());
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);

    private BlockingQueue<Chunk> queue = new LinkedBlockingQueue<>();
    private ExecutorService threadPool;
    private Thread service;

    public ChunkSumService() {
        threadPool = Executors.newSingleThreadExecutor();

        start();
    }

    public void add(Chunk chunk) {
        queue.offer(chunk);
    }

    public void addAll(Collection<Chunk> chunks) {
        chunks.forEach(queue::offer);
    }

    private void start() {
        log.info("Start ChunkSumService");

        service = new Thread(() -> {
            while(true) {
                Chunk c;
                try {
                    c = queue.take();
                } catch (InterruptedException e) {
                    log.log(Level.INFO, "", e);
                    continue;
                }

                // todo: calculate checksum
                threadPool.execute(() -> c.setChecksum(calculateChecksum(c)));
            }
        });
        service.start();
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

        Chunk chunk = null;

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
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                log.log(Level.WARNING, "Hash algorithm MD5 not found!", e);
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
