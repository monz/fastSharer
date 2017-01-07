package net.data;

import com.google.gson.annotations.Expose;

public class DownloadRequest {
    @Expose private String nodeId;
    @Expose private String fileId;
    @Expose private String chunkChecksum;

    public DownloadRequest(String nodeId, String fileId, String chunkChecksum) {
        this.nodeId = nodeId;
        this.fileId = fileId;
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
