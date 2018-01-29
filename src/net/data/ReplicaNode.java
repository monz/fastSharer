/*
 * Copyright (c) 2018. Markus Monz
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

package net.data;

import com.google.gson.annotations.Expose;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ReplicaNode {
    @Expose UUID id;
    @Expose Set<String> chunks = new HashSet<>();
    @Expose boolean isComplete = false; // received all chunk information
    boolean stopSharedInfo = false;

    public ReplicaNode() {
        // empty constructor is required for GSON
        // without this constructor the class member wont get initialized
        // e.g. replica nodes is null
    }

    public ReplicaNode(UUID id, List<String> chunks, boolean isComplete) {
        this.id = id;
        this.chunks.addAll(chunks);
        this.isComplete = isComplete;
    }

    public Set<String> getChunks() {
        return chunks;
    }

    public boolean contains(String chunk) {
        return chunks.contains(chunk);
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setStopSharedInfo(boolean stopSharedInfo) {
        this.stopSharedInfo = stopSharedInfo;
    }

    public boolean isStopSharedInfo() {
        return stopSharedInfo;
    }
}
