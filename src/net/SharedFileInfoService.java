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

package net;

import data.Chunk;
import data.SharedFile;
import local.ServiceLocator;
import local.SharedFileService;
import net.data.ShareCommand;
import net.decl.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SharedFileInfoService implements Service {
    private static final Logger log = Logger.getLogger(SharedFileInfoService.class.getName());

    private static final NetworkService NETWORK_SERVICE = (NetworkService) ServiceLocator.getInstance().getService(ServiceLocator.NETWORK_SERVICE);
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);

    private ScheduledExecutorService sender;

    private long period;

    public SharedFileInfoService(long period) {
        this.period = period;
        this.sender = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void start() {
        sender.scheduleAtFixedRate(sendSharedFileInfo, 0, period, TimeUnit.MILLISECONDS);
    }

    private Runnable sendSharedFileInfo = () -> {
        try {
            SHARED_FILE_SERVICE.getAll().values().forEach(sf -> {
                ShareCommand<SharedFile> msg = new ShareCommand<>(ShareCommand.ShareCommandType.PUSH_SHARE_LIST);

                // add local node as replica node for all local chunks
                List<String> chunkSums = new ArrayList<>();
                sf.getMetadata().getChunks().stream()
                    .filter(Chunk::isLocal)
                    .forEach(c -> chunkSums.add(c.getChecksum()));
                sf.addReplicaNode(NETWORK_SERVICE.getLocalNodeId(), chunkSums);

                msg.addData(sf);

                NETWORK_SERVICE.broadcast(msg);
            });
        } catch (Exception e) {
            log.log(Level.SEVERE, "Ooops!", e);
        }
    };

    @Override
    public void stop() {
        sender.shutdown();
    }
}
