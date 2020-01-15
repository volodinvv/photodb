package vv.photodb;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

public class Scanner implements AutoCloseable {

    Logger logger = LoggerFactory.getLogger(Scanner.class);

    private static final Set<Object> SKIP_EXT = Set.of("index", "txt", "ini",
            "info", "db", "amr", "ctg", "ithmb", "nri", "scn", "thm", "xml", "json");

    public Long totalSize = 0L;
    public Long totalCount = 0L;
    public Long startProcessing = 0L;


    public PhotosDAO dao;

    public Scanner() {
        startProcessing = System.currentTimeMillis();
        dao = new PhotosDAO();
    }

    @Override
    public void close() throws Exception {
        if (dao != null) {
            dao.close();
        }
    }

    public void rescanMeta() throws Exception {
        dao.list(" where created is null", old -> {
            PhotoInfo photoInfo = new PhotoInfoBuilder(old)
                    .readMetadata(Path.of(old.path), old.equipment)
                    .build();
            dao.save(photoInfo);
            totalSize += photoInfo.size;
            logger.info("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Scanned: " + ++totalCount + " Size:" + (totalSize >> 20) + "M " + photoInfo);
        });
    }

    @Deprecated
    public void calculateFolder(String rootDir) throws SQLException {
        Path root = Path.of(rootDir);
        dao.list(" where path like '" + dao.escape(rootDir) + "%'", old -> {
            dao.save(new PhotoInfoBuilder(old).readFileInfo(Path.of(old.path), root).build());
            logger.info(old.path + " -> " + old.folder);
            logger.info("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Scanned: " + ++totalCount);
        });
    }

    public void scan(String source) throws Exception {
        processDir(dao, Path.of(source));
    }

    private void processDir(PhotosDAO dao, Path path) throws IOException, SQLException {
        String defaultEquipment = getDefaultEquipment(path);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    processDir(dao, entry);
                } else {
                    logger.info("{}", entry);
                    if (PhotoDB.args.resume && dao.isSaved(entry.toString())) {
                        totalCount++;
                        logger.info("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Skipped: " + totalCount + " Size:" + (totalSize >> 20) + "M " + entry);
                    } else {
                        if (allowableFile(entry)) {
                            PhotoInfo photoInfo = new PhotoInfoBuilder()
                                    .readFileInfo(entry, Path.of(PhotoDB.args.source))
                                    .readMetadata(entry, defaultEquipment)
                                    .readComments(entry)
                                    .addMD5(entry).build();
                            dao.save(photoInfo);
                            totalSize += photoInfo.size;
                            totalCount++;

                            logger.info("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Scanned: " + totalCount + " Size:" + (totalSize >> 20) + "M " + photoInfo);
                        }
                    }
                }
            }
        }
    }

    private boolean allowableFile(Path entry) {
        String name = entry.getFileName().toString();
        String ext = Utils.getFileExtension(name);
        return ext != null && !SKIP_EXT.contains(ext) && !name.startsWith(".");
    }

    private String getDefaultEquipment(Path path) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (allowableFile(entry)) {
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

    public void copy(String dest) throws Exception {
        dao.listForCopy(item -> {
            try {
                Path destFile = copyFile(item, dest);
                dao.updateDestination(item, destFile.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Path copyFile(PhotoInfo item, String dest) throws Exception {
        Path destRootDir = Path.of(dest);
        Path destSubDir;
        Path destDir;
        if (item.createDate != null) {
            LocalDate createdDate = item.createDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            String year = Integer.toString(createdDate.getYear());
            String month = StringUtils.leftPad(Integer.toString(createdDate.getMonthValue()), 2, '0');
            String day = StringUtils.leftPad(Integer.toString(createdDate.getDayOfMonth()), 2, '0');

            String commentVal = item.comment != null ? "_" + item.comment : "";
            String equipmentVal = item.equipment != null && !item.equipment.equals("unknown") ? "_" + item.equipment : "";

            destSubDir = Path.of(dest, year, month + "_" + day + equipmentVal + commentVal);
        } else {
            destSubDir = Path.of(dest, "Unsorted", item.folder);
        }

        destDir = destRootDir.resolve(destSubDir);

        if (Files.notExists(destDir)) {
            Files.createDirectories(destDir);
        }

        Path sourceFile = Path.of(item.path);
        Path destFile = destDir.resolve(item.name);

        totalSize += sourceFile.toFile().length();
        totalCount++;

        if (Files.notExists(sourceFile)) {
            logger.info("File not exist: " + sourceFile);
            return null;
        }

        if (Files.notExists(destFile)) {
            Files.copy(sourceFile, destFile);
            if (item.createDate != null) {
                Files.setLastModifiedTime(destDir, FileTime.fromMillis(item.createDate.getTime()));
            }

            logger.info(((System.currentTimeMillis() - startProcessing) / 1000) + "s Copied: " + totalCount + " Size:" + (totalSize >> 20) + "M Files: " + sourceFile + " -> " + destFile);
        } else {
            String destMD5 = Utils.MD5(destFile);
            if (!destMD5.equals(item.md5)) {

                Path destMD5ErrorDir = destRootDir.resolve("MD5Error").resolve(destSubDir);
                if (Files.notExists(destMD5ErrorDir)) {
                    Files.createDirectories(destMD5ErrorDir);
                }

                destFile = destMD5ErrorDir.resolve(item.name);
                Files.copy(sourceFile, destFile);
                if (item.createDate != null) {
                    Files.setLastModifiedTime(destDir, FileTime.fromMillis(item.createDate.getTime()));
                }
                logger.info("MD5 not equals: " + sourceFile + " -> " + destFile + "(" + destMD5ErrorDir + ")");

                logger.info(((System.currentTimeMillis() - startProcessing) / 1000) + "s Checked: " + totalCount + " Size:" + (totalSize >> 20) + "M Files: " + sourceFile + " -> " + destFile);
            }
        }
        return destFile;
    }

    @Deprecated
    public void updateDest() throws SQLException {
        dao.list(" where destination is not null", old -> {
            try {
                dao.updateDestination(old, old.destination);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            logger.info(old.path + " -> " + old.folder);
            logger.info("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Scanned: " + ++totalCount);
        });
    }

    public void deleteSource() throws SQLException {
        dao.list(" where destination is not null and deleted <> true", item -> {
            if (Files.exists(Path.of(item.path))) {
                if (Files.exists(Path.of(item.destination))) {
                    if (Utils.equalsFiles(item.path, item.destination)) {
                        logger.info("Time: " + ((System.currentTimeMillis() - startProcessing) / 1000) + "s Delete: " + item.path);
                        try {
                            Files.delete(Path.of(item.path));
                            dao.markDeleted(item.path);
                        } catch (IOException e) {
                            logger.error("Can't delete file: " + item.path, e);
                        }
                    }
                } else {
                    logger.info("Destination not found: {}", item);
                    throw new RuntimeException("Destination not found: " + item.destination);
                }
            } else {
                logger.info("Mark deleted not existed file: {}", item.path);
                dao.markDeleted(item.path);
            }
        });
    }


}
