package data;

import java.nio.file.Paths;

public class FileMetadata {
    public static final String UNKNOWN_FILE_ID = null;

    private String fileId;
    private long fileSize;
    private String checksum;
    private String fileName;
    private String filePath;

    public FileMetadata(String fileId, String filePath) {
        this.fileId = fileId;
        this.filePath = filePath;
        this.fileName = Paths.get(filePath).getFileName().toString();
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
}
