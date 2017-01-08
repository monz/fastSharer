package data;

import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class FileMetadata {
    @Expose private String fileId;
    @Expose private long fileSize;
    @Expose private String checksum;
    @Expose private String fileName;
    @Expose private List<Chunk> chunks;

    private String filePath;

    public FileMetadata(String filePath) throws IOException {
        this(UUID.randomUUID().toString(), filePath);
    }

    public FileMetadata(String fileId, String filePath) throws IOException {
        this.fileId = fileId;
        this.filePath = filePath;
        this.fileName = Paths.get(filePath).getFileName().toString();
        this.fileSize = Files.size(Paths.get(filePath));
        this.chunks = Chunk.getChunks(fileId, fileSize);
    }

    synchronized public List<Chunk> getChunks() {
        return chunks;
    }

    synchronized public boolean isChunkLocal(String checksum) {
        return chunks.stream()
            .filter(c -> c.getChecksum().equals(checksum))
            .anyMatch(Chunk::isLocal);
    }

    synchronized public String getChecksum() {
        return checksum;
    }

    synchronized public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    synchronized public String getFilePath() {
        return filePath;
    }

    synchronized public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileId() {
        return fileId;
    }

    synchronized public boolean hasChecksum() {
        return checksum != null && ! checksum.equals("");
    }
}
