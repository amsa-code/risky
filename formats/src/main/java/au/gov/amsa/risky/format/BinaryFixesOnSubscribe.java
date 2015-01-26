package au.gov.amsa.risky.format;

import static au.gov.amsa.risky.format.BinaryFixes.BINARY_FIX_BYTES;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;

import com.google.common.base.Optional;

public class BinaryFixesOnSubscribe implements OnSubscribe<Fix> {

	private File file;

	public BinaryFixesOnSubscribe(File file) {
		this.file = file;
	}

	@Override
	public void call(Subscriber<? super Fix> subscriber) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			subscriber.add(createSubscription(fis));
			reportFixes(file, subscriber, fis);
			if (!subscriber.isUnsubscribed())
				subscriber.onCompleted();
		} catch (Exception e) {
			if (!subscriber.isUnsubscribed())
				subscriber.onError(e);
		}
	}

	private Subscription createSubscription(final FileInputStream fis) {
		return new Subscription() {

			volatile boolean subscribed = true;

			@Override
			public void unsubscribe() {
				subscribed = false;
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public boolean isUnsubscribed() {
				return !subscribed;
			}
		};
	}

	private static void reportFixes(final File file,
			Subscriber<? super Fix> subscriber, InputStream fis)
			throws IOException {
		byte[] bytes = new byte[4096 * BINARY_FIX_BYTES];
		int length = 0;
		if (subscriber.isUnsubscribed())
			return;
		long mmsi = getMmsi(file);
		while ((length = fis.read(bytes)) > 0) {
			for (int i = 0; i < length; i += BINARY_FIX_BYTES) {
				if (subscriber.isUnsubscribed())
					return;
				ByteBuffer bb = ByteBuffer.wrap(bytes, i, BINARY_FIX_BYTES);
				Fix fix = toFix(mmsi, bb);
				subscriber.onNext(fix);
			}
		}
	}

	private static Fix toFix(long mmsi, ByteBuffer bb) {
		float lat = bb.getFloat();
		float lon = bb.getFloat();
		long time = bb.getLong();
		int latency = bb.getInt();
		final Optional<Integer> latencySeconds;
		if (latency == -1)
			latencySeconds = absent();
		else
			latencySeconds = of(latency);
		short src = bb.getShort();
		final Optional<Short> source;
		if (src == 0)
			source = absent();
		else
			source = of(src);
		byte nav = bb.get();
		final Optional<NavigationalStatus> navigationalStatus;
		if (nav == Byte.MAX_VALUE)
			navigationalStatus = absent();
		else
			navigationalStatus = of(NavigationalStatus.values()[nav]);

		// rate of turn
		bb.get();

		short sog = bb.getShort();
		final Optional<Float> speedOverGroundKnots;
		if (sog == 1023)
			speedOverGroundKnots = absent();
		else
			speedOverGroundKnots = of(sog / 10f);

		short cog = bb.getShort();
		final Optional<Float> courseOverGroundDegrees;
		if (cog == 3600)
			courseOverGroundDegrees = absent();
		else
			courseOverGroundDegrees = of(cog / 10f);

		short heading = bb.getShort();
		final Optional<Float> headingDegrees;
		if (heading == 360)
			headingDegrees = absent();
		else
			headingDegrees = of((float) heading);
		byte cls = bb.get();
		final AisClass aisClass;
		if (cls == 0)
			aisClass = AisClass.A;
		else
			aisClass = AisClass.B;

		Fix fix = new Fix(mmsi, lat, lon, time, latencySeconds, source,
				navigationalStatus, speedOverGroundKnots,
				courseOverGroundDegrees, headingDegrees, aisClass);
		return fix;
	}

	public static long getMmsi(File file) {
		int finish = file.getName().indexOf('.');
		String id = file.getName().substring(0, finish);
		return Long.parseLong(id);
	}

}
