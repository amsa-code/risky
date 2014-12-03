package au.gov.amsa.ais;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class BenchmarksAis {

	private static String shipStaticA = "\\s:rEV02,c:1334337326*5A\\!ABVDM,1,1,0,2,57PBtv01sb5IH`PR221LE986222222222222220l28?554000:kQEhhDm31H20DPSmD`880,2*40";
	private static String aisPositionA = "\\s:rEV02,c:1334337326*5A\\!AIVDM,1,1,,B,18JSad001i5gcaArTICimQTT068t,0*4A";

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
