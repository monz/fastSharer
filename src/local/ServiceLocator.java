package local;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ServiceLocator {
    public static final String FILE_SERVICE = "fileService";

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
        services = new HashMap<>();

        services.put(FILE_SERVICE, new FileService());
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
