package vv.photodb;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class Scanner {

    private static final Set<Object> SKIP_EXT = Set.of("index", "txt", "ini",
            "info", "db", "amr", "ctg", "ithmb", "nri", "scn", "thm", "xml");

    public Long totalSize = 0L;
    public Long totalCount = 0L;
    public Long startProcessing = 0L;

    public Scanner() {
        startProcessing = System.currentTimeMillis();
    }

    public void rescanMeta() {
        try (PhotosDAO dao = new PhotosDAO()) {
            dao.list(" where created is null", resultSet -> {
                String path = dao.getString(resultSet, "path");
                String defaultEquipment = dao.getString(resultSet, "equipment");
                Path entry = Path.of(path);
                PhotoInfo photoInfo = new PhotoInfoBuilder()
                        .readFileInfo(entry, null)
                        .readMetadata(entry, defaultEquipment)
                        .readComments(entry)
                        .addMD5(entry).build();
                dao.save(photoInfo);
                totalSize += photoInfo.size;
                System.out.println("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Scanned: " + ++totalCount + " Size:" + (totalSize >> 20) + "M " + photoInfo);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void calculateFolder() {
        Path root = Path.of(PhotoDB.args.source);
        try (PhotosDAO dao = new PhotosDAO()) {
            dao.list(" where path like '" + dao.escape(PhotoDB.args.source) + "%'", resultSet -> {
                String path = dao.getString(resultSet, "path");
                String folder = new PhotoInfoBuilder().readFileInfo(Path.of(path), root).build().folder;
                System.out.println(path + " -> " + folder);

                dao.updateFolder(path, folder);
                System.out.println("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Scanned: " + ++totalCount);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void scan(String source) {
        try (PhotosDAO dao = new PhotosDAO()) {
            processDir(dao, Path.of(source));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processDir(PhotosDAO dao, Path path) throws IOException, SQLException {
        String defaultEquipment = getDefaultEquipment(path);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    processDir(dao, entry);
                } else {
                    System.out.println(entry);
                    if (PhotoDB.args.resume && dao.isSaved(entry.toString())) {
                        totalCount++;
                        System.out.println("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Skipped: " + totalCount + " Size:" + (totalSize >> 20) + "M " + entry);
                    } else {
                        if (!skipFile(entry)) {
                            PhotoInfo photoInfo = new PhotoInfoBuilder()
                                    .readFileInfo(entry, Path.of(PhotoDB.args.source))
                                    .readMetadata(entry, defaultEquipment)
                                    .readComments(entry)
                                    .addMD5(entry).build();
                            dao.save(photoInfo);
                            totalSize += photoInfo.size;
                            totalCount++;

                            System.out.println("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Scanned: " + totalCount + " Size:" + (totalSize >> 20) + "M " + photoInfo);
                        }
                    }
                }
            }
        }
    }

    private boolean skipFile(Path entry) {
        String name = entry.getFileName().toString();
        String ext = Utils.getFileExtension(name);
        return ext == null || SKIP_EXT.contains(ext) || name.startsWith(".");
    }

    private String getDefaultEquipment(Path path) throws IOException {
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

    public void copy(String dest) {
        try (PhotosDAO dao = new PhotosDAO()) {
            String sql = "select path, name, created, COALESCE (alias,equipment) equipment, md5, comment, folder " +
                    "from photos_for_copy p left join equipments e using(equipment) " +
                    "where destination is null";
            ResultSet resultSet = dao.getConnection().createStatement().executeQuery(sql);
            while (resultSet.next()) {

                String path = resultSet.getString("path");
                String name = resultSet.getString("name");
                String folder = resultSet.getString("folder");
                String created = resultSet.getString("created");
                String equipment = resultSet.getString("equipment");
                String comment = resultSet.getString("comment");
                String sourceMD5 = resultSet.getString("md5");


                Path destFile = copyFile(path, dest, name, folder, created, equipment, comment, sourceMD5);

                dao.getConnection().createStatement().executeUpdate("update photos set destination='" + destFile.toString().replace("\'", "\'\'") + "' where path='" + path.replace("\'", "\'\'") + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Path copyFile(String path, String dest, String name, String folder, String created, String equipment, String comment, String sourceMD5) throws Exception {

        Path destDir;
        if (created != null) {
            String commentVal = comment != null ? "_" + comment : "";
            String equipmentVal = equipment != null && !equipment.equals("unknown") ? "_" + equipment : "";
            String dirName = created.substring(5, 7) + "_" + created.substring(8, 10) + equipmentVal + commentVal;
            destDir = Path.of(dest, created.substring(0, 4), dirName);
        } else {
            destDir = Path.of(dest, "Unsorted", folder);
        }

        if (Files.notExists(destDir)) {
            Files.createDirectories(destDir);
        }

        Path sourceFile = Path.of(path);
        Path destFile = destDir.resolve(name);

        totalSize += sourceFile.toFile().length();
        totalCount++;

        if (Files.notExists(destFile)) {
            Files.copy(sourceFile, destFile);
            if (created != null) {
                Files.setLastModifiedTime(destDir, FileTime.fromMillis(Utils.formatter.parse(created).getTime()));
            }

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
