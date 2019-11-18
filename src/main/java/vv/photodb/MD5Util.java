package vv.photodb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class MD5Util {
    public static String MD5(Path path) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (DigestInputStream dis = new DigestInputStream(Files.newInputStream(path), md)) {
            dis.readAllBytes();
            byte[] digest = md.digest();
            md.reset();
            return Base64.getEncoder().encodeToString(digest);
        }
    }
}
