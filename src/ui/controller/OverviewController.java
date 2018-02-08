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

import data.SharedFile;
import local.ServiceLocator;
import local.SharedFileService;
import local.decl.AddFileListener;
import local.decl.NodeStateListener;
import main.Sharer;
import net.data.Node;
import persistence.ConfigFileHandler;
import persistence.SharedFileHandler;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OverviewController implements AddFileListener, NodeStateListener {
    // todo: hold views for overview
    public static final String STAT_ACTIVE_CHUNKS = "STAT_ACTIVE_CHUNKS";
    public static final String STAT_CHUNKS_TO_DOWNLOAD = "STAT_CHUNKS_TO_DOWNLOAD";
    public static final String STAT_FILES_TO_DOWNLOAD = "STAT_FILES_TO_DOWNLOAD";
    public static final String STAT_FILES_DOWNLOADED = "STAT_FILES_DOWNLOADED";
    public static final String STAT_CHUNKS_WITH_CHECKSUM = "STAT_CHUNKS_WITH_CHECKSUM";
    public static final String STAT_SHARED_FILES_WITH_CHECKSUM = "STAT_SHARED_FILES_WITH_CHECKSUM";

    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);

    private static final Logger log = Logger.getLogger(OverviewController.class.getName());
    private static final DefaultListModel<String> FILE_LIST_MODEL = new DefaultListModel<>();
    private static final DefaultListModel<String> NODE_LIST_MODEL = new DefaultListModel<>();
    private static final Document SHARER_ID_MODEL = new JTextField().getDocument();

    private Map<String, Document> sharer_statistics;

    private static OverviewController instance;

    private OverviewController() {
        sharer_statistics = new HashMap<>();
        sharer_statistics.put(STAT_ACTIVE_CHUNKS, new JTextField().getDocument());
        sharer_statistics.put(STAT_CHUNKS_TO_DOWNLOAD, new JTextField().getDocument());
        sharer_statistics.put(STAT_FILES_TO_DOWNLOAD, new JTextField().getDocument());
        sharer_statistics.put(STAT_FILES_DOWNLOADED, new JTextField().getDocument());
        sharer_statistics.put(STAT_CHUNKS_WITH_CHECKSUM, new JTextField().getDocument());
        sharer_statistics.put(STAT_SHARED_FILES_WITH_CHECKSUM, new JTextField().getDocument());
    }

    public static OverviewController getInstance() {
        if ( instance == null ) {
            instance = new OverviewController();
        }
        return instance;
    }

    public DefaultListModel<String> getFileListModel() {
        return FILE_LIST_MODEL;
    }

    public DefaultListModel<String> getNodeListModel() {
        return NODE_LIST_MODEL;
    }

    public void updateSharerId(String id) {
        SwingUtilities.invokeLater(() -> {
            try {
                SHARER_ID_MODEL.insertString(0, id, null);
            } catch (BadLocationException e) {
                log.log(Level.WARNING, "", e);
            }
        });
    }

    public Document getIdModel() {
        return SHARER_ID_MODEL;
    }

    public void updateSharerStatistics(Map<String, ?> statistics) {
        SwingUtilities.invokeLater(() -> {
            statistics.forEach((k, v) -> {
                try {
                    Document d = sharer_statistics.get(k);
                    d.remove(0, d.getLength());
                    d.insertString(0, v.toString(), null);
                } catch (BadLocationException e) {
                    log.log(Level.WARNING, "", e);
                }
            });
        });
    }

    public Map<String, Document> getStatisticsModels() {
        return sharer_statistics;
    }

    public void openSharerFile(String filePath) throws IOException {
        SharedFile sharedFile = null;
        try {
            sharedFile = SharedFileHandler.loadSharedFile(filePath);
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not load sharer file: " + sharedFile.getFilename(), e);
        }

        SHARED_FILE_SERVICE.addLocalFile(sharedFile.getMetadata());
    }

    public void loadSharerFiles() {
        List<SharedFile> sharedFiles = SharedFileHandler.loadSharedFiles(Sharer.configDir);
        sharedFiles.forEach(sf -> {
            SHARED_FILE_SERVICE.addLocalFile(sf.getMetadata());
        });
    }

    public void saveSharerFiles() {
        SharedFileHandler.saveShareFiles(SHARED_FILE_SERVICE.getLocal(), Sharer.configDir);
    }

    public void saveSettings(Properties config) throws IOException {
        ConfigFileHandler.saveConfigFile(config);
        // update net settings
        ServiceLocator.updateConfig(config);
    }

    public Properties loadSettings() throws IOException {
        return ConfigFileHandler.loadConfigFile();
    }

    @Override
    public void addedLocalFile(SharedFile sharedFile) {
        // due to swing weirdness (not thread safe) have to run on EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            if (FILE_LIST_MODEL.contains(sharedFile.getFilePath())) {
                return;
            }
            FILE_LIST_MODEL.addElement(sharedFile.getFilePath());
        });
    }

    @Override
    public void addedRemoteFile(SharedFile sharedFile) {
        // due to swing weirdness (not thread safe) have to run on EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            if (FILE_LIST_MODEL.contains(sharedFile.getFilePath())) {
                return;
            }
            FILE_LIST_MODEL.addElement(sharedFile.getFilePath());
        });
    }

    @Override
    public void addNode(Node node) {
        SwingUtilities.invokeLater(() -> {
            for (String ip : node.getIps()) {
                if (NODE_LIST_MODEL.contains(ip)) {
                    return;
                }
                NODE_LIST_MODEL.addElement(ip);
            }
        });
    }

    @Override
    public void removeNode(Node node) {
        SwingUtilities.invokeLater(() -> {
            for (String ip : node.getIps()) {
                NODE_LIST_MODEL.removeElement(ip);
            }
        });
    }
}
