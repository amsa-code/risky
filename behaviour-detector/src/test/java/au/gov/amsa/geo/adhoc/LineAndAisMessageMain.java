package au.gov.amsa.geo.adhoc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.gov.amsa.ais.rx.Streams;

public class LineAndAisMessageMain {

    private static final long START_TIME = 1495756800L;
    private static final String TAG_BLOCK_REGEX = "\\\\.*\\*..?\\\\";

    public static void main(String[] args) throws IOException {
        long t = System.currentTimeMillis();
        if (false) {
            try (PrintStream out = new PrintStream("target/subset.txt")) {
                Pattern p = Pattern.compile("\\\\.*,c:(14\\d{8}),.*\\\\.*");
                Streams.nmeaFromGzip(
                        new File("/media/an/amsa_26_05_2017_5_IEC/iec/2017-05-26.txt.gz"))
                        .concatWith(Streams.nmeaFromGzip(
                                new File("/media/an/amsa_26_05_2017_5_IEC/iec/2017-05-27.txt.gz")))
                        .filter(x -> {
                            Matcher m = p.matcher(x);
                            if (m.matches()) {
                                String s = m.group(1);
                                long epochSeconds = Long.parseLong(s);
                                return epochSeconds >= START_TIME
                                        && epochSeconds <= START_TIME + TimeUnit.DAYS.toSeconds(1);
                            } else {
                                return false;
                            }
                        }).doOnNext(out::println) //
                        .subscribe();
            }
            System.out.println(System.currentTimeMillis() - t + "ms");
            System.exit(0);
        }
        if (false) {
            String a = "\\s:Sugarloaf Point,c:1495756800,T:2017-05-26 00.00.00*64\\!BSVDM,1,1,,B,15DQAR00imbrvf?eEIjp<Faj00S1,0*65";
            System.out.println(a.replaceFirst("\\\\.*\\*..\\\\", ""));
            System.out.println(significant(a));
            System.exit(0);
        }
        BufferedWriter b = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream("target/out.txt")));
        // System.exit(0);
        Set<String> set = new HashSet<>(7000000);
        for (int i = 1; i <= 10; i++) {
            File f = new File(
                    "/media/an/amsa_26_05_2017_5_IEC/iec/amsa_26_05_2017_5_ITU123_20170526_" + i
                            + ".txt");
            System.out.println("reading " + f);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f)))) {
                String line;
                while ((line = br.readLine()) != null) {
                    b.write(line.trim());
                    b.write("\n");
                    String s = significant(line.trim());
                    set.add(s);
                }
            }
        }
        b.close();
        Set<String> set2 = new HashSet<String>();
        System.out.println("written");
        AtomicInteger count123 = new AtomicInteger(0);
        AtomicInteger samples = new AtomicInteger(0);
        Streams.nmeaFromGzip(new File("/media/an/amsa_26_05_2017_5_IEC/iec/subset.txt.gz")) //
                .compose(o -> Streams.extractWithLines(o)) //
                .doOnNext(m -> {
                    if (m.getError() != null) {
                        System.out.println();
                    }
                }) //
                .filter(m -> m.getMessage().isPresent()
                        && is123(m.getMessage().get().message().getMessageId())) //
                .filter(x -> x.getMessage().get().time() >= START_TIME) //
                .map(x -> x.getLines().get(x.getLines().size() - 1)) //
                .doOnNext(x -> count123.incrementAndGet()) //
                // .filter(x -> !x.startsWith("\\1G2:") &&
                // !x.startsWith("\\1G3")
                // && !x.startsWith("\\2G3"))
                .doOnNext(x -> set2.add(significant(x))) //
                .filter(x -> !set.contains(significant(x))) //
                .doOnNext(x -> {
                    if (samples.incrementAndGet() <= 100) {
                        System.out.println(x);
                    }
                }) //
                .count() //
                .doOnNext(System.out::println) //
                .toBlocking() //
                .subscribe();
        System.out.println("count123=" + count123.get());
        System.out.println((System.currentTimeMillis() - t) + "ms");

        int count = 0;
        for (String s : set) {
            if (!set2.contains(s)) {
                if (count++ < 1000) {
                    System.out.println(s);
                }
            }
        }
        System.out.println("not found=" + count);
        System.out.println((System.currentTimeMillis() - t) + "ms");
    }

    private static boolean is123(int n) {
        return n == 1 || n == 2 || n == 3;
    }

    private static final Pattern extractPattern = Pattern.compile(",([^,]*),[^,]\\*..$");

    private static String significant(String nmea) {
        Matcher m = extractPattern.matcher(nmea);
        if (m.find()) {
            return m.group(1);
        } else {
            System.err.println(nmea);
            return "unknown";
        }
        // return nmea.replaceFirst(TAG_BLOCK_REGEX, "");
    }

}
