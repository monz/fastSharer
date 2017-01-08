package net.impl;

import local.ServiceLocator;
import net.ShareService;
import net.data.DownloadRequestResult;
import net.decl.Worker;

import java.util.List;
import java.util.logging.Logger;

public class DownloadResultWorker extends Worker<List<DownloadRequestResult>> {
    private static final Logger log = Logger.getLogger(DownloadRequestWorker.class.getName());
    private static final ShareService SHARE_SERVICE = (ShareService) ServiceLocator.getInstance().getService(ServiceLocator.SHARE_SERVICE);

    public DownloadResultWorker(List<DownloadRequestResult> data) {
        super(data);
    }

    @Override
    public void serve() {
        log.info(String.format("Received download request result for file '%s'", data.get(0).getFileId()));
        data.forEach(SHARE_SERVICE::addDownload);
    }
}
