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

    public SharedListWorker(List<SharedFile> data) {
        super(data);
    }

    @Override
    public void serve() {
        log.info(String.format("Received remote file: %s", data.get(0).getFilename()));
        data.stream().forEach(SHARED_FILE_SERVICE::addRemoteFile);
    }
}
