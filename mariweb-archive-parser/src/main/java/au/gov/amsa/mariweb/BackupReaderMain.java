package au.gov.amsa.mariweb;

import java.io.File;
import java.io.IOException;

import rx.schedulers.Schedulers;

public class BackupReaderMain {

    public static void main(String[] args) throws IOException {
        final String directory;
        if (args.length > 0)
            directory = args[0];
        else
            directory = "/media/an/mariweb-2015";

        // BackupReader.convertFileToNmea(new File(
        // "/media/analysis/ITU_20140101.bu.gz"));
        // System.exit(0);

        BackupReader.convertDirectoryToNmea(new File(directory), Schedulers.computation(), true);
    }
}
