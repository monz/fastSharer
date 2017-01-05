package main;

import local.ServiceLocator;
import local.SharedFileService;
import net.DiscoveryService;
import net.NetworkService;
import persistence.ConfigFileHandler;
import ui.Overview;
import ui.controller.OverviewController;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

public class Sharer {
    public static final Path configDir = Paths.get(System.getProperty("user.home"), ".sharer");
    public static final Path configFile = Paths.get(configDir.toString(), "config.properties");

    public static final String DISCOVERY_PORT = "sharer_discovery_port";
    public static final String DISCOVERY_PERIOD = "sharer_discovery_period";
    public static final String NODE_CLEANUP_RATE = "sharer_node_cleanup_period";
    public static final String CMD_PORT = "sharer_cmd_port";

    public static final String MAX_INCOMING_CONNECTIONS = "sharer_max_incoming_connections";
    public static final String MAX_OUTGOING_CONNECTIONS = "sharer_max_outgoing_connections";
    public static final String DOWNLOAD_DIRECTORY = "sharer_download_directory";

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

        // init service locator - start services
        ServiceLocator.init(config);
        ServiceLocator serviceLocator = ServiceLocator.getInstance();

        DiscoveryService discoveryService = ((DiscoveryService) serviceLocator.getService(ServiceLocator.DISCOVERY_SERVICE));
        discoveryService.start();

        // set Sharer id on gui
        NetworkService networkService = ((NetworkService)serviceLocator.getService(ServiceLocator.NETWORK_SERVICE));
        OverviewController.getInstance().updateSharerId(networkService.getLocalNodeId().toString());

        // register observer for gui
        ((SharedFileService)serviceLocator.getService(ServiceLocator.SHARED_FILE_SERVICE)).addObserver(OverviewController.getInstance());
        networkService.addObserver(OverviewController.getInstance());

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
