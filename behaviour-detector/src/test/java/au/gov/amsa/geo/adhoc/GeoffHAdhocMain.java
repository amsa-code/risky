package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.util.Files;
import au.gov.amsa.util.Pair;

public class GeoffHAdhocMain {

    public static void main(String[] args) {
        int option = 1;
        if (option == 1) {
            String filename = "/home/dxm/temp/235099876.track";
            // String filename =
            // "/media/an/binary-fixes-5-minute/2014/235099876.track";
            File file = new File(filename);
            BinaryFixes.from(file).doOnNext(System.out::println)
                    .toSortedList((a, b) -> Long.compare(a.time(), b.time()))
                    .flatMap(Observable::from).doOnCompleted(() -> System.out.println("complete"))
                    .subscribe();
        } else if (option == 2) {
            Observable<File> files = Observable.from(Files.find(new File(
                    "/media/an/binary-fixes-5-minute/2014"), Pattern.compile("\\d+\\.track")));

            Func2<Pair<File, Long>, Pair<File, Long>, Pair<File, Long>> earliest = (p1, p2) -> {
                if (p1.b() <= p2.b())
                    return p1;
                else
                    return p2;
            };
            files.filter(file -> !file.getName().equals("0.track"))
                    // read fixes from file
                    .flatMap(
                            file -> BinaryFixes.from(file)
                                    .<Pair<File, Long>> map(fix -> Pair.create(file, fix.time()))
                                    .reduce(earliest)
                                    .doOnNext(p -> System.out.println(file + ": " + p))
                                    .subscribeOn(Schedulers.computation()))
                    //
                    .reduce(earliest)
                    // print answer
                    .doOnNext(System.out::println)
                    // go
                    .toBlocking().single();
        }
    }
}
