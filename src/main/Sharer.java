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

package main;

import local.ServiceLocator;
import local.SharedFileService;
import net.*;
import persistence.ConfigFileHandler;
import ui.Overview;
import ui.controller.OverviewController;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Sharer {
    public static final Path configDir = Paths.get(System.getProperty("user.home"), ".sharer");
    public static final Path configFile = Paths.get(configDir.toString(), "config.properties");

    public static final String DISCOVERY_PORT = "sharer_discovery_port";
    public static final String DISCOVERY_PERIOD = "sharer_discovery_period";
    public static final String SHARE_INFO_PERIOD = "sharer_share_info_period";
    public static final String NODE_CLEANUP_RATE = "sharer_discovery_node_cleanup_period";
    public static final String CMD_PORT = "sharer_cmd_port";

    public static final String MAX_DOWNLOADS = "sharer_max_downloads";
    public static final String MAX_UPLOADS = "sharer_max_uploads";
    public static final String DOWNLOAD_DIRECTORY = "sharer_download_directory";
    public static final String CHECKSUM_ALGORITHM = "sharer_checksum_algorithm";

    private static final Logger log = Logger.getLogger(Sharer.class.getName());

    public static void main(String[] args) {
        // load default settings
        Properties config;
        try {
            config = loadProperties(configFile.toFile()); // TODO: read from application arguments?
            log.info("Start args: " + Arrays.toString(args) + " length: " + args.length);
            if (args.length > 0) {
                config.remove(DOWNLOAD_DIRECTORY);
                config.setProperty(DOWNLOAD_DIRECTORY, args[0]);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // create download directory if not exists
        try {
            Files.createDirectories(Paths.get(config.getProperty(DOWNLOAD_DIRECTORY)));
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not create download directory", e);
            System.exit(1);
        }

        // init service locator - start services
        ServiceLocator.init(config);
        ServiceLocator serviceLocator = ServiceLocator.getInstance();

        // start discovery service
        DiscoveryService discoveryService = ((DiscoveryService) serviceLocator.getService(ServiceLocator.DISCOVERY_SERVICE));
        discoveryService.start();

        // start share command receiver service
        ShareCommandReceiverService shareCommandReceiverService = ((ShareCommandReceiverService) serviceLocator.getService(ServiceLocator.SHARED_CMD_RECEIVER_SERVICE));
        shareCommandReceiverService.start();

        // start share info file service
        SharedFileInfoService sharedFileInfoService = ((SharedFileInfoService) serviceLocator.getService(ServiceLocator.SHARED_FILE_INFO_SERVICE));
        sharedFileInfoService.start();

        // register observer for shared files
        SharedFileService sharedFileService = ((SharedFileService) serviceLocator.getService(ServiceLocator.SHARED_FILE_SERVICE));
        sharedFileService.addFileListener((ShareService)serviceLocator.getService(ServiceLocator.SHARE_SERVICE));

        // set Sharer id on gui
        NetworkService networkService = ((NetworkService)serviceLocator.getService(ServiceLocator.NETWORK_SERVICE));
        OverviewController.getInstance().updateSharerId(networkService.getLocalNodeId().toString());

        // register listener for gui
        sharedFileService.addFileListener(OverviewController.getInstance());
        networkService.addNodeStateListener(OverviewController.getInstance());

        // show gui
        // todo: start GUI via controller
        Overview o = new Overview("Sharer");
        o.setVisible(true);
    }

    private static Properties loadProperties(File configuration) throws IOException {
        boolean loadedDefault = false;

        BufferedInputStream is;
        if (configuration.exists()) {
            try {
                is = new BufferedInputStream(new FileInputStream(configuration));
            } catch (FileNotFoundException e) {
                log.info("Could not open Sharer configuration file.");
                return null;
            }
        } else {
            // load default values
            // use default settings delivered by Sharer
            is = new BufferedInputStream(Sharer.class.getClassLoader().getResourceAsStream("config.properties"));
            loadedDefault = true;
        }

        Properties config = new Properties();
        config.load(is);
        is.close();

        if (loadedDefault) {
            String downloadDestination = String.format(config.getProperty(DOWNLOAD_DIRECTORY), configDir.getParent() + File.separator);
            config.setProperty(DOWNLOAD_DIRECTORY, downloadDestination);
            // create new config file
            ConfigFileHandler.saveConfigFile(config);
            log.info("Saved new configuration file in: " + configDir);
        }

        return config;
    }
}
