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

package ui.controller;

import data.Chunk;
import data.FileMetadata;
import data.SharedFile;
import local.ServiceLocator;
import local.SharedFileService;
import local.decl.Observer;
import local.impl.ObserverCmd;
import ui.ProgressDialog;

import java.util.logging.Logger;

/**
 * Shows the current progress on a shared file's chunk calculation
 * therefore observes the state of the shared file's chunks.
 */
public class ChunkDownloadProgressController implements Observer<FileMetadata> {
    private static final Logger log = Logger.getLogger(ChunkDownloadProgressController.class.getName());
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);
    private static final String TITLE = "Download File";

    private ProgressDialog progressDialog;

    @Override
    synchronized public void update(FileMetadata data, ObserverCmd cmd) {
        // do not open dialog for files smaller than 5MB
        if (data.getFileSize() < 5 * 1024 * 1024) {
            return;
        }

        // open dialog for shared file(one for each fileId), if not exist
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(TITLE, data.getFileName(), Chunk.getChunkCount(data.getFileSize()));
            progressDialog.setVisible(true);
        }

        // update progress on GUI
        int chunksDownloadedCount = (int)data.getChunks().stream().filter(c -> c.isLocal()).count();
        progressDialog.update(chunksDownloadedCount);

        // if was last chunk, close dialog
        SharedFile sharedFile = SHARED_FILE_SERVICE.getFile(data.getFileId());
        if (sharedFile.isLocal()) {
            // close dialog
            progressDialog.setVisible(false);
            progressDialog.dispose();

            // remove this listener from listener list
            sharedFile.removeObserver(this);
        }
    }
}
