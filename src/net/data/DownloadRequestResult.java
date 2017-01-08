package net.data;

import com.google.gson.annotations.Expose;

public class DownloadRequestResult {

    @Expose private String fileId;
    @Expose private String nodeId;
    @Expose private String chunkChecksum;
    @Expose private int downloadPort;

    public DownloadRequestResult(String fileId, String nodeId, String chunkChecksum, int downloadPort) {
        this.fileId = fileId;
        this.nodeId = nodeId;
        this.chunkChecksum = chunkChecksum;
        this.downloadPort = downloadPort;
    }

    public String getFileId() {
        return fileId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getChunkChecksum() {
        return chunkChecksum;
    }

    public int getDownloadPort() {
        return downloadPort;
    }
}
