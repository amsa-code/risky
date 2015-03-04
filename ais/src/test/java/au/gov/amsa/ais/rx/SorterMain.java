package au.gov.amsa.ais.rx;

import java.io.File;

import rx.schedulers.Schedulers;

public class SorterMain {

	public static void main(String[] args) {
		String output = System.getProperty("output", "target/output");
		output = "/media/an/binary-fixes-all";
		long sampleSeconds = Long.parseLong(System.getProperty("sampleSeconds", "0"));
		Streams.sortOutputFilesByTime(new File(output), sampleSeconds, Schedulers.computation())
		        .subscribe();
	}

}
