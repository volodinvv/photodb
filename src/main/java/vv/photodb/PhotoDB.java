package vv.photodb;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
public class PhotoDB {

    public static class Args {
        @Parameter(names = "-cmd")
        public String cmd = "scan";

        @Parameter(names = "-dest")
        public String dest = ".";

        @Parameter(names = "-source")
        public String source = ".";

        @Parameter(names = "-resume")
        public boolean resume;

        @Parameter(names = "-table")
        public String table = "photos";
    }

    public static final Args args = new Args();

    public static void main(String[] args) {

        new JCommander(PhotoDB.args).parse(args);

        switch (PhotoDB.args.cmd) {
            case "scan":
                Scanner.scan(PhotoDB.args.source);
                break;
            case "rescanMeta":
                Scanner.rescanMeta();
                break;
            case "copy":
                Scanner.copy(PhotoDB.args.dest);
                break;
            case "calculateFolder":
                Scanner.calculateFolder();
                break;
            default:
                System.out.println("Unknown command: " + PhotoDB.args.cmd);
        }
    }

}
