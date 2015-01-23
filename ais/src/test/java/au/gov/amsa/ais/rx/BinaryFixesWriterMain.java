package au.gov.amsa.ais.rx;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BinaryFixesWriterMain {

	private static final Logger log = LoggerFactory
			.getLogger(BinaryFixesWriterMain.class);

	public static void main(String[] args) throws IOException {
		log.info("starting");
		final String inputFilename;
		if (args.length > 0)
			inputFilename = args[0];
		else
			// inputFilename = "G:\\mariweb";
			inputFilename = "/media/analysis/test";

		File input = new File(inputFilename);
		File output = new File("target/binary");
		long t = System.currentTimeMillis();

		BinaryFixesWriter.writeFixes(input, output);

		log.info("finished in " + (System.currentTimeMillis() - t) / 1000.0
				+ "s");
	}

}
