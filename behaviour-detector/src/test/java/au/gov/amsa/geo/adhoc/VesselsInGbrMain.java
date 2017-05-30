package au.gov.amsa.geo.adhoc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.Files;
import au.gov.amsa.util.identity.MmsiValidator2;
import rx.Observable;
import rx.Scheduler;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;

public class VesselsInGbrMain {

    public static void main(String[] args) throws IOException {
        long t = System.currentTimeMillis();
        File out = new File("target/mmsi.txt");
        out.delete();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(out)))) {
            Pattern pattern = Pattern.compile(".*\\.track");
            List<File> list = new ArrayList<File>();
            list.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2014"), pattern));
            list.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2015"), pattern));
            list.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2016"), pattern));
            AtomicInteger count = new AtomicInteger();

            Observable.from(list) //
                    .groupBy(f -> count.getAndIncrement()
                            % Runtime.getRuntime().availableProcessors()) //
                    .flatMap(files -> vesselsInGbr(files, Schedulers.computation())) //
                    .distinct(fix -> fix.mmsi() + fix.aisClass().name()) //
                    .sorted((a, b) -> Integer.compare(a.mmsi(), b.mmsi())) //
                    .filter(fix -> MmsiValidator2.INSTANCE.isValid((long) fix.mmsi())) //
                    .doOnNext(fix -> write(writer, fix)) //
                    .toBlocking() //
                    .subscribe();
        }
        System.out.println((System.currentTimeMillis() - t) + "ms");
    }

    private static void write(BufferedWriter b, Fix fix) {
        try {
            b.write(String.valueOf(fix.mmsi()));
            b.write('\t');
            b.write(fix.aisClass().name());
            b.write('\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Observable<Fix> vesselsInGbr(GroupedObservable<Integer, File> files,
            Scheduler scheduler) {
        return files //
                .flatMap(f -> BinaryFixes.from(f) //
                        .filter(fix -> inGbr(fix)) //
                        .distinct(fix -> fix.mmsi() + fix.aisClass().name()) //
                        .subscribeOn(scheduler));
    }

    private static boolean inGbr(Fix fix) {
        return fix.lat() >= -27.8 && fix.lat() <= -8.4 && fix.lon() >= 142 && fix.lon() <= 162;
    }

}
