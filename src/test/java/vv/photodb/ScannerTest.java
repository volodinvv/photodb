package vv.photodb;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.SQLException;

public class ScannerTest {


    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void close() throws SQLException {
        Scanner scanner = new Scanner();

        scanner.dao = Mockito.mock(PhotosDAO.class);

    }

    @Test
    public void rescanMeta() {
    }

    @Test
    public void calculateFolder() {
    }

    @Test
    public void scan() {
    }

    @Test
    public void copy() {
    }
}