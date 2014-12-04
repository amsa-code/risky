package au.gov.amsa.util.nmea.saver;

import java.io.File;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class FileFactoryPerDay implements FileFactory {
	
	private static DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd")
			.withZoneUTC();
	
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
		return dtf.print(arrivalTime);
	}
}
