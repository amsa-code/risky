package au.gov.amsa.risky.format;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.joda.time.DateTime;

import rx.Observable;
import rx.functions.Func1;

public final class BinaryFixes {

	public static final int BINARY_FIX_BYTES = 31;
	public static final short SOG_ABSENT = 1023;
	public static final short COG_ABSENT = 3600;
	public static final short HEADING_ABSENT = 360;
	public static final byte NAV_STATUS_ABSENT = Byte.MAX_VALUE;
	public static final int LATENCY_ABSENT = -1;
	public static final short SOURCE_ABSENT = 0;
	public static final byte ROT_ABSENT = Byte.MIN_VALUE;
	public static final byte SOURCE_PRESENT_BUT_UNKNOWN = 1;
	protected static final char COMMA = ',';
	protected static final byte RATE_OF_TURN_ABSENT = -128;

	public static Observable<Fix> from(File file) {
		return from(file, false);
	}

	public static Observable<Fix> from(File file, boolean backpressure) {
		if (backpressure)
			return BinaryFixesOnSubscribe2.from(file);
		else
			return BinaryFixesOnSubscribe.from(file);
	}

	public static Observable<String> csv(Observable<Fix> fixes) {
		return fixes.map(new Func1<Fix, String>() {

			@Override
			public String call(Fix f) {
				StringBuilder s = new StringBuilder();
				s.append(f.getLat());
				s.append(COMMA);
				s.append(f.getLon());
				s.append(COMMA);
				s.append(new DateTime(f.getTime()).toString());
				s.append(COMMA);
				s.append(f.getSource().or(SOURCE_ABSENT));
				s.append(COMMA);
				s.append(f.getLatencySeconds().or(LATENCY_ABSENT));
				s.append(COMMA);
				s.append(f.getNavigationalStatus().or(
				        NavigationalStatus.values()[NAV_STATUS_ABSENT]));
				s.append(COMMA);
				s.append(f.getRateOfTurn().or(RATE_OF_TURN_ABSENT));
				s.append(COMMA);
				// TODO add the rest of the fields
				return s.toString();
			}
		});
	}

	public static void write(Fix fix, OutputStream os) {
		byte[] bytes = new byte[BINARY_FIX_BYTES];
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		write(fix, bb);
		try {
			os.write(bytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static ByteBuffer createFixByteBuffer() {
		return ByteBuffer.allocate(BINARY_FIX_BYTES);
	}

	public static void write(Fix fix, ByteBuffer bb) {
		bb.putFloat(fix.getLat());
		bb.putFloat(fix.getLon());
		bb.putLong(fix.getTime());
		if (fix.getLatencySeconds().isPresent())
			bb.putInt(fix.getLatencySeconds().get());
		else
			bb.putInt(LATENCY_ABSENT);
		if (fix.getSource().isPresent())
			bb.putShort(fix.getSource().get());
		else
			bb.putShort(SOURCE_ABSENT);

		if (fix.getNavigationalStatus().isPresent())
			bb.put((byte) fix.getNavigationalStatus().get().ordinal());
		else
			bb.put(NAV_STATUS_ABSENT);

		// rot
		bb.put(ROT_ABSENT);

		if (fix.getSpeedOverGroundKnots().isPresent())
			bb.putShort((short) Math.round(10 * fix.getSpeedOverGroundKnots().get()));
		else
			bb.putShort(SOG_ABSENT);

		if (fix.getCourseOverGroundDegrees().isPresent())
			bb.putShort((short) Math.round(10 * fix.getCourseOverGroundDegrees().get()));
		else
			bb.putShort(COG_ABSENT);

		if (fix.getHeadingDegrees().isPresent())
			bb.putShort((short) Math.round(10 * fix.getHeadingDegrees().get()));
		else
			bb.putShort(HEADING_ABSENT);
		if (fix.getAisClass() == AisClass.A)
			bb.put((byte) 0);
		else
			bb.put((byte) 1);
	}

}
