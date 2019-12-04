package vv.photodb;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class ScannerTest {


    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void close() throws Exception {
        Scanner scanner = new Scanner();
        scanner.dao = Mockito.mock(PhotosDAO.class);
        scanner.close();
        Mockito.verify(scanner.dao, atLeastOnce()).close();
    }

    @Test
    public void rescanMeta() throws Exception {
//        Scanner scanner = new Scanner();
//        scanner.dao = Mockito.mock(PhotosDAO.class);
//        scanner.rescanMeta();
//
//        verify(scanner.dao, atLeastOnce()).list(anyString(), any(new Consumer<PhotoInfo>() {
//            @Override
//            public void accept(PhotoInfo photoInfo) {
//
//            }
//        }));
//
//        verify(scanner.dao, atLeastOnce()).save(argThat(new ArgumentMatcher<PhotoInfo>() {
//            @Override
//            public boolean matches(Object o) {
//                return ((PhotoInfo) o).createDate == null;
//            }
//        }));

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