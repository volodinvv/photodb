package vv.photodb;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

public class UtilsTest {

    @Test
    public void MD5() throws IOException, NoSuchAlgorithmException {
        ClassLoader classLoader = getClass().getClassLoader();
        Assert.assertEquals("j4MXzIOefch4WU17JTDJ4Q==", Utils.MD5(Path.of(new File(classLoader.getResource("md5test.txt").getFile()).getAbsolutePath())));
    }

    @Test
    public void getFileExtension() {
        Assert.assertEquals("jpg", Utils.getFileExtension("file.jpg"));
        Assert.assertNotEquals("JPG", Utils.getFileExtension("file.JPG"));
        Assert.assertEquals("jpg", Utils.getFileExtension("file.JPG"));
        Assert.assertEquals("jpg", Utils.getFileExtension("file.xxx.JPG"));
        Assert.assertEquals("jpg", Utils.getFileExtension("file.xxx.jpg"));

        Assert.assertNull(Utils.getFileExtension("file_xxx_JPG"));


    }
}