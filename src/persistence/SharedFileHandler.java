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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import data.SharedFile;
import local.ServiceLocator;
import net.NetworkService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SharedFileHandler {
    private static final Logger log = Logger.getLogger(SharedFileHandler.class.getName());
    private static String SHARER_EXTENSION = ".sharer";
    private static final UUID LOCAL_NODE_ID = ((NetworkService) ServiceLocator.getInstance().getService(ServiceLocator.NETWORK_SERVICE)).getLocalNodeId();

    public static List<SharedFile> loadSharedFiles(Path source) {
        List<SharedFile> sharedFiles = new ArrayList<>();

        try {
            if(! Files.exists(source)) {
                log.warning("Directory '" + source.toString() + "' does not exist.");
                return sharedFiles;
            }
            if (Files.list(source).count() == 0) {
                log.info("Directory does not contain any sharer files");
                return sharedFiles;
            }
            Files.list(source)
                .filter(f -> {
                    String filename = f.getFileName().toString();
                    String ext = (filename.lastIndexOf(".") == -1 || filename.lastIndexOf(".") == 0) ? "" : filename.substring(filename.lastIndexOf("."));
                    return ext.equals(SHARER_EXTENSION);
                })
                .forEach(f -> {
                    try {
                        sharedFiles.add(loadSharedFile(f.toString()));
                    } catch (IOException e) {
                        log.log(Level.WARNING, "Could not load sharer file: " + f.getFileName(), e);
                    }
                });
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not load shared files", e);
        }

        return sharedFiles;
    }

    public static SharedFile loadSharedFile(String filePath) throws IOException {
        Path sharerFile = Paths.get(filePath);

        SharedFile sharedFile = null;
        if(! Files.exists(sharerFile)) {
            log.warning("File '" + filePath + "' does not exist.");
            return sharedFile;
        }
        BufferedReader in = new BufferedReader(new FileReader(filePath));
        String line = in.readLine();

        sharedFile = deserialize(line);
        sharedFile.resetReplicaNodes();
        sharedFile.addReplicaNode(LOCAL_NODE_ID, sharedFile.getAllChunkChecksums(), true);

        in.close();
        log.info("Loaded shared file: " + sharerFile.getFileName());

        return sharedFile;
    }

    public static void saveShareFile(SharedFile shareFile, Path destination) {
        try {
            Files.createDirectories(destination);
            Path outFile = Paths.get(destination.toString(), shareFile.getFilename() + ".sharer");
            BufferedWriter out = new BufferedWriter(new FileWriter(outFile.toFile()));
            out.write(serialize(shareFile));
            out.close();
            log.info("Saved shared file: " + shareFile.getFilename());
        } catch (IOException e) {
            log.log(Level.WARNING, "Could not save SharedFile: " + shareFile.getFilename(), e);
        }
    }

    public static void saveShareFiles(Collection<SharedFile> shareFiles, Path destination) {
        shareFiles.forEach(sf -> saveShareFile(sf, destination));
    }

    private static String serialize(SharedFile shareFile) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        return gson.toJson(shareFile);
    }

    private static SharedFile deserialize(String shareFileString) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        return gson.fromJson(shareFileString, SharedFile.class);
    }

}
