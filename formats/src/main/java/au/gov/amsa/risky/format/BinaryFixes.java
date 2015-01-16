package au.gov.amsa.risky.format;

import java.io.File;
import java.nio.ByteBuffer;

import rx.Observable;

public final class BinaryFixes {

	public static final int BINARY_FIX_BYTES = 25;
	public static final short SOG_ABSENT = 1023;
	public static final short COG_ABSENT = 3600;
	public static final short HEADING_ABSENT = 360;

	public static Observable<Fix> from(File file) {
		return Observable.create(new BinaryFixesOnSubscribe(file));
	}

	public static void write(Fix fix, ByteBuffer bb) {
		bb.putFloat(fix.getLat());
		bb.putFloat(fix.getLon());
		bb.putLong(fix.getTime());
		if (fix.getNavigationalStatus().isPresent())
			bb.put((byte) fix.getNavigationalStatus().get().ordinal());
		else
			bb.put(Byte.MAX_VALUE);
		// rot
		bb.put((byte) 0);

		if (fix.getSpeedOverGroundKnots().isPresent())
			bb.putShort((short) Math.round(10 * fix.getSpeedOverGroundKnots()
					.get()));
		else
			bb.putShort(SOG_ABSENT);

		if (fix.getCourseOverGroundDegrees().isPresent())
			bb.putShort((short) Math.round(10 * fix
					.getCourseOverGroundDegrees().get()));
		else
			bb.putShort(COG_ABSENT);

		if (fix.getHeadingDegrees().isPresent())
			bb.putShort((short) Math.round(fix.getHeadingDegrees().get()));
		else
			bb.putShort(HEADING_ABSENT);
		if (fix.getAisClass() == AisClass.A)
			bb.put((byte) 0);
		else
			bb.put((byte) 1);
	}

}
