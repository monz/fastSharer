package data;

public class SharedFile {
    private FileMetadata metadata;

    public SharedFile(FileMetadata metadata) {
        this.metadata = metadata;
    }

    public String getFilePath() {
        return metadata.getFilePath();
    }
}
