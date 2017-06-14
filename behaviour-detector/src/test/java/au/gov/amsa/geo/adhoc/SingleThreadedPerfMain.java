package au.gov.amsa.geo.adhoc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.github.davidmoten.rx.Schedulers;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.util.Files;
import rx.Observable;

public class SingleThreadedPerfMain {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        long t = System.currentTimeMillis();
        File out = new File("target/positions.txt");
        out.delete();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(out)))) {
            Pattern pattern = Pattern.compile(".*\\.track");
            List<File> list = new ArrayList<File>();
            list.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2014"), pattern));
            list.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2015"), pattern));
            list.addAll(Files.find(new File("/media/an/binary-fixes-5-minute/2016"), pattern));

            Observable.from(list) //
                    .concatMap(BinaryFixes::from) //
                    .count() //
                    .doOnNext(System.out::println) //
                    .toBlocking() //
                    .subscribe();
        }
        System.out.println((System.currentTimeMillis() - t) + "ms");
    }

}
