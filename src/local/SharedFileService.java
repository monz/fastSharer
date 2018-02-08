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
import data.FileMetadata;
import data.SharedFile;
import local.decl.AddFileListener;
import local.impl.FileChecksumObserver;
import ui.controller.ChunkProgressController;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SharedFileService {
    private static final Logger log = Logger.getLogger(SharedFileService.class.getName());

    private ConcurrentMap<String, SharedFile> sharedFiles = new ConcurrentHashMap<>();
    private List<AddFileListener> fileListeners = new ArrayList<>();
    private String downloadDirectory;
    private String checksumAlgorithm;
    private Executor executorForRemote;

    public SharedFileService(String downloadDirectory, String checksumAlgorithm) {
        this.downloadDirectory = downloadDirectory;
        this.checksumAlgorithm = checksumAlgorithm;
        this.executorForRemote = Executors.newFixedThreadPool(2);
    }

    public void addFileListener(AddFileListener listener) {
        if (listener == null || fileListeners.contains(listener)) {
            return;
        }
        fileListeners.add(listener);
    }

    public void addLocalFile(FileMetadata metadata) {
        // ATTENTION! Multi-threading already after file drop on GUI
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

    public void addRemoteFile(SharedFile remoteSharedFile) {
        executorForRemote.execute(() -> {
            // check whether the object is valid
            if (remoteSharedFile == null || remoteSharedFile.getMetadata() == null) {
                log.info("Received remote file was not valid");
                return;
            }

            // set file path
            remoteSharedFile.setFilePath(Paths.get(downloadDirectory, remoteSharedFile.getMetadata().getRelativePath().split("[/\\\\]")).toString());

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

            // check whether the file is local (already downloaded)
            // must be after the merge of replica nodes; so they can update their "complete" state properly
            SharedFile localSharedFile = sharedFiles.get(remoteSharedFile.getFileId());
            if (localSharedFile != null && localSharedFile.isLocal()) {
                log.info(String.format("Received remote file '%s' was already downloaded", remoteSharedFile.getFilename()));
                return;
            }

            log.info(String.format("Added remote file: '%s' to shared files", remoteSharedFile.getFilename()));

            // add fileId to not downloaded chunks
            // clean up chunks without checksum
            if (updatedSharedFile != null) {
                updatedSharedFile.getMetadata().getChunks().retainAll(updatedSharedFile.getMetadata().getChunks().stream().filter(Chunk::hasChecksum).collect(Collectors.toList()));
                updatedSharedFile.getChunksToDownload().forEach(c -> c.setFileId(remoteSharedFile.getFileId()));
            }

            // notify listeners
            fileListeners.forEach(l -> l.addedRemoteFile(updatedSharedFile));
        });
    }

    public String getDownloadDirectory() {
        return downloadDirectory;
    }

    public String getFilePath(String fileId) {
        return sharedFiles.get(fileId).getFilePath();
    }

    public SharedFile getFile(String fileId) {
        return sharedFiles.get(fileId);
    }

    public Map<String, SharedFile> getAll() {
        return sharedFiles;
    }

    boolean isFileShared(File file) {
        // check whether a file with given path exists already
        return sharedFiles.values().stream().anyMatch(sf -> sf.getFilename().equals(file.getName()));
    }

    public void removeNodeFromReplicaNodes(UUID nodeId) {
        sharedFiles.values().forEach(sf -> {
            sf.removeReplicaNode(nodeId);
        });
    }

    public List<SharedFile> getLocal() {
        return sharedFiles.values().stream().filter(SharedFile::isLocal).collect(Collectors.toList());
    }

    public List<SharedFile> getNotLocal() {
        return sharedFiles.values().stream()
            .filter(sf -> !sf.isLocal())
            .filter(sf -> sf.getChunksToDownload().size() > 0)
            .collect(Collectors.toList());
    }
}
