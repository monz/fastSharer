package ui.controller;

import persistence.ConfigFileHandler;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SharerController {
    private static final Logger log = Logger.getLogger(SharerController.class.getName());
    private static final DefaultListModel<String> FILE_LIST_MODEL = new DefaultListModel<>();
    private static final DefaultListModel<String> NODE_LIST_MODEL = new DefaultListModel<>();
    private static final Document SHARER_ID_MODEL = new JTextField().getDocument();

    private static SharerController instance;

    private SharerController() {
    }

    public static SharerController getInstance() {
        if ( instance == null ) {
            instance = new SharerController();
        }
        return instance;
    }

    public static SharerController TEST_newShareControllerInstance() {
        instance = new SharerController();
        return instance;
    }

    public DefaultListModel<String> getFileListModel() {
        return FILE_LIST_MODEL;
    }

    public DefaultListModel<String> getNodeListModel() {
        return NODE_LIST_MODEL;
    }

    synchronized public void addNode(String ip) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                if (NODE_LIST_MODEL.contains(ip)) {
                    return;
                }
                NODE_LIST_MODEL.addElement(ip);
            }
        });
    }

    synchronized public void addFile(String filepath) {
        // due to swing weirdness (not thread safe) have to run on EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (FILE_LIST_MODEL.contains(filepath)) {
                    return;
                }
                FILE_LIST_MODEL.addElement(filepath);
            }
        });
    }

    synchronized public void removeNode(String ip) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                NODE_LIST_MODEL.removeElement(ip);
            }
        });
    }

    synchronized public void updateSharerId(String id) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    SHARER_ID_MODEL.insertString(0, id, null);
                } catch (BadLocationException e) {
                    log.log(Level.WARNING, "", e);
                }
            }
        });
    }

    synchronized public Document getIdModel() {
        return SHARER_ID_MODEL;
    }

    public void openSharerFile(Path filepath) {
//        Net net = Net.getInstance();
//        ShareFile sharedFile = null;
//        try {
//            sharedFile = ShareFileHandler.loadSharedFile(net.getNodeId(), filepath);
//        } catch (IOException e) {
//            log.log(Level.WARNING, "Could not load sharer file: " + sharedFile.getFilename(), e);
//        }
//
//        net.addFile(sharedFile);
//        addFile(sharedFile.getFilename());
    }

    public void loadSharerFiles() {
//        Net net = Net.getInstance();
//        List<ShareFile> sharedFiles = ShareFileHandler.loadSharedFiles(net.getNodeId(), Sharer.configDir);
//        sharedFiles.forEach(sf -> {
//            net.addFile(sf);
//            addFile(sf.getFilename());
//        });
    }

    public void saveSharerFiles() {
//        Net net = Net.getInstance();
//        ShareFileHandler.saveShareFiles(net.getLocalFiles().values(), Sharer.configDir);
    }

    public void saveSettings(Properties config) throws IOException {
//        ConfigFileHandler.saveConfigFile(config);
//        // update net settings
//        Net net = Net.getInstance();
//        net.setConfig(config);
    }

    public Properties loadSettings() throws IOException {
        return ConfigFileHandler.loadConfigFile();
    }
}
