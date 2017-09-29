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
