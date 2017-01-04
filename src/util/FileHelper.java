package util;

public class FileHelper {
    private static final int MD5_LENGTH = 32;

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
}
