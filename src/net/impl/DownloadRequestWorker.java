package net.impl;

import local.ServiceLocator;
import net.ShareService;
import net.data.DownloadRequest;
import net.decl.Worker;

import java.util.List;
import java.util.logging.Logger;

public class DownloadRequestWorker extends Worker<List<DownloadRequest>> {
    private static final Logger log = Logger.getLogger(DownloadRequestWorker.class.getName());
    private static final ShareService SHARE_SERVICE = (ShareService) ServiceLocator.getInstance().getService(ServiceLocator.SHARE_SERVICE);

    public DownloadRequestWorker(List<DownloadRequest> data) {
        super(data);
    }

    @Override
    public void serve() {
        log.info(String.format("Received download request for file '%s'", data.get(0).getFileId()));
        data.forEach(SHARE_SERVICE::addUpload);
    }
}
