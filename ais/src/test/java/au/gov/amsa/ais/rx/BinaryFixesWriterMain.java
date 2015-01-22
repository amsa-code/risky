package au.gov.amsa.ais.rx;

import java.io.File;
import java.io.IOException;

public class BinaryFixesWriterMain {

	public static void main(String[] args) throws IOException {
		System.out.println("starting");
		final String inputFilename;
		if (args.length > 0)
			inputFilename = args[0];
		else
			// inputFilename = "G:\\mariweb";
			inputFilename = "/media/analysis/test";

		File input = new File(inputFilename);
		File output = new File("target/binary");
		BinaryFixesWriter.writeFixes(input, output);
	}

}
