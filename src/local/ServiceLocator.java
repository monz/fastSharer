package local;

import main.Sharer;
import net.DiscoveryService;
import net.NetworkService;

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
    public static final String CHUNK_SUM_SERVICE = "chunkSumService";
    public static final String NETWORK_SERVICE = "networkService";
    public static final String DISCOVERY_SERVICE = "discoveryService";

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
        int maxIncomingConnections = Integer.parseInt(config.getProperty(Sharer.MAX_INCOMING_CONNECTIONS));
        int maxOutgoingConnections = Integer.parseInt(config.getProperty(Sharer.MAX_OUTGOING_CONNECTIONS));
        int discoveryPort = Integer.parseInt(config.getProperty(Sharer.DISCOVERY_PORT));
        long discoveryPeriod = Long.parseLong(config.getProperty(Sharer.DISCOVERY_PERIOD));

        services = new HashMap<>();

        services.put(SHARED_FILE_SERVICE, new SharedFileService());
        services.put(NETWORK_SERVICE, new NetworkService(cmdPort, maxIncomingConnections, maxOutgoingConnections));
        services.put(CHUNK_SUM_SERVICE, new ChunkSumService()); // depends on shared file service
        services.put(FILE_SERVICE, new FileService()); // depends on shared file service, chunk sum service

        try {
            services.put(DISCOVERY_SERVICE, new DiscoveryService(discoveryPort, 0, discoveryPeriod)); // depends on network service
        } catch (SocketException e) {
            log.log(Level.SEVERE, "Could not bind discovery service to port", e);
            System.exit(1);
        }
    }

    public static void init(Properties config) {
        if (instance == null) {
            ServiceLocator.config = config;
            init();
        } else {
            ServiceLocator.config = config;
        }
    }

    public Object getService(String serviceName) {
        Object service = services.get(serviceName);

        if (service == null) {
            throw new RuntimeException("Service not found");
        }

        return service;
    }
}
