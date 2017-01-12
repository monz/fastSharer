package net.data;

import com.google.gson.annotations.Expose;

public class DownloadRequest {
    @Expose private String fileId;
    @Expose private String nodeId;
    @Expose private String chunkChecksum;

    public DownloadRequest(String fileId, String nodeId, String chunkChecksum) {
        this.fileId = fileId;
        this.nodeId = nodeId;
        this.chunkChecksum = chunkChecksum;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getFileId() {
        return fileId;
    }

    public String getChunkChecksum() {
        return chunkChecksum;
    }
}