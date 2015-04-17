package au.gov.amsa.navigation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.FixImpl;
import au.gov.amsa.risky.format.NavigationalStatus;
import au.gov.amsa.streams.Strings;

import com.github.davidmoten.rx.Checked;
import com.google.common.base.Optional;

public final class DriftCandidates {

    public static Observable<DriftCandidate> fromCsv(Reader reader) {
        return Strings.lines(reader)
        // remove blank lines
                .filter(nonBlankLinesOnly())
                // parse candidate
                .map(line -> toDriftCandidate(line));
    }

    private static DriftCandidate toDriftCandidate(String line) {
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
        FixImpl fix = new FixImpl(mmsi, lat, lon, time, Optional.<Integer> absent(),
                Optional.<Short> absent(), navigationalStatus, Optional.of(speedKnots),
                Optional.of(course), Optional.of(heading), aisClass);
        return new DriftCandidate(fix, driftingSince);
    }

    public static Observable<DriftCandidate> fromCsv(final File file) {
        Action1<Reader> disposeAction = reader -> {
            try {
                reader.close();
            } catch (IOException e) {
                // ignore
            }
        };
        Func0<Reader> resourceFactory = Checked.f0(() -> new InputStreamReader(new FileInputStream(
                file)));
        Func1<Reader, Observable<DriftCandidate>> obFactory = reader -> fromCsv(reader);
        return Observable.using(resourceFactory, obFactory, disposeAction, true);
    }

    private static Func1<String, Boolean> nonBlankLinesOnly() {
        return line -> line.trim().length() > 0;
    }

}
