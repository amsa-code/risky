package au.gov.amsa.navigation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.Files;
import rx.Observable;

public class VoyageDatasetProducer {
    public static void produce() throws Exception {
        long t = System.currentTimeMillis();
        File out = new File("target/positions.txt");
        out.delete();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out)))) {
            Pattern pattern = Pattern.compile(".*\\.track");
            List<File> list = new ArrayList<File>();

            // list.addAll(Files.find(new
            // File("/media/an/binary-fixes-5-minute/2014"), pattern));
            // list.addAll(Files.find(new
            // File("/media/an/binary-fixes-5-minute/2015"), pattern));
            list.addAll(Files.find(new File("/home/dave/Downloads/2016"), pattern));
            AtomicInteger count = new AtomicInteger();

            int numFiles = list.size();
            System.out.println(numFiles + " files");

            AtomicInteger fileNumber = new AtomicInteger(0);
            Observable.from(list) //
                    // .groupBy(f -> count.getAndIncrement() %
                    // Runtime.getRuntime().availableProcessors()) //
                    .groupBy(f -> f.getName().substring(0, f.getName().indexOf("."))) //
                    .flatMap(files -> files // s
                            .compose(o -> logPercentCompleted(numFiles, o, fileNumber)) //
                            .concatMap(BinaryFixes::from) //
                            .compose(o -> toWaypoints(o)) //
                    // .filter(x -> inGbr(x)) //
                    // .onBackpressureBuffer() //
                    // .subscribeOn(Schedulers.computation()) //
                    ) //
                    .count() //
                    .doOnNext(System.out::println) //
                    .toBlocking() //
                    .subscribe();
        }
        System.out.println((System.currentTimeMillis() - t) + "ms");
    }

    private static Observable<File> logPercentCompleted(int numFiles, Observable<File> o, AtomicInteger fileNumber) {
        return o.doOnNext(file -> {
            int n = fileNumber.incrementAndGet();
            if (n % 1000 == 0)
                System.out.println("complete: " + new DecimalFormat("0.0").format(n / (double) numFiles * 100) + "%");
        });
    }

    private static final class Waypoint {

    }

    private enum EezStatus {
        IN, OUT, UNKNOWN;
    }

    private static final class State {
        final EezStatus eez;
        final long time;

        public State(EezStatus eez, long time) {
            this.eez = eez;
            this.time = time;
        }
    }

    private static final long FIX_AGE_THRESHOLD_MS = TimeUnit.DAYS.toMillis(5);

    private static Observable<Waypoint> toWaypoints(Observable<Fix> fixes) {
        return Observable.defer(() -> //
        {
            State[] state = new State[1];
            state[0] = new State(EezStatus.UNKNOWN, 0);
            return fixes //
                    .map(fix -> {
                        boolean inEez = inEez(fix);
                        State previous = state[0];
                        boolean previousRecent = fix.time() - previous.time <= FIX_AGE_THRESHOLD_MS;
                        boolean crossed = (inEez && previous.eez == EezStatus.OUT)
                                || (!inEez && previous.eez == EezStatus.IN);
                        if (previousRecent && crossed) {
                            
                        }
                        return null;
                    }) //
                    .ignoreElements() //
                    .cast(Waypoint.class);
        });

    }

    private static boolean inEez(Fix fix) {
        return true;
    }

    private static boolean inGbr(Fix fix) {
        return fix.lat() >= -27.8 && fix.lat() <= -8.4 && fix.lon() >= 142 && fix.lon() <= 162;
    }

    public static void main(String[] args) throws Exception {
        VoyageDatasetProducer.produce();
    }
}
