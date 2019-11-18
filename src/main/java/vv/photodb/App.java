package vv.photodb;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.mp4parser.Box;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.MovieBox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Hello world!
 */
public class App {


    private static Map<String, String> SOURCE_MAP = Map.of("Canon PowerShot SX230 HS", "Canon", "SM-A520F", "MobileA5");

    private static class Args {
        @Parameter(names = "-cmd")
        private String cmd = "scan";

        @Parameter(names = "-dest")
        private String dest = ".";

        @Parameter(names = "-source")
        private String source = ".";
    }

    private static final Args args = new Args();

    public static void main(String[] args) {

        new JCommander(App.args).parse(args);

        switch (App.args.cmd) {
            case "scan":
                scan();
                break;
            case "copy":
                copy();
                break;
        }


    }

    private static void copy() {
        try (Connection conn = getConnection()) {

            ResultSet resultSet = conn.createStatement().executeQuery("select * from photos where not duplicate");
            while (resultSet.next()) {
                String path = resultSet.getString("path");
                String created = resultSet.getString("created");
                String source = resultSet.getString("source");
                String dest = created.substring(0, 4) + "/" + created.substring(5, 7) + "_" + created.substring(8, 10) + "_" + SOURCE_MAP.getOrDefault(source, source);
                Path destPath = Path.of(args.dest, dest);
                Files.createDirectories(destPath);

                Path sourcePath = Path.of(path);
                Path destFile = Path.of(destPath.toString(), sourcePath.getFileName().toString());
                System.out.println(sourcePath + " -> " + destFile);

                try {
                    Files.copy(sourcePath, destFile);

                } catch (FileAlreadyExistsException e) {
                    String destMD5 = getMD5(destFile);
                    String sourceMD5 = resultSet.getString("md5");
                    if (!destMD5.equals(sourceMD5)) {
                        throw e;
                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void scan() {
        try (Connection conn = getConnection()) {
            processDir(conn, Path.of(App.args.source));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processDir(Connection conn, Path path) throws ImageProcessingException, IOException, NoSuchAlgorithmException, SQLException {
        Optional<String> dirModel = Optional.empty();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                try {
                    Metadata metadata = ImageMetadataReader.readMetadata(entry.toFile());
                    dirModel = Optional.of(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class).getString(0x0110));
                    break;
                } catch (Exception e) {
                    //ignore
                }
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {

            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    processDir(conn, entry);
                    continue;
                }

                System.out.println(entry.toFile());

                Date createDate;
                String model;

                if (entry.toFile().getName().endsWith("mp4")) {
                    MovieBox moov = new IsoFile(entry.toFile()).getMovieBox();
                    // for (Box b : moov.getBoxes()) {
                    //System.out.println(b);
                    //}
                    createDate = moov.getMovieHeaderBox().getCreationTime();
                    model = dirModel.orElse("MP4");
                } else {
                    Metadata metadata = ImageMetadataReader.readMetadata(entry.toFile());
                    //metadata.getDirectories().forEach(d -> {
                    // System.out.println(d.getName() + " " + d.getClass());
                    // d.getTags().forEach(t -> System.out.println("\t" + t.getTagTypeHex() + " : " + t.toString()));
                    //});

                    createDate = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class).getDateOriginal();
                    model = dirModel.orElse(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class).getString(0x0110));
                }

                save(conn, entry.toFile(), createDate, getMD5(entry), model);
            }
        }
    }

    private static String getMD5(Path path) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(path);
             DigestInputStream dis = new DigestInputStream(is, md)) {
            dis.readAllBytes();
        }
        byte[] digest = md.digest();
        String md5 = Base64.getEncoder().encodeToString(digest);
        md.reset();
        return md5;
    }

    private static void save(Connection conn, File file, Date createDate, String md5, String model) throws SQLException {
        conn.createStatement().executeUpdate("INSERT INTO photos " +
                "(path, name, size, created, md5, source) " +
                "VALUES('" + file.getAbsolutePath() +
                "', '" + file.getName() +
                "', " + file.length() +
                ", '" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(createDate) +
                "', '" + md5 +
                "', '" + model +
                "') ON CONFLICT(path) DO nothing");
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/db/photodb");
    }
}
