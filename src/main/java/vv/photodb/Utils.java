package vv.photodb;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    public static final SimpleDateFormat formatter;
    public static final SimpleDateFormat aviFormatter;


    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.UK);
        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        aviFormatter = new SimpleDateFormat("MMM dd HH:mm:ss yyyy");

    }

    public static String MD5(Path path) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (DigestInputStream dis = new DigestInputStream(Files.newInputStream(path), md)) {
            dis.readNBytes(10 << 20);
            byte[] digest = md.digest();
            md.reset();
            return Base64.getEncoder().encodeToString(digest);
        }
    }

    public static String getFileExtension(String entry) {
        int idx = entry.lastIndexOf('.');
        if (idx > 0) {
            return entry.substring(idx + 1).toLowerCase();
        }
        return null;
    }

    public static boolean equalsFiles(String file1, String file2) {
        long start = System.nanoTime();
        try (BufferedInputStream fis1 = new BufferedInputStream(new FileInputStream(file1));
             BufferedInputStream fis2 = new BufferedInputStream(new FileInputStream(file2));) {
            int b1 = 0, b2 = 0, pos = 1;
            while (b1 != -1 && b2 != -1) {
                if (b1 != b2) {
                    System.out.println("Files differ at position " + pos + ": " + file1 + " != " + file2);
                    return false;
                }
                pos++;
                b1 = fis1.read();
                b2 = fis2.read();
            }
            if (b1 != b2) {
                System.out.println("Files have different length: " + file1 + " != " + file2);
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void findEmptyFolders(Path root) {


    }

    private void processDir(PhotosDAO dao, Path path) throws IOException, SQLException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    processDir(dao, entry);
                } else {

                }
            }
        }
    }

}
