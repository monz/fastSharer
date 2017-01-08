package data;

import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final String UNKNOWN_CHECKSUM = null;
    public static final int CHUNK_SIZE = 1024 * 1024 * 64; // 64 MByte

    @Expose private String checksum;
    @Expose private long offset;
    @Expose private long size;

    private String fileId;
    private boolean isLocal;
    private boolean downloadActive;

    public Chunk(String fileId, long offset, long size) {
        this.fileId = fileId;
        this.checksum = UNKNOWN_CHECKSUM;
        this.offset = offset;
        this.size = size;
        this.isLocal = true;
        this.downloadActive = false;
    }

    public String getFileId() {
        return fileId;
    }

    synchronized public void setLocal(boolean isLocal) {
        this.isLocal = isLocal;
    }

    synchronized public boolean activateDownload() {
        boolean success;
        if (downloadActive) {
            success = false;
        } else {
            downloadActive = true;
            success = true;
        }
        return success;
    }
    synchronized public boolean deactivateDownload() {
        boolean success;
        if (downloadActive) {
            downloadActive = false;
            success = true;
        } else {
            success = false;
        }
        return success;
    }

    synchronized public boolean isDownloadActive() {
        return downloadActive;
    }

    synchronized public boolean isLocal() {
        return isLocal;
    }

    synchronized public String getChecksum() {
        return checksum;
    }

    synchronized public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    synchronized public long getOffset() {
        return offset;
    }

    synchronized public long getSize() {
        return size;
    }

    @Override
    synchronized public boolean equals(Object o) {
        return this.checksum.equals(((Chunk) o).getChecksum());
    }

    public static int getChunkCount(long fileSize) {
        return (int) Math.ceil(fileSize / (double) CHUNK_SIZE);
    }

    public static List<Chunk> getChunks(String fileId, long fileSize) throws IOException {
        List<Chunk> chunks = new ArrayList<>(getChunkCount(fileSize));

        long remainingSize = fileSize;
        int size = (int) Math.min(CHUNK_SIZE, remainingSize);
        while (size > 0) {
            long offset = fileSize - remainingSize;

            // add chunk
            chunks.add(new Chunk(fileId, offset, size));

            remainingSize -= size;
            size = (remainingSize - CHUNK_SIZE) < 0 ? (int) remainingSize : CHUNK_SIZE;
        }

        return chunks;
    }

    public boolean hasChecksum() {
        return checksum != null && ! checksum.equals("");
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}