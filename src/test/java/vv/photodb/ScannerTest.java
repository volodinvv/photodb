package vv.photodb;

import org.junit.Test;

import java.nio.file.Path;

public class ScannerTest {

    @Test
    public void dateFromPath() {

        Scanner.dateFromPath(Path.of("ddd", "2090asdskkj", "2010-09-23"));

    }
}