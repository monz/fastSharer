package data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class FileMetadata {
    public static final String UNKNOWN_FILE_ID = null;

    private String fileId;
    private long fileSize;
    private String checksum;
    private String fileName;
    private String filePath;
    private List<Chunk> chunks;

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

    public List<Chunk> getChunks() {
        return chunks;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileId() {
        return fileId;
    }

    public int getExpectedChunkCount() {
        return Chunk.getChunkCount(fileSize);
    }

    public boolean hasChecksum() {
        return checksum != null && ! checksum.equals("");
    }
}
