package local;

import data.FileMetadata;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public class FileService {
    private static final Logger log = Logger.getLogger(FileService.class.getName());

    public void calculateFileChunks(FileMetadata fileMetadata) {

        // calculate chunk data async
//        long remainingSize = fileMetadata.getFileSize();
//        int len = (int) Math.min(Chunk.CHUNK_SIZE, remainingSize);
//        while (len > 0) {
//            long offset = fileMetadata.getFileSize() - remainingSize;
//
//            // start new checksum job
//            threadPool.execute(new CalculateChecksumWorker(new ChecksumWorkerData(fileMetadata, offset, len)));
//
//            remainingSize -= len;
//            len = remainingSize - Chunk.CHUNK_SIZE < 0 ? (int) remainingSize : Chunk.CHUNK_SIZE;
//        }
//
//        return;
    }

    public void handleDroppedPaths(List<File> fileList) {
        // todo: implement
    }
}
