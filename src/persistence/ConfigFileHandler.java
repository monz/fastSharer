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
