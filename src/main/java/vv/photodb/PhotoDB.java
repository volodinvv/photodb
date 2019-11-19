package vv.photodb;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.avi.AviDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mov.metadata.QuickTimeMetadataDirectory;
import com.drew.metadata.mp4.Mp4Directory;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;

/**
 * Hello world!
 */
public class PhotoDB {

    private static final Map<String, String> EQUIPMENT_MAP = Map.of("Canon PowerShot SX230 HS", "Canon", "SM-A520F", "MobileA5");
    private static final SimpleDateFormat formatter;
    private static final Set<Object> SKIP_EXT = new HashSet<>();

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SKIP_EXT.add("index");
        SKIP_EXT.add("txt");
        SKIP_EXT.add("ini");
        SKIP_EXT.add("info");
        SKIP_EXT.add("db");
    }

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
                Metadata metadata = readMetadata(Path.of(path), null);
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

    private static void processDir(Connection conn, Path path) throws IOException, NoSuchAlgorithmException, SQLException {

        String defaultEquipment = getDeafultEquipment(path);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    processDir(conn, entry);
                    continue;
                }
                System.out.println(entry.toFile());
                String ext = getFileExtension(entry.toString());
                if (ext != null && !SKIP_EXT.contains(ext)) {
                    Metadata metadata = readMetadata(entry, defaultEquipment);
                    save(conn, entry.toFile(), metadata.createDate, MD5Util.MD5(entry), metadata.equipment);
                }
            }
        }
    }

    private static String getDeafultEquipment(Path path) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                String ext = getFileExtension(entry.toString());
                if (ext != null && !SKIP_EXT.contains(ext)) {
                    try {
                        return ImageMetadataReader.readMetadata(entry.toFile()).getFirstDirectoryOfType(ExifIFD0Directory.class).getString(0x0110);
                    } catch (Exception e) {
                        //ignore
                    }
                }
            }
        }
        return null;
    }

    private static String getFileExtension(String entry) {
        int idx = entry.lastIndexOf('.');
        if (idx > 0) {
            return entry.substring(idx + 1).toLowerCase();
        }
        return null;
    }

    private static Metadata readMetadata(Path entry, String defaultEquipment) {
        Metadata meta = new Metadata(defaultEquipment);
        try {

            com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(entry.toFile());
            //// if (entry.toFile().getName().endsWith("MOV")) {
            metadata.getDirectories().forEach(d -> {
                System.out.println(d.getName() + " " + d.getClass());
                d.getTags().forEach(t -> System.out.println("\t" + t.getTagTypeHex() + " : " + t.toString()));
            });
            //   }


            if (metadata.containsDirectoryOfType(Mp4Directory.class)) {
                meta.createDate = metadata.getFirstDirectoryOfType(Mp4Directory.class).getDate(Mp4Directory.TAG_CREATION_TIME);
            }
            if (metadata.containsDirectoryOfType(AviDirectory.class)) {
                String aviDate = metadata.getFirstDirectoryOfType(AviDirectory.class).getString(AviDirectory.TAG_DATETIME_ORIGINAL);
                meta.createDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy").parse(aviDate);
            }
            if (metadata.containsDirectoryOfType(ExifSubIFDDirectory.class)) {
                meta.createDate = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class).getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            }
            if (metadata.containsDirectoryOfType(ExifIFD0Directory.class)) {
                meta.equipment = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class).getString(0x0110);
            }

            if (metadata.containsDirectoryOfType(QuickTimeDirectory.class)) {
                Date origDate = metadata.getFirstDirectoryOfType(QuickTimeDirectory.class).getDate(QuickTimeDirectory.TAG_CREATION_TIME);
                meta.createDate = new Date(origDate.getTime() + TimeZone.getTimeZone("Europe/Minsk").getOffset(origDate.getTime()));
            }
            if (metadata.containsDirectoryOfType(QuickTimeMetadataDirectory.class)) {
                meta.equipment = metadata.getFirstDirectoryOfType(QuickTimeMetadataDirectory.class).getString(QuickTimeMetadataDirectory.TAG_MODEL);
            }
            if (meta.equipment == null) {
                meta.equipment = "unknown";
            }

        } catch (Exception e) {
            System.out.println("Can't read metadata: " + entry);
        }
        return meta;
    }

    private static void save(Connection conn, File file, Date createDate, String md5, String equipment) throws
            SQLException {
        PreparedStatement st = conn.prepareStatement("INSERT INTO photos " +
                "(path, name, ext, size, created, md5, equipment) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?) ON CONFLICT(path) DO update set created = ?, md5 = ?, equipment = ?");
        st.setString(1, file.getAbsolutePath());
        st.setString(2, file.getName());
        st.setString(3, getFileExtension(file.getName()));
        st.setLong(4, file.length());
        st.setString(5, createDate == null ? null : formatter.format(createDate));
        st.setString(6, md5);
        st.setString(7, equipment);
        st.setString(8, createDate == null ? null : formatter.format(createDate));
        st.setString(9, md5);
        st.setString(10, equipment);
        st.executeUpdate();
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:D:/Private/PhotoDB/db/photodb");
    }

    private static class Metadata {
        public Metadata(String equipment) {
            this.equipment = equipment;
        }

        Date createDate;
        String equipment;
    }
}
