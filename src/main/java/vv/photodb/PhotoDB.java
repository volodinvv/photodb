package vv.photodb;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 *
 */
public class PhotoDB {

    public static class Args {
        @Parameter(names = "-cmd")
        public String cmd = null;

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

        try (Scanner scanner = new Scanner()) {
            switch (PhotoDB.args.cmd) {
                case "scan":
                    scanner.scan(PhotoDB.args.source);
                    break;
                case "rescanMeta":
                    scanner.rescanMeta();
                    break;
                case "copy":
                    scanner.copy(PhotoDB.args.dest);
                    break;
                case "updateDest":
                    scanner.updateDest();
                    break;
                case "deleteSource":
                    scanner.deleteSource(".".equals(PhotoDB.args.source) ? "" : PhotoDB.args.source);
                    break;
                case "deleteEmptyDirs":
                    scanner.deleteEmptyDirs(PhotoDB.args.source);
                    break;
                default:
                    System.out.println("Unknown command: " + PhotoDB.args.cmd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
