package au.gov.amsa.risky.format;

import java.io.File;

import rx.schedulers.Schedulers;

public class SorterMain {

	public static void main(String[] args) throws InterruptedException {
		String output = System.getProperty("output", "target/output");
		// output = "/media/an/binary-fixes-2012/temp";
		long sampleSeconds = Long.parseLong(System.getProperty("sampleSeconds", "0"));
		BinaryFixes
		        .sortBinaryFixFilesByTime(new File(output), sampleSeconds, Schedulers.immediate())
		        .count().toBlocking().single();
	}

}
