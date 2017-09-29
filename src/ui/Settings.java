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

package ui;

import main.Sharer;
import ui.controller.OverviewController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Settings extends JPanel {
    private static final Logger log = Logger.getLogger(Settings.class.getName());
    private static final OverviewController SHARE_CONTROLLER = OverviewController.getInstance();
    private JDialog dialog;
    private JTextField downloadDir;

    public Settings(JDialog dialog) {
        this.dialog = dialog;
        init();
        setCurrentSettings();
    }

    private void init() {
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        this.add(defineGeneral(), BorderLayout.CENTER);
        this.add(defineButtons(), BorderLayout.SOUTH);
    }

    private JPanel defineGeneral() {
        SpringLayout layout = new SpringLayout();

        JPanel p = new JPanel(layout);

        JLabel downloadDirLabel = new JLabel("Download directory:");
        layout.putConstraint(SpringLayout.WEST, downloadDirLabel, 5, SpringLayout.WEST, p);
        layout.putConstraint(SpringLayout.NORTH, downloadDirLabel, 5, SpringLayout.NORTH, p);

        downloadDir = new JTextField(30);
        layout.putConstraint(SpringLayout.WEST, downloadDir, 5, SpringLayout.EAST, downloadDirLabel);
        layout.putConstraint(SpringLayout.NORTH, downloadDir, 5, SpringLayout.NORTH, p);

        p.add(downloadDirLabel);
        p.add(downloadDir);


        return p;
    }

    private JPanel defineButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    SHARE_CONTROLLER.saveSettings(getCurrentSettings());

                    // show restart hint // todo: implement settings reload mechanism
                    JOptionPane.showMessageDialog(
                        null,
                        "New settings will be active after Sharer restart."
                    );

                } catch (IOException err) {
                    log.log(Level.SEVERE, "Could not store config file to disk", err);
                }
                Settings.this.setVisible(false);
                dialog.dispose();
            }
        });
        p.add(okButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Settings.this.setVisible(false);
                dialog.dispose();
            }
        });
        p.add(cancelButton);

        return p;
    }

    private Properties getCurrentSettings() {
        // load current config file
        Properties config;
        try {
            config = SHARE_CONTROLLER.loadSettings();
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not load current config file", e);
            config = new Properties();
        }

        // update config file with settings in gui
        String downloadDirectory = downloadDir.getText();
        if(! downloadDirectory.isEmpty()) {
            config.setProperty(Sharer.DOWNLOAD_DIRECTORY, downloadDirectory);
        } else {
            log.warning("Could not save scheduleDownloadRequest directory settings: '" + downloadDirectory + "'");
        }

        return config;
    }

    private void setCurrentSettings() {
        Properties config;
        try {
            config = SHARE_CONTROLLER.loadSettings();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not load Sharer settings file", e);
            return;
        }

        downloadDir.setText(config.getProperty(Sharer.DOWNLOAD_DIRECTORY));
    }
}
