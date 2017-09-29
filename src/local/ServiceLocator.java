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

package local;

import main.Sharer;
import net.*;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceLocator {
    private static final Logger log = Logger.getLogger(ServiceLocator.class.getName());

    public static final String FILE_SERVICE = "fileService";
    public static final String SHARED_FILE_SERVICE = "sharedFileService";
    public static final String CHECKSUM_SERVICE = "checksumService";
    public static final String NETWORK_SERVICE = "networkService";
    public static final String DISCOVERY_SERVICE = "discoveryService";
    public static final String SHARED_FILE_INFO_SERVICE = "shareFileInfoService";
    public static final String SHARED_CMD_RECEIVER_SERVICE = "shareCmdReceiverService";
    public static final String SHARE_SERVICE = "shareService";

    private static Map<String, Object> services;
    private static ServiceLocator instance;
    private static Properties config;

    private ServiceLocator() {
        // private constructor. Nobody should create own instance
    }

    public static ServiceLocator getInstance() {
        if (instance == null) {
            if (config == null) {
                throw new RuntimeException("Service Locator was not initialized.");
            }
            instance = new ServiceLocator();
        }
        return instance;
    }

    private static void init() {

        int cmdPort = Integer.parseInt(config.getProperty(Sharer.CMD_PORT));
        int maxConcurrentDownloads = Integer.parseInt(config.getProperty(Sharer.MAX_DOWNLOADS));
        int maxConcurrentUploads = Integer.parseInt(config.getProperty(Sharer.MAX_UPLOADS));
        int discoveryPort = Integer.parseInt(config.getProperty(Sharer.DISCOVERY_PORT));
        long discoveryPeriod = Long.parseLong(config.getProperty(Sharer.DISCOVERY_PERIOD));
        long shareInfoPeriod = Long.parseLong(config.getProperty(Sharer.SHARE_INFO_PERIOD));
        String downloadDirectory = config.getProperty(Sharer.DOWNLOAD_DIRECTORY);
        String checksumAlgorithm = config.getProperty(Sharer.CHECKSUM_ALGORITHM);

        services = new HashMap<>();

        services.put(SHARED_FILE_SERVICE, new SharedFileService(downloadDirectory, checksumAlgorithm));
        services.put(NETWORK_SERVICE, new NetworkService(cmdPort)); // depends on shared file service
        services.put(SHARED_FILE_INFO_SERVICE, new SharedFileInfoService(shareInfoPeriod)); // depends on network service, shared file service
        services.put(CHECKSUM_SERVICE, new ChecksumService(checksumAlgorithm)); // depends on shared file service
        services.put(SHARE_SERVICE, new ShareService(maxConcurrentDownloads, maxConcurrentUploads, checksumAlgorithm)); // depends on network service, checksum service
        services.put(FILE_SERVICE, new FileService()); // depends on shared file service, chunk sum service

        try {
            services.put(DISCOVERY_SERVICE, new DiscoveryService(discoveryPort, 0, discoveryPeriod)); // depends on network service
        } catch (SocketException e) {
            log.log(Level.SEVERE, "Could not bind discovery service to port", e);
            System.exit(1);
        }

        try {
            services.put(SHARED_CMD_RECEIVER_SERVICE, new ShareCommandReceiverService(cmdPort));
        } catch (IOException e) {
            log.log(Level.SEVERE, "Could not bind share command receiver service to port", e);
        }

    }

    public static void init(Properties config) {
        ServiceLocator.config = config;

        if (instance == null) {
            init();
        }
    }

    public Object getService(String serviceName) {
        Object service = services.get(serviceName);

        if (service == null) {
            throw new RuntimeException("Service not found");
        }

        return service;
    }

    public static void updateConfig(Properties config) {
        // todo: implement
        // stop/shutdown all services
        // init services with new config
        // restart all services
    }
}
