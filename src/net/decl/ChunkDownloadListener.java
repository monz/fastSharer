package net.decl;

import data.SharedFile;

public interface ChunkDownloadListener {
    public void chunkDownloaded(SharedFile sharedFile, boolean fileComplete);
}
