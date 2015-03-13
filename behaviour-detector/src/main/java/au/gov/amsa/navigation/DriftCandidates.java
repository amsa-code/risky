package au.gov.amsa.navigation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.NavigationalStatus;
import au.gov.amsa.streams.Strings;

import com.google.common.base.Optional;

public final class DriftCandidates {

	public static Observable<DriftCandidate> fromCsv(Reader reader) {
		return Strings.lines(reader)
		// remove blank lines
		        .filter(nonBlankLinesOnly())
		        // parse candidate
		        .map(new Func1<String, DriftCandidate>() {

			        @Override
			        public DriftCandidate call(String line) {
				        String[] items = line.split(",");
				        int i = 0;
				        long mmsi = Long.parseLong(items[i++]);
				        float lat = Float.parseFloat(items[i++]);
				        float lon = Float.parseFloat(items[i++]);
				        long time = Long.parseLong(items[i++]);
				        String cls = items[i++];
				        float course = Float.parseFloat(items[i++]);
				        float heading = Float.parseFloat(items[i++]);
				        float speedKnots = Float.parseFloat(items[i++]);
				        String status = items[i++];
				        long driftingSince = Long.parseLong(items[i++]);
				        final Optional<NavigationalStatus> navigationalStatus;
				        if (status.trim().length() == 0)
					        navigationalStatus = Optional.absent();
				        else
					        navigationalStatus = Optional.of(NavigationalStatus.valueOf(status));
				        final AisClass aisClass;
				        if (cls.trim().length() == 0)
					        throw new RuntimeException("cls should not be empty");
				        else if (AisClass.A.name().equals(cls))
					        aisClass = AisClass.A;
				        else
					        aisClass = AisClass.B;
				        Fix fix = new Fix(mmsi, lat, lon, time, Optional.<Integer> absent(),
				                Optional.<Short> absent(), navigationalStatus, Optional
				                        .of(speedKnots), Optional.of(course), Optional.of(heading),
				                aisClass);
				        return new DriftCandidate(fix, driftingSince);
			        }

		        });
	}

	public static Observable<DriftCandidate> fromCsv(final File file) {
		Action1<Reader> disposeAction = new Action1<Reader>() {

			@Override
			public void call(Reader reader) {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
			}
		};
		Func0<Reader> resourceFactory = new Func0<Reader>() {

			@Override
			public Reader call() {
				try {
					return new InputStreamReader(new FileInputStream(file));
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
		};
		Func1<Reader, Observable<DriftCandidate>> obFactory = new Func1<Reader, Observable<DriftCandidate>>() {

			@Override
			public Observable<DriftCandidate> call(Reader reader) {
				return fromCsv(reader);
			}
		};
		return Observable.using(resourceFactory, obFactory, disposeAction, true);
	}

	private static Func1<String, Boolean> nonBlankLinesOnly() {
		return new Func1<String, Boolean>() {

			@Override
			public Boolean call(String line) {
				return line.trim().length() > 0;
			}
		};
	}

}
