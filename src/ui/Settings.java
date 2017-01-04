package ui;

import ui.controller.SharerController;

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
    private static final SharerController SHARE_CONTROLLER = SharerController.getInstance();
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
            config.setProperty("download_destination", downloadDirectory);
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

        downloadDir.setText(config.getProperty("download_destination"));
    }
}
