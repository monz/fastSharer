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

public class SharedFileInfoService implements Service {
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

    private Runnable sendSharedFileInfo = () ->
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

    @Override
    public void stop() {
        sender.shutdown();
    }
}
