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

import data.FileMetadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileService {
    private static final Logger log = Logger.getLogger(FileService.class.getName());
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);
    private static final ChecksumService CHUNK_SUM_SERVICE = (ChecksumService) ServiceLocator.getInstance().getService(ServiceLocator.CHECKSUM_SERVICE);

    class SharedFileVisitor extends SimpleFileVisitor<Path> {
        private Path base;

        protected SharedFileVisitor(File path) {
            this.base = Paths.get(path.getParent());
        }

        @Override
        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            return super.preVisitDirectory(path, basicFileAttributes);
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            handleFile(path.toFile(), base.relativize(path));
            return super.visitFile(path, basicFileAttributes);
        }

        @Override
        public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
            log.log(Level.SEVERE, "File: '" + path + "' could not be visited!", e);
            return super.visitFileFailed(path, e);
        }

        private void handleFile(File file, Path relativePath) {
            // check if file exists
            if ( ! file.exists() ) {
                log.warning("File '" + file.getAbsolutePath() + "' does not exist.");
                return;
            }

            if (SHARED_FILE_SERVICE.isFileShared(file)) {
                log.info("File was not added, maybe already in list.");
                return;
            }

            FileMetadata metadata;
            try {
                metadata = new FileMetadata(file.getAbsolutePath(), relativePath.toString());
            } catch (IOException e) {
                log.log(Level.WARNING, "Could not extract file metadata", e);
                return;
            }

            // todo: instead of push objects to xService use events and listeners
            // add to shared file list
            SHARED_FILE_SERVICE.addLocalFile(metadata);

            // start chunk checksum calculation
            CHUNK_SUM_SERVICE.setChecksums(metadata.getChunks());
        }
    }

    public void handleDroppedPaths(List<File> pathList) {
        for (File path : pathList) {
            log.info("Dropped path: " + path.getAbsolutePath());

            if (path.isFile() || path.isDirectory()) {
                // walk path
                SharedFileVisitor pathVisitor = new SharedFileVisitor(path);
                try {
                    Files.walkFileTree(Paths.get(path.getAbsolutePath()), pathVisitor);
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Could not walk directory!", e);
                }
            } else {
                log.severe("Dropped path:" + path.getAbsolutePath() + " is neither a file nor a directory, get skipped!");
            }
        }
    }
}
