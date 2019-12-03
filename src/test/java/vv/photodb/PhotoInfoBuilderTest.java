package vv.photodb;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class PhotoInfoBuilderTest {

    private Path root;

    private Path file1;
    private Path file2;
    private Path mov;

    @Before
    public void setUp() throws Exception {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        ClassLoader classLoader = getClass().getClassLoader();

        file1 = Path.of(new File(classLoader.getResource("builder/IMG_1088.JPG").getFile()).getAbsolutePath());
        file2 = Path.of(new File(classLoader.getResource("builder/20191012_132153.jpg").getFile()).getAbsolutePath());
        //mov = Path.of(new File(classLoader.getResource("builder/MVI_1446.MOV").getFile()).getAbsolutePath());

        root = file1.getParent().getParent();
    }


    @Test
    public void readComments() {
        assertEquals("комент", new PhotoInfoBuilder().readComments(Path.of("root", "root2", "комент", "image.jpg")).build().comment);
        assertEquals("комент", new PhotoInfoBuilder().readComments(Path.of("root", "root2", "12 комент", "image.jpg")).build().comment);
        assertEquals("комент", new PhotoInfoBuilder().readComments(Path.of("root", "root2", "12 комент", "parent", "image.jpg")).build().comment);
        assertEquals("комент", new PhotoInfoBuilder().readComments(Path.of("root", "root2", "text комент", "parent", "image.jpg")).build().comment);
        assertNotEquals("комент", new PhotoInfoBuilder().readComments(Path.of("root", "root2", "12 комент", "parent", "parent", "image.jpg")).build().comment);
    }

    @Test
    public void readMetadata() throws ParseException {
        PhotoInfo photoInfo = new PhotoInfoBuilder().readMetadata(file1, "default").build();
        assertEquals("Canon PowerShot A620", photoInfo.equipment);
        assertEquals(Utils.formatter.parse("2007-06-24 16:08:56"), photoInfo.createDate);

        assertNull(photoInfo.comment);
        assertNull(photoInfo.path);
        assertNull(photoInfo.folder);
        assertNull(photoInfo.md5);
        assertNull(photoInfo.destination);
        assertNull(photoInfo.size);
        assertNull(photoInfo.ext);

        photoInfo = new PhotoInfoBuilder().readMetadata(file2, "default").build();
        assertEquals("SM-A520F", photoInfo.equipment);
        assertEquals(Utils.formatter.parse("2019-10-12 13:21:53"), photoInfo.createDate);

        assertNull(photoInfo.comment);
        assertNull(photoInfo.path);
        assertNull(photoInfo.folder);
        assertNull(photoInfo.md5);
        assertNull(photoInfo.destination);
        assertNull(photoInfo.size);
        assertNull(photoInfo.ext);

       // photoInfo = new PhotoInfoBuilder().readMetadata(mov, "default").build();
       // assertEquals("Canon PowerShot SX230 HS", photoInfo.equipment);
       // assertEquals("2012-09-12 09:58:06", Utils.formatter.format(photoInfo.createDate));

    }

    @Test
    public void build() {
    }

    @Test
    public void readFileInfo() {
        PhotoInfo photoInfo = new PhotoInfoBuilder().readFileInfo(file1, root).build();
        assertEquals(file1.getFileName().toString(), photoInfo.name);
        assertEquals("jpg", photoInfo.ext);
        assertEquals(file1.toString(), photoInfo.path);
        assertEquals("builder", photoInfo.folder);
        assertEquals((long) 2667387L, (long) photoInfo.size);
    }

    @Test
    public void addMD5() {
        PhotoInfo photoInfo = new PhotoInfoBuilder().addMD5(file1).build();
        assertEquals("c3iAZY2O0Gw5BZbjq54DUA==", photoInfo.md5);
    }
}