package vv.photodb;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class Scanner {

    private static final Set<Object> SKIP_EXT = Set.of("index", "txt", "ini",
            "info", "db", "amr", "ctg", "ithmb", "nri", "scn", "thm", "xml");

    public static Long totalSize = 0L;
    public static Long totalCount = 0L;

    public static Long startProcessing = 0L;

    static void rescanMeta(String tableForSave) {
        startProcessing = System.currentTimeMillis();
        try (Connection conn = PhotosDAO.getConnection()) {
            ResultSet resultSet = conn.createStatement().executeQuery("select * from " + tableForSave +
                    " where created is null");
            while (resultSet.next()) {
                String path = resultSet.getString("path");
                String defaultEquipment = resultSet.getString("equipment");
                Path entry = Path.of(path);
                PhotoInfo photoInfo = new PhotoInfoBuilder()
                        .readFileInfo(entry)
                        .readMetadata(entry, defaultEquipment)
                        .readComments(entry)
                        .addMD5(entry).build();
                PhotosDAO.save(conn, tableForSave, photoInfo);
                totalSize += photoInfo.size;
                totalCount++;
                System.out.println("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Scanned: " + totalCount + " Size:" + (totalSize >> 20) + "M " + photoInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void calculateFolder(String tableForSave) {
        startProcessing = System.currentTimeMillis();
        try (Connection conn = PhotosDAO.getConnection()) {
            ResultSet resultSet = conn.createStatement().executeQuery("select * from " + tableForSave);
            while (resultSet.next()) {
                String path = resultSet.getString("path");

                String folder = StringUtils.removeStart(path, "F:\\wireless\\Фото\\");

                folder = StringUtils.removeStart(folder, "F:\\Фото\\");
                folder = StringUtils.removeStart(folder, "F:\\Фото\\Фото\\");

                folder = StringUtils.substringBeforeLast(folder, "\\");

                System.out.println(path + " -> " + folder);

                PhotosDAO.updateFolder(conn, tableForSave, path, folder);
                totalCount++;
                //System.out.println("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Scanned: " + totalCount + " Size:" + (totalSize >> 20) + "M " + photoInfo);
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
                    if (!skipFile(entry)) {
                        PhotoInfo photoInfo = new PhotoInfoBuilder()
                                .readFileInfo(entry)
                                .readMetadata(entry, defaultEquipment)
                                .readComments(entry)
                                .addMD5(entry).build();
                        PhotosDAO.save(conn, tableForSave, photoInfo);
                        totalSize += photoInfo.size;
                        totalCount++;

                        System.out.println("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Scanned: " + totalCount + " Size:" + (totalSize >> 20) + "M " + photoInfo);
                    }
                }
            }
        }
    }

    private static boolean skipFile(Path entry) {
        String name = entry.getFileName().toString();
        String ext = Utils.getFileExtension(name);
        return ext == null || SKIP_EXT.contains(ext) || name.startsWith(".");
    }

    private static String getDefaultEquipment(Path path) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (!skipFile(entry)) {
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

    static void copy(String dest) {
        startProcessing = System.currentTimeMillis();
        try (Connection conn = PhotosDAO.getConnection()) {
            String sql = "select path, name, created, COALESCE (alias,equipment) equipment, md5, comment " +
                    "from photos_for_copy p left join equipments e using(equipment) " +
                    "where destination is null and created is not null";
            ResultSet resultSet = conn.createStatement().executeQuery(sql);
            while (resultSet.next()) {

                String path = resultSet.getString("path");
                String name = resultSet.getString("path");
                String created = resultSet.getString("created");
                String equipment = resultSet.getString("equipment");
                String comment = resultSet.getString("comment");
                String sourceMD5 = resultSet.getString("md5");

                Path destFile = copyFile(path, dest, name, created, equipment, comment, sourceMD5);

                conn.createStatement().executeUpdate("update photos set destination='" + destFile.toString() + "' where path='" + path + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Path copyFile(String path, String dest, String name, String created, String equipment, String comment, String sourceMD5) throws Exception {

        String commentVal = comment != null ? "_" + comment : "";
        String equipmentVal = equipment != null && !equipment.equals("unknown") ? "_" + equipment : "";
        String dirName = created.substring(5, 7) + "_" + created.substring(8, 10) + equipmentVal + commentVal;

        Path destDir = Path.of(dest, created.substring(0, 4), dirName);

        if (Files.notExists(destDir)) {
            Files.createDirectories(destDir);
        }

        Path sourceFile = Path.of(path);
        Path destFile = destDir.resolve(name);

        totalSize += sourceFile.toFile().length();
        totalCount++;

        if (Files.notExists(destFile)) {
            Files.copy(sourceFile, destFile);
            Files.setLastModifiedTime(destDir, FileTime.fromMillis(Utils.formatter.parse(created).getTime()));

            System.out.println(((System.currentTimeMillis() - startProcessing) / 1000) + "s Copied: " + totalCount + " Size:" + (totalSize >> 20) + "M Files: " + sourceFile + " -> " + destFile);
        } else {
            String destMD5 = Utils.MD5(destFile);
            if (!destMD5.equals(sourceMD5)) {
                throw new Exception("MD5 not equals: " + sourceFile + " -> " + destFile);
            }

            System.out.println(((System.currentTimeMillis() - startProcessing) / 1000) + "s Checked: " + totalCount + " Size:" + (totalSize >> 20) + "M Files: " + sourceFile + " -> " + destFile);
        }
        return destFile;
    }
}
