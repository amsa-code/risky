package au.gov.amsa.risky.format;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class DownsampleMain {

	public static void main(String[] args) {
		try {
			File input = new File(System.getProperty("input"));
			File output = new File(System.getProperty("output"));
			Pattern pattern = Pattern.compile(System.getProperty("pattern"));
			long intervalMs = Long.parseLong(System.getProperty("ms"));
			Downsample.downsample(input, output, pattern, intervalMs,
					TimeUnit.MILLISECONDS);
		} catch (RuntimeException e) {
			System.out
					.println("Usage: -Dinput=<input directory> -Doutput=<output directory> -Dpattern=<filename pattern> -Dms=<downsample interval ms>");
		}
	}
}
