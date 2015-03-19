package au.gov.amsa.ais;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import rx.Observable;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.util.nmea.NmeaMessage;

@State(Scope.Benchmark)
public class BenchmarksAis {

	private static final String shipStaticA = "\\s:rEV02,c:1334337326*5A\\!ABVDM,1,1,0,2,57PBtv01sb5IH`PR221LE986222222222222220l28?554000:kQEhhDm31H20DPSmD`880,2*40";
	private static final String aisPositionA = "\\s:rEV02,c:1334337326*5A\\!AIVDM,1,1,,B,18JSad001i5gcaArTICimQTT068t,0*4A";
	private static final List<String> nmeaLines = Streams
	        .nmeaFromGzip(new File("src/test/resources/ais.txt.gz")).toList().toBlocking().single();

	@Benchmark
	public void parseShipStaticNmeaMessage() {
		AisNmeaMessage n = new AisNmeaMessage(shipStaticA);
		n.getMessage();
	}

	@Benchmark
	public void parseAisPositionANmeaMessage() {
		AisNmeaMessage n = new AisNmeaMessage(aisPositionA);
		n.getMessage();
	}

	// @Benchmark
	// public void parseAisPositionANmeaMessageUsingDmaLibrary() throws
	// SentenceException,
	// AisMessageException, SixbitException {
	// Vdm vdm = new Vdm();
	// vdm.parse(aisPositionA);
	// AisMessage3.getInstance(vdm);
	// }

	@Benchmark
	public void parseMany() throws IOException {
		// process 44K lines
		Observable.from(nmeaLines).map(Streams.LINE_TO_NMEA_MESSAGE)
		        .compose(Streams.<NmeaMessage> valueIfPresent()).subscribe();
	}

	public static void main(String[] args) {
		System.setProperty("a", "");
		while (true) {
			AisNmeaMessage n = new AisNmeaMessage(aisPositionA);
			n.getMessage();
			n = new AisNmeaMessage(shipStaticA);
			n.getMessage();
		}
	}

}
