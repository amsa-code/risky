package au.gov.amsa.ihs.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;

import com.github.davidmoten.rx.Checked;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

public class ExtractAllCsvMain {

    public static Observable<Map<String, String>> extractMaps(File file, String parentElementName) {
        Func0<InputStream> resourceFactory = Checked.f0(() -> new FileInputStream(file));
        Func1<InputStream, Observable<Map<String, String>>> observableFactory = is -> Observable
                .just(is).lift(new OperatorIhsReader(parentElementName));
        Action1<InputStream> disposeAction = Checked.a1(is -> is.close());
        return Observable.using(resourceFactory, observableFactory, disposeAction);
    }

    public static void writeCsv(String parentElementName, File output, File... files) {
        Observable<Map<String, String>> o = Observable.from(files)
                .flatMap(file -> extractMaps(file, parentElementName));
        TreeSet<String> keys = o
                .collect(() -> new TreeSet<String>(), (set, map) -> set.addAll(map.keySet()))
                .toBlocking().single();
        System.out.println(keys);
        try {
            PrintStream out = new PrintStream(output);
            {
                boolean isFirst = true;
                for (String key : keys) {
                    if (!isFirst)
                        out.print("\t");
                    out.print(key);
                    isFirst = false;
                }
                out.println();
            }
            o.forEach(map -> {
                boolean isFirst = true;
                for (String key : keys) {
                    String value = map.get(key);
                    if (value == null)
                        value = "";
                    if (!isFirst)
                        out.print("\t");
                    out.print(value);
                    isFirst = false;
                }
                out.println();
            });
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File directory = new File("/media/an/ship-data/ihs/608750-2015-04-01/");
        File output = new File("target");

        extract(directory, output, "ShipData", "ShipData.xml", "ShipData1.xml");
        extract(directory, output, "ShipData", "ShipData1.xml");
        extract(directory, output, "tblBuilderAndSubcontractorDetails",
                "tblBuilderAndSubcontractorDetails.xml");

    }

    private static void extract(File directory, File output, String parentElement,
            String... names) {
        File[] files = Arrays.stream(names).map(name -> new File(directory, name))
                .toArray(n -> new File[n]);
        writeCsv(parentElement, new File(output, names[0].replace("xml", "txt")), files);
    }
}
