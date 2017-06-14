package au.gov.amsa.geo.adhoc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import au.gov.amsa.ais.rx.Streams;

public class LineAndAisMessageMain {
    
    private static final String TAG_BLOCK_REGEX = "\\\\.*\\*..?\\\\";

    public static void main(String[] args) throws IOException {
        String a = "\\s:Sugarloaf Point,c:1495756800,T:2017-05-26 00.00.00*64\\!BSVDM,1,1,,B,15DQAR00imbrvf?eEIjp<Faj00S1,0*65";
        System.out.println(a.replaceFirst("\\\\.*\\*..\\\\", ""));
//        System.exit(0);
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
                    String s = line.trim().replaceFirst(TAG_BLOCK_REGEX, "");
                    set.add(s);
                }
            }
        }
        Streams.nmeaFromGzip(new File("/media/an/amsa_26_05_2017_5_IEC/iec/2017-05-26.txt.gz")) //
                .compose(o -> Streams.extract(o)) //
                .filter(m -> m.getMessage().isPresent()
                        && is123(m.getMessage().get().message().getMessageId())) //
                .filter(x -> x.getMessage().get().time() >= 1495756800000L+ 100000) //
                .map(x -> x.getLine().trim()) //
                .filter(x -> !x.startsWith("\\1G2:") && ! x.startsWith("\\1G3") && !x.startsWith("\\2G3"))
                .filter(x -> !set.contains(x.replaceFirst(TAG_BLOCK_REGEX, ""))) //
                .doOnNext(System.out::println) //
                .count() //
                .doOnNext(System.out::println) //
                .toBlocking() //
                .subscribe();
    }

    private static boolean is123(int n) {
        return n == 1 || n == 2 || n == 3;
    }

}
