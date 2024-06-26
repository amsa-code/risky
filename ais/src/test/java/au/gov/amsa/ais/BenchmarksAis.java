package au.gov.amsa.ais;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import rx.Observable;
import au.gov.amsa.ais.message.AisShipStaticA;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.util.nmea.NmeaMessage;

@State(Scope.Benchmark)
public class BenchmarksAis {

	private static final String shipStaticA = "\\s:rEV02,c:1334337326*5A\\!ABVDM,1,1,0,2,57PBtv01sb5IH`PR221LE986222222222222220l28?554000:kQEhhDm31H20DPSmD`880,2*40";
	private static final String aisPositionA = "\\s:rEV02,c:1334337326*5A\\!AIVDM,1,1,,B,18JSad001i5gcaArTICimQTT068t,0*4A";
	private static final String aisPositionB = "\\s:MSQ - Mt Cootha,c:1426803365*73\\!AIVDM,1,1,,A,B7P?n900Irg8IHL4RblF?wRToP06,0*1B";

//	@Benchmark
	public void parseShipStaticNmeaMessage() {
		AisNmeaMessage n = new AisNmeaMessage(shipStaticA);
		n.getMessage();
	}

	@Benchmark
	public void parseShipStaticNmeaMessageAndExtractBitsOfInterest() {
		AisNmeaMessage n = new AisNmeaMessage(shipStaticA);
		AisShipStaticA m = (AisShipStaticA) n.getMessage();
		m.getName();
		m.getShipType();
		m.getImo();
		m.getLengthMetres();
		m.getWidthMetres();
		m.getCallsign();
		m.getMmsi();
	}

	@Benchmark
	public void parseAisPositionANmeaMessage() {
		AisNmeaMessage n = new AisNmeaMessage(aisPositionA);
		n.getMessage();
	}

	@Benchmark
	public void parseAisPositionBNmeaMessage() {
		AisNmeaMessage n = new AisNmeaMessage(aisPositionB);
		n.getMessage();
	}
}
