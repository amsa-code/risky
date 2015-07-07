package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.util.regex.Pattern;

import rx.Observable;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.util.Files;
import au.gov.amsa.util.Pair;

import com.github.davidmoten.rx.slf4j.Logging;

public class GeoffHosackAdhocMain {

    public static void main(String[] args) {
        int option = 2;
        if (option == 1) {
            File file = new File("/media/an/binary-fixes-5-minute/2014/235099876.track");
            BinaryFixes.from(file).doOnNext(System.out::println)
                    .toSortedList((a, b) -> Long.compare(a.time(), b.time()))
                    .flatMap(Observable::from).doOnCompleted(() -> System.out.println("complete"))
                    .subscribe();
        } else if (option == 2) {
            Observable<File> files = Observable.from(Files.find(new File(
                    "/media/an/binary-fixes-5-minute/2014"), Pattern.compile("\\d+\\.track")));

            files.filter(file -> !file.getName().equals("0.track"))
                    //
                    .flatMap(
                            file -> BinaryFixes.from(file).<Pair<File, Long>> map(
                                    fix -> Pair.create(file, fix.time())))
                    // log
                    .lift(Logging.<Pair<File, Long>> logger().showCount().every(1000000).log())
                    //
                    .reduce((p1, p2) -> {
                        if (p1.b() <= p2.b())
                            return p1;
                        else
                            return p2;
                    }).doOnNext(System.out::println).subscribe();
        }
    }
}
