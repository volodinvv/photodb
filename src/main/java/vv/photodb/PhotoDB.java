package vv.photodb;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.iso14496.part12.MovieBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Hello world!
 */
public class PhotoDB {


    private static Map<String, String> EQUIPMENT_MAP = Map.of("Canon PowerShot SX230 HS", "Canon", "SM-A520F", "MobileA5");
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    ;

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

        new JCommander(PhotoDB.args).parse(args);

        switch (PhotoDB.args.cmd) {
            case "scan":
                scan();
                break;
            case "rescanMeta":
                rescanMeta();
                break;
            case "copy":
                copy();
                break;
        }
    }

    private static void copy() {
        try (Connection conn = getConnection()) {
            ResultSet resultSet = conn.createStatement().executeQuery("select * from photos where destination is null and equipment is not null");
            while (resultSet.next()) {

                String path = resultSet.getString("path");
                String created = resultSet.getString("created");
                String equipment = resultSet.getString("equipment");
                String sourceMD5 = resultSet.getString("md5");

                Path destFile = copyFile(path, args.dest, created, equipment, sourceMD5);

                conn.createStatement().executeUpdate("update photos set destination='" + destFile.toString() + "' where path='" + path + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void rescanMeta() {
        try (Connection conn = getConnection()) {
            ResultSet resultSet = conn.createStatement().executeQuery("select * from photos where equipment is not null");
            while (resultSet.next()) {
                String path = resultSet.getString("path");
                String sourceMD5 = resultSet.getString("md5");
                Metadata metadata = readMetadata(Optional.empty(), Path.of(path));
                save(conn, new File(path), metadata.createDate, sourceMD5, metadata.equipment);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Path copyFile(String path, String dest, String created, String equipment, String sourceMD5) throws Exception {
        Path destDir = Path.of(dest,
                created.substring(0, 4),
                created.substring(5, 7) + "_" + created.substring(8, 10) + "_" + EQUIPMENT_MAP.getOrDefault(equipment, equipment));

        if (Files.notExists(destDir)) {
            Files.createDirectories(destDir);
        }

        Path sourceFile = Path.of(path);

        Path destFile = Path.of(destDir.toString(), sourceFile.getFileName().toString());

        if (Files.notExists(destFile)) {
            Files.copy(sourceFile, destFile);
            Files.setLastModifiedTime(destDir, FileTime.fromMillis(formatter.parse(created).getTime()));
            System.out.println("Copied : " + sourceFile + " -> " + destFile);
        } else {
            String destMD5 = MD5Util.MD5(destFile);
            if (!destMD5.equals(sourceMD5)) {
                throw new Exception("MD5 not equals: " + sourceFile + " -> " + destFile);
            }
            System.out.println("Checked: " + sourceFile + " -> " + destFile);

        }
        return destFile;
    }

    private static void scan() {
        try (Connection conn = getConnection()) {
            processDir(conn, Path.of(PhotoDB.args.source));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processDir(Connection conn, Path path) throws ImageProcessingException, IOException, NoSuchAlgorithmException, SQLException {
        Optional<String> dirModel = Optional.empty();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                try {
                    com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(entry.toFile());
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

                Metadata metadata = readMetadata(dirModel, entry);
                save(conn, entry.toFile(), metadata.createDate, MD5Util.MD5(entry), metadata.equipment);
            }
        }
    }

    private static Metadata readMetadata(Optional<String> dirModel, Path entry) throws IOException, ImageProcessingException {
        Metadata meta = new Metadata();
        try {
            if (entry.toFile().getName().endsWith("mp4")) {
                MovieBox moov = new IsoFile(entry.toFile()).getMovieBox();
                // for (Box b : moov.getBoxes()) {
                //System.out.println(b);
                //}
                meta.createDate = moov.getMovieHeaderBox().getCreationTime();
                meta.equipment = dirModel.orElse("MP4");
            } else {
                com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(entry.toFile());
                //metadata.getDirectories().forEach(d -> {
                // System.out.println(d.getName() + " " + d.getClass());
                // d.getTags().forEach(t -> System.out.println("\t" + t.getTagTypeHex() + " : " + t.toString()));
                //});

                meta.createDate = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class).getDateOriginal();
                meta.equipment = dirModel.orElse(metadata.getFirstDirectoryOfType(ExifIFD0Directory.class).getString(0x0110));
            }
        } catch (Exception e) {
            System.out.println("Can't read metadata: " + entry);
        }
        return meta;
    }

    private static void save(Connection conn, File file, Date createDate, String md5, String equipment) throws SQLException {
        PreparedStatement st = conn.prepareStatement("INSERT INTO photos " +
                "(path, name, size, created, md5, equipment) " +
                "VALUES(?, ?, ?, ?, ?, ?) ON CONFLICT(path) DO nothing");
        st.setString(1, file.getAbsolutePath());
        st.setString(2, file.getName());
        st.setLong(3, file.length());
        st.setString(4, createDate == null ? null : formatter.format(createDate));
        st.setString(5, md5);
        st.setString(6, equipment);
        st.executeUpdate();
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/db/photodb");
    }

    private static class Metadata {
        Date createDate;
        String equipment;
    }
}
