package vv.photodb;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.avi.AviDirectory;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.drew.metadata.mov.QuickTimeDirectory;
import com.drew.metadata.mov.metadata.QuickTimeMetadataDirectory;
import com.drew.metadata.mp4.Mp4Directory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class PhotoInfoBuilder {
    private PhotoInfo photoInfo = new PhotoInfo();

    public PhotoInfoBuilder readComments(Path entry) {
        photoInfo.comment = readComment(entry.getParent().getFileName().toString());
        if (photoInfo.comment == null) {
            photoInfo.comment = readComment(entry.getParent().getParent().getFileName().toString());
        }
        return this;
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

    public PhotoInfoBuilder readMetadata(Path entry, String defaultEquipment) {
        photoInfo.equipment = defaultEquipment;
        try {

            com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(entry.toFile());
            if (entry.toFile().getName().endsWith("JPG")) {
                metadata.getDirectories().forEach(d -> {
                    System.out.println(d.getName() + " " + d.getClass());
                    d.getTags().forEach(t -> System.out.println("\t" + t.getTagTypeHex() + " : " + t.toString()));
                });
            }

            if (metadata.containsDirectoryOfType(Mp4Directory.class)) {
                photoInfo.createDate = metadata.getFirstDirectoryOfType(Mp4Directory.class).getDate(Mp4Directory.TAG_CREATION_TIME);
            }
            if (metadata.containsDirectoryOfType(AviDirectory.class)) {
                String aviDate = metadata.getFirstDirectoryOfType(AviDirectory.class).getString(AviDirectory.TAG_DATETIME_ORIGINAL);
                photoInfo.createDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy").parse(aviDate);
            }
            if (metadata.containsDirectoryOfType(ExifSubIFDDirectory.class)) {
                photoInfo.createDate = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class).getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            }
            if (metadata.containsDirectoryOfType(ExifIFD0Directory.class)) {
                photoInfo.equipment = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class).getString(0x0110);
            }

            if (metadata.containsDirectoryOfType(QuickTimeDirectory.class)) {
                Date origDate = metadata.getFirstDirectoryOfType(QuickTimeDirectory.class).getDate(QuickTimeDirectory.TAG_CREATION_TIME);
                photoInfo.createDate = new Date(origDate.getTime() + TimeZone.getTimeZone("Europe/Minsk").getOffset(origDate.getTime()));
            }
            if (metadata.containsDirectoryOfType(QuickTimeMetadataDirectory.class)) {
                photoInfo.equipment = metadata.getFirstDirectoryOfType(QuickTimeMetadataDirectory.class).getString(QuickTimeMetadataDirectory.TAG_MODEL);
            }

            if(photoInfo.createDate == null){
                photoInfo.createDate=  metadata.getFirstDirectoryOfType(FileSystemDirectory.class).getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE);
            }

            if (photoInfo.equipment == null) {
                photoInfo.equipment = "unknown";
            }

            photoInfo.equipment = photoInfo.equipment.trim();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Can't read metadata: " + entry);
        }
        return this;
    }

    public PhotoInfo build() {
        return photoInfo;
    }

    public PhotoInfoBuilder readFileInfo(Path entry) {
        File file = entry.toFile();
        photoInfo.path = file.getAbsolutePath();
        photoInfo.name = file.getName();
        photoInfo.ext = Utils.getFileExtension(file.getName());
        photoInfo.size = file.length();
        return this;
    }

    public PhotoInfoBuilder addMD5(Path entry) throws IOException, NoSuchAlgorithmException {
        photoInfo.md5 = Utils.MD5(entry);
        return this;
    }
}
