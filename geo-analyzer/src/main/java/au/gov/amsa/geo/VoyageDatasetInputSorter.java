package au.gov.amsa.geo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import com.github.davidmoten.bigsorter.Sorter;

public class VoyageDatasetInputSorter {

    private static final DateTimeFormatter format = new DateTimeFormatterBuilder()
            // case insensitive to parse JAN and FEB
            .parseCaseInsensitive()
            // add pattern
            .appendPattern("d-MMM-yy h.m.s.SSSSSSSSS a") //
            // create formatter (use English Locale to parse month names)
            .toFormatter(Locale.ENGLISH) //
            .withZone(ZoneId.of("UTC"));

    public static void main(String[] args) {
        Sorter.serializerLinesUtf8() //
                .comparator((a, b) -> compare(a, b)) //
                .input(() -> {
                    try {
                        return new GZIPInputStream(new BufferedInputStream(
                                new FileInputStream(new File("/home/dave/Downloads/export2.txt.gz"))));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }) //
                .output(new File("target/export2.sorted.txt")) //
                .maxItemsPerFile(1000000) //
                .initialSortInParallel() //
                .loggerStdOut() //
                .sort();
    }

    private static long parseTime(String s) {
        return ZonedDateTime.parse(s, format).toInstant().toEpochMilli();
    }

    private static int compare(String a, String b) {
        String[] itemsA = a.split(",");
        String[] itemsB = b.split(",");
        String mmsiA = itemsA[3];
        String mmsiB = itemsB[3];

        String ida = mmsiA.isEmpty() ? "-" + itemsA[4] : mmsiA;
        String idb = mmsiB.isEmpty() ? "-" + itemsB[4] : mmsiB;
        int c = ida.compareTo(idb);
        if (c == 0) {
            long timeA = parseTime(itemsA[2]);
            long timeB = parseTime(itemsB[2]);
            return Long.compare(timeA, timeB);
        } else {
            return c;
        }
    }

}
