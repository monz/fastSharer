package util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class FileHelper {
    private static final Logger log = Logger.getLogger(FileHelper.class.getName());
    private static final int MD5_LENGTH = 32;
    private static final int BUFFER_SIZE = 4096;

    public static String digestToString(byte[] digest) {

        // convert byte[] to string; hex value
        StringBuffer checksum = new StringBuffer(MD5_LENGTH);
        String s;
        for (byte b : digest) {
            s = Integer.toHexString(0xFF & b);
            if (s.length() == 1) {
                checksum.append('0');
            }
            checksum.append(s);
        }

        return checksum.toString();
    }

    public static String calculateChecksum(String filePath, String checksumAlgorithm) throws IOException {
        // open file
        BufferedInputStream is = new BufferedInputStream((new FileInputStream(filePath)));

        MessageDigest md;
        try {
            md = MessageDigest.getInstance(checksumAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        byte[] buf = new byte[BUFFER_SIZE];
        int len;
        while ( (len = is.read(buf)) >= 0 ) {
            md.update(buf, 0, len);
        }
        is.close();

        return digestToString(md.digest());
    }
}
