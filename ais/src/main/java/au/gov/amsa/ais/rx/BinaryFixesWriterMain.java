package au.gov.amsa.ais.rx;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import rx.Observable;
import rx.functions.Func1;
import au.gov.amsa.ais.rx.BinaryFixesWriter.ByMonth;
import au.gov.amsa.risky.format.Fix;

public class BinaryFixesWriterMain {

	public static void main(String[] args) throws IOException {
		System.out.println("starting");
		long t = System.currentTimeMillis();
		final String inputFilename;
		if (args.length > 0)
			inputFilename = args[0];
		else
			// inputFilename = "G:\\mariweb";
			inputFilename = "/media/analysis/test";

		File input = new File(inputFilename);
		File output = new File("target/binary");
		Observable<File> files = Observable.from(find(input,
				Pattern.compile("NMEA_ITU_20.*.gz")));
		// System.out.println(files.count().toBlocking().single());

		FileUtils.deleteDirectory(output);
		Observable<Fix> fixes = files
				.flatMap(new Func1<File, Observable<Fix>>() {
					@Override
					public Observable<Fix> call(File file) {
						return Streams.extractFixes(Streams.nmeaFromGzip(file
								.getAbsolutePath()));
					}
				});
		ByMonth fileMapper = new BinaryFixesWriter.ByMonth(output);
		BinaryFixesWriter.writeFixes(fileMapper, fixes, 100).subscribe();
		System.out.println("finished in " + (System.currentTimeMillis() - t)
				/ 1000.0 + "s");
	}

	private static List<File> find(File file, final Pattern pattern) {
		if (!file.exists())
			return Collections.emptyList();
		else {
			if (!file.isDirectory()
					&& pattern.matcher(file.getName()).matches())
				return Collections.singletonList(file);
			else if (file.isDirectory()) {
				List<File> list = new ArrayList<File>();
				File[] files = file.listFiles();
				if (files != null)
					for (File f : file.listFiles()) {
						if (!f.getName().startsWith(".")) {
							if (f.isFile()
									&& pattern.matcher(f.getName()).matches())
								list.add(f);
							else
								list.addAll(find(f, pattern));
						}
					}
				return list;
			} else
				return Collections.emptyList();
		}
	}
}
