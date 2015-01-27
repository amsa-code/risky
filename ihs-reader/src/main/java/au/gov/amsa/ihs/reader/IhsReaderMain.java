package au.gov.amsa.ihs.reader;

import java.io.File;

import org.apache.log4j.Logger;

public class IhsReaderMain {

	private static final Logger log = Logger.getLogger(IhsReaderMain.class);

	public static void main(String[] args) {
		File file = new File("/media/analysis/ship-data/ihs/608750.zip");
		log.info(IhsReader.fromZip(file).count().toBlocking().single()
				+ " ships");
	}
}
