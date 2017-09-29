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
