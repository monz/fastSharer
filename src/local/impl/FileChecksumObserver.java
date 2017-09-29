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

package local.impl;

import data.FileMetadata;
import data.SharedFile;
import local.ChecksumService;
import local.ServiceLocator;
import local.SharedFileService;
import local.decl.Observer;

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

    private String checksumAlgorithm;

    public FileChecksumObserver(String checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    @Override
    public void update(FileMetadata data, ObserverCmd cmd) {
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
            md = MessageDigest.getInstance(checksumAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.WARNING, "Hash algorithm not found!", e);
            return;
        }

        // calculate final checksum
        data.getChunks().forEach(c -> {
            md.update(c.getChecksum().getBytes());
        });

        String finalChecksum = ChecksumService.digestToString(md.digest());
        data.setChecksum(finalChecksum);

        log.info(String.format("Final checksum of file '%s': %s", data.getFileName(), finalChecksum));

        // remove this observer from observer list
        sharedFile.removeObserver(this);

        // update all remaining observer
        sharedFile.notifyObservers(data);
    }
}
