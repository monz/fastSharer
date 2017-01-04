package ui.logic;

import local.FileService;
import local.ServiceLocator;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DroppedFileListener implements DropTargetListener {
    private static final Logger log = Logger.getLogger(DroppedFileListener.class.getName());
    private static final FileService FILE_SERVICE = (FileService) ServiceLocator.getInstance().getService(ServiceLocator.FILE_SERVICE);

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        // intentionally empty
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
        // intentionally empty
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // intentionally empty
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        // intentionally empty
    }

    @SuppressWarnings("unchecked")
    @Override
    public void drop(DropTargetDropEvent dtde) {
        Transferable transferable = dtde.getTransferable();

        if ( ! transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ) {
            log.warning("DnD data flavor is not supported.");
            return; // TODO: message, exception?
        }

        dtde.acceptDrop(DnDConstants.ACTION_COPY);

        // extract file path information from file drop
        List<File> fileList;
        try {
            fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not get transferable object.", e);
            return;
        } catch (UnsupportedFlavorException e) {
            log.log(Level.SEVERE, "DnD flavor not supported", e);
            return;
        }

        if ( fileList == null ) {
            log.warning("fileList was null");
            return; // TODO: message, exception?
        }

        // user maybe dropped multiple files, handle each file by its own
        FILE_SERVICE.handleDroppedPaths(fileList);
    }
}