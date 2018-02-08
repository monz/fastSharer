/*
 * Copyright (c) 2017. Markus Monz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package local;

import data.Chunk;
import data.SharedFile;

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
    private static final int MD5_LENGTH = 32;
    private static final int BUFFER_SIZE = 32*1024; // 32 kBytes

    private ExecutorService executor;
    private String checksumAlgorithm;

    public ChecksumService(String checksumAlgorithm) {
        this.executor = Executors.newSingleThreadExecutor();
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public static String digestToString(byte[] digest) {

        // convert byte[] to string; hex value
        StringBuffer checksum = new StringBuffer(MD5_LENGTH);
        String s;
        for (byte b : digest) {
            s = Integer.toHexString(0xFF & b);
            if (s.length() == 1) {
                checksum.append('0');
            }
            checksum.append(s);
        }

        return checksum.toString();
    }

    private Runnable handleChunk(Chunk c) {
        return () -> {
            // calculate checksum
            String checksum = calculateChecksum(c);
            if (checksum == null || checksum.isEmpty()) {
                log.severe(String.format("Chunk checksum calculation failed for file '%s'.", c.getFileId()));
                return;
            }
            c.setChecksum(checksum);

            // update metadata observers
            SharedFile sharedFile = SHARED_FILE_SERVICE.getFile(c.getFileId());
            sharedFile.notifyObservers(sharedFile.getMetadata());
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
            String checksum = calculateChecksum(c);
            if (checksum != null) {
                checksums.add(checksum);
                c.setLocal(true);
            }
        });

        // prepare message digest
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(checksumAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.WARNING, "Hash algorithm not found!", e);
            return null;
        }

        checksums.forEach(s -> {
            if (s != null && ! s.equals("")) {
                md.update(s.getBytes());
            }
        });

        return digestToString(md.digest());
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

        // prepare message digest
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(checksumAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.WARNING, "Hash algorithm not found!", e);
            return null;
        }

        // calculate chunk checksum
        String checksum;
        byte[] b = new byte[BUFFER_SIZE];
        try {
            // move to beginning of chunk
            long bytesSkipped = is.skip(c.getOffset());
            if (bytesSkipped != c.getOffset()) {
                log.warning("Could not skip enough bytes to comply with chunk offset!");
                return null;
            }

            // read bytes
            long remainingBytes = c.getSize();
            while (remainingBytes > 0) {
                int expectedLen = (int) Math.min(BUFFER_SIZE, remainingBytes);
                int len = is.read(b, 0, expectedLen);
                if (len != expectedLen) {
                    // error occurred read insufficient bytes
                    log.log(Level.SEVERE, "Could not read enough bytes from file!");
                    return null;
                }
                md.update(b, 0, len);

                remainingBytes -= len;
            }

            checksum = digestToString(md.digest());

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
