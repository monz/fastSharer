package ui.controller;

import data.SharedFile;
import local.decl.Observer;
import local.impl.ObserverCmd;
import net.data.Node;
import persistence.ConfigFileHandler;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OverviewController implements Observer<Object> {
    // todo: hold views for overview

    private static final Logger log = Logger.getLogger(OverviewController.class.getName());
    private static final DefaultListModel<String> FILE_LIST_MODEL = new DefaultListModel<>();
    private static final DefaultListModel<String> NODE_LIST_MODEL = new DefaultListModel<>();
    private static final Document SHARER_ID_MODEL = new JTextField().getDocument();

    private static OverviewController instance;

    private OverviewController() {

    }

    public static OverviewController getInstance() {
        if ( instance == null ) {
            instance = new OverviewController();
        }
        return instance;
    }

    public static OverviewController TEST_newShareControllerInstance() {
        instance = new OverviewController();
        return instance;
    }

    public DefaultListModel<String> getFileListModel() {
        return FILE_LIST_MODEL;
    }

    public DefaultListModel<String> getNodeListModel() {
        return NODE_LIST_MODEL;
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

    @Override
    public void update(Object data, ObserverCmd cmd) {
        if (data instanceof SharedFile) {
            SharedFile sharedFile = (SharedFile) data;
            // due to swing weirdness (not thread safe) have to run on EDT (Event Dispatch Thread)
            SwingUtilities.invokeLater(() -> {
                if (FILE_LIST_MODEL.contains(sharedFile.getFilePath())) {
                    return;
                }
                FILE_LIST_MODEL.addElement(sharedFile.getFilePath());
            });
        } else if (data instanceof Node) {
            Node node = (Node) data;

            switch (cmd) {
                case ADD:
                    addNode(node);
                    break;
                case DELETE:
                    removeNode(node);
                    break;
                default:
                    log.warning(String.format("Unknown ObserverCmd '%s'", cmd));
                    break;
            }
        }
    }

    private void addNode(Node node) {
        SwingUtilities.invokeLater(() -> {
            for (String ip : node.getIps()) {
                if (NODE_LIST_MODEL.contains(ip)) {
                    return;
                }
                NODE_LIST_MODEL.addElement(ip);
            }
        });
    }

    private void removeNode(Node node) {
        SwingUtilities.invokeLater(() -> {
            for (String ip : node.getIps()) {
                NODE_LIST_MODEL.removeElement(ip);
            }
        });
    }
}