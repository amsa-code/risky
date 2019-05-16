package au.gov.amsa.geo.adhoc;

import java.io.File;

import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.risky.format.BinaryFixesFormat;
import au.gov.amsa.risky.format.BinaryFixesWriter;
import au.gov.amsa.risky.format.Fix;
import rx.Observable;

public class NmeaToBinaryFixesConvertorMain {

	public static void main(String[] args) {
		Observable<Fix> fixes = Streams.extractFixes(Streams.nmeaFromGzip(new File("/home/dxm/2019-05-15.txt.gz")));
		BinaryFixesWriter //
				.writeFixes(f -> "target/fixes.bin", fixes, 8192, false, BinaryFixesFormat.WITH_MMSI)
				.doOnError(e -> e.printStackTrace()) //
				.subscribe();
	}

}
