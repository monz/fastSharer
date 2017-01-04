package persistence;

import main.Sharer;

import java.io.*;
import java.nio.file.Files;
import java.util.Properties;

public class ConfigFileHandler {
    public static void saveConfigFile(Properties config) throws IOException {
        Files.createDirectories(Sharer.configDir);

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(Sharer.configFile.toFile()));

        config.store(out, "");
        out.close();
    }

    public static Properties loadConfigFile() throws IOException {
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(Sharer.configFile.toFile()));

        Properties config = new Properties();
        config.load(is);
        is.close();

        return config;
    }
}
