package vv.photodb;

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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Scanner {

    private static final Set<Object> SKIP_EXT = Set.of("index", "txt", "ini",
            "info", "db", "amr", "ctg", "ithmb", "nri", "scn", "thm", "xml");

    public static Long totalProcessed = 0L;
    public static Long startProcessing = 0L;

    static void rescanMeta(String tableForSave) {
        try (Connection conn = PhotosDAO.getConnection()) {
            ResultSet resultSet = conn.createStatement().executeQuery("select * from " + tableForSave +
                    " where created is null");
            while (resultSet.next()) {
                String path = resultSet.getString("path");
                String sourceMD5 = resultSet.getString("md5");
                Metadata metadata = readMetadata(Path.of(path), null);
                String comment = readComments(Path.of(path));
                PhotosDAO.save(conn, tableForSave, new File(path), metadata.createDate, sourceMD5, metadata.equipment, comment);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void scan(String source, String tableForSave) {
        startProcessing = System.currentTimeMillis();
        try (Connection conn = PhotosDAO.getConnection()) {
            processDir(conn, Path.of(source), tableForSave);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processDir(Connection conn, Path path, String tableForSave) throws IOException, NoSuchAlgorithmException, SQLException {
        String defaultEquipment = getDefaultEquipment(path);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    processDir(conn, entry, tableForSave);
                } else {
                    if (PhotoDB.args.resume && PhotosDAO.isSaved(conn, entry.toString(), tableForSave)) {
                        continue;
                    }
                    String ext = Utils.getFileExtension(entry.toString());
                    if (ext != null && !SKIP_EXT.contains(ext)) {
                        Metadata metadata = readMetadata(entry, defaultEquipment);
                        String comment = readComments(entry);
                        PhotosDAO.save(conn, tableForSave, entry.toFile(), metadata.createDate, Utils.MD5(entry), metadata.equipment, comment);
                        totalProcessed += entry.toFile().length();
                        System.out.println(((System.currentTimeMillis() - startProcessing) / 1000) + "s Scanned: " + (totalProcessed >> 20) + "M file:" + entry.toFile());
                    }
                }
            }
        }
    }

    private static String getDefaultEquipment(Path path) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                String ext = Utils.getFileExtension(entry.toString());
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

    public static String readComments(Path entry) {
        String result = readComment(entry.getParent().getFileName().toString());
        if (result == null) {
            result = readComment(entry.getParent().getParent().getFileName().toString());
        }
        return result;
    }

    private static String readComment(String parent) {
        String result = null;
        boolean hasComment = false;
        for (int i = 0; i < parent.length(); i++) {
            char ch = parent.charAt(i);
            if (!hasComment && (ch > 'z')) {
                hasComment = true;
                result = "";
            }
            if (hasComment) {
                result += ch;
            }
        }
        return result;
    }

    private static Metadata readMetadata(Path entry, String defaultEquipment) {
        Metadata meta = new Metadata(defaultEquipment);
        try {

            com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(entry.toFile());
            // if (entry.toFile().getName().endsWith("MOV")) {
            //metadata.getDirectories().forEach(d -> {
            //    System.out.println(d.getName() + " " + d.getClass());
            //    d.getTags().forEach(t -> System.out.println("\t" + t.getTagTypeHex() + " : " + t.toString()));
            //});
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

    static void copy(String dest) {
        startProcessing = System.currentTimeMillis();
        try (Connection conn = PhotosDAO.getConnection()) {
            String sql = "select path, created, COALESCE (alias,equipment,'unknown') equipment, md5, comment " +
                    "from photos_for_copy p left join equipments e using(equipment) " +
                    "where destination is null and created is not null";
            ResultSet resultSet = conn.createStatement().executeQuery(sql);
            while (resultSet.next()) {

                String path = resultSet.getString("path");
                String created = resultSet.getString("created");
                String equipment = resultSet.getString("equipment");
                String comment = resultSet.getString("comment");
                String sourceMD5 = resultSet.getString("md5");

                Path destFile = copyFile(path, dest, created, equipment, comment, sourceMD5);

                conn.createStatement().executeUpdate("update photos set destination='" + destFile.toString() + "' where path='" + path + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Path copyFile(String path, String dest, String created, String equipment, String comment, String sourceMD5) throws Exception {

        String commentVal = comment != null ? "_" + comment : "";
        String equipmentVal = equipment != null ? "_" + equipment : "";
        String dirName = created.substring(5, 7) + "_" + created.substring(8, 10) + equipmentVal + commentVal;

        Path destDir = Path.of(dest, created.substring(0, 4), dirName);

        if (Files.notExists(destDir)) {
            Files.createDirectories(destDir);
        }

        Path sourceFile = Path.of(path);
        Path destFile = destDir.resolve(sourceFile.getFileName());

        totalProcessed += sourceFile.toFile().length();

        if (Files.notExists(destFile)) {
            Files.copy(sourceFile, destFile);
            Files.setLastModifiedTime(destDir, FileTime.fromMillis(Utils.formatter.parse(created).getTime()));

            System.out.println(((System.currentTimeMillis() - startProcessing) / 1000) + "s Copied: " + (totalProcessed >> 20) + "M files: " + sourceFile + " -> " + destFile);
        } else {
            String destMD5 = Utils.MD5(destFile);
            if (!destMD5.equals(sourceMD5)) {
                throw new Exception("MD5 not equals: " + sourceFile + " -> " + destFile);
            }

            System.out.println("Checked: " + sourceFile + " -> " + destFile);
        }
        return destFile;
    }
}
