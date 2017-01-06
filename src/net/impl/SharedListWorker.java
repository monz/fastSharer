package net.impl;

import data.SharedFile;
import local.ServiceLocator;
import local.SharedFileService;
import net.decl.Worker;

import java.util.List;
import java.util.logging.Logger;

public class SharedListWorker extends Worker<List<SharedFile>> {
    private static final Logger log = Logger.getLogger(SharedListWorker.class.getName());
    private static final SharedFileService SHARED_FILE_SERVICE = (SharedFileService) ServiceLocator.getInstance().getService(ServiceLocator.SHARED_FILE_SERVICE);

    public SharedListWorker(List data) {
        super(data);
    }

    @Override
    public void serve() {
//        // only consider replica nodes which are in this machines node list
//        data.stream().map(sf -> {
//            sf.getReplicaNodes().keySet().stream()
//                .filter(uuid -> NETWORK_SERVICE.getNode(uuid) == null)
//                .collect(Collectors.toMap(uuid -> uuid, uuid -> sf.getReplicaNodes().get(uuid)))
//                .forEach((uuid, chunks) -> sf.removeReplicaNode(uuid));
//            log.info("Added remote file with replica nodes: " + sf.getReplicaNodes().keySet());
//            return sf;
//        }).forEach(SHARED_FILE_SERVICE::handleRemotePath);

        log.info("Received remote file: " + data.get(0).getFilename());
        data.forEach(SHARED_FILE_SERVICE::addRemoteFile);
    }
}
