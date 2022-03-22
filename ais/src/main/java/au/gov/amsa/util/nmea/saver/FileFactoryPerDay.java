package au.gov.amsa.util.nmea.saver;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FileFactoryPerDay implements FileFactory {

    private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC"));

    private File directory;

    public FileFactoryPerDay(File directory) {
        this.directory = directory;
        directory.mkdirs();
    }

    @Override
    public File file(String line, long arrivalTime) {
        return new File(directory, date(arrivalTime) + ".txt");
    }

    @Override
    public String key(String line, long arrivalTime) {
        return date(arrivalTime);
    }

    private String date(long arrivalTime) {
        return dtf.format(Instant.ofEpochMilli(arrivalTime));
    }
}
