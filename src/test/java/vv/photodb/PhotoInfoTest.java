package vv.photodb;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class PhotoInfoTest {

    @Test
    public void testToString() {
        PhotoInfo info = new PhotoInfo();
        Assert.assertNotNull(info.toString());

        info = new PhotoInfo();
        info.createDate = new Date();
        Assert.assertNotNull(info.toString());
    }
}