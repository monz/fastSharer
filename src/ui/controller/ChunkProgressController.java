package ui.controller;

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
public class ChunkProgressController implements Observer<FileMetadata> {
    private static final Logger log = Logger.getLogger(ChunkProgressController.class.getName());
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);

    private String fileId;
    private ProgressDialog progressDialog;

    public ChunkProgressController(String fileId) {
        this.fileId = fileId;
    }

    @Override
    synchronized public void update(FileMetadata data, ObserverCmd cmd) {
        // open dialog for shared file(one for each fileId), if not exist
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(data.getFileName(), data.getChunks().size());
            progressDialog.setVisible(true);
        }

        // update progress on GUI
        int chunksChecksumCount = (int)data.getChunks().stream().filter(c -> c.hasChecksum()).count();
        progressDialog.update(chunksChecksumCount);

        // if was last chunk, close dialog
        if (data.getChunks().size() == chunksChecksumCount) {
            // close dialog
            progressDialog.setVisible(false);
            progressDialog.dispose();

            log.info(String.format("Finished chunk calculation for file '%s'", data.getFileName()));

            // remove this observer from observer list
            SharedFile sharedFile = SHARED_FILE_SERVICE.getFile(data.getFileId());
            sharedFile.removeObserver(this);
        }
    }
}
