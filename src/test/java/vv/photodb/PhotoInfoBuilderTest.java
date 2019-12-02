package vv.photodb;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.text.ParseException;

import static org.junit.Assert.*;

public class PhotoInfoBuilderTest {

    private Path file1;
    private Path file2;

    @Before
    public void setUp() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        file1 = Path.of(new File(classLoader.getResource("builder/IMG_1088.JPG").getFile()).getAbsolutePath());
        file2 = Path.of(new File(classLoader.getResource("builder/20191012_132153.jpg").getFile()).getAbsolutePath());

    }

    @Test
    public void readComments() {
    }

    @Test
    public void readMetadata() throws ParseException {
        PhotoInfo photoInfo = new PhotoInfoBuilder().readMetadata(file1, "default").build();
        assertEquals("Canon PowerShot A620", photoInfo.equipment);
        assertEquals(Utils.formatter.parse("2007-06-24 16:08:56"), photoInfo.createDate);
    }

    @Test
    public void build() {
    }

    @Test
    public void readFileInfo() {
    }

    @Test
    public void addMD5() {
    }
}