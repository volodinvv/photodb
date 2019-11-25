package vv.photodb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
}
