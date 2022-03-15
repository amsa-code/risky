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
import java.util.Arrays;
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
                .maxItemsPerFile(10000000) //
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
        long timeA = parseTime(itemsA[2]);
        long timeB = parseTime(itemsB[2]);
        int c = Long.compare(timeA, timeB);
        if (c == 0) {
            String mmsiA = itemsA[3];
            String mmsiB = itemsB[3];
            if (mmsiA.isEmpty()) {
                if (mmsiB.isEmpty()) {
                    String imoA = itemsA[4];
                    String imoB = itemsB[4];
                    return imoA.compareTo(imoB);
                } else {
                    return -1;
                }
            } else if (mmsiB.isEmpty()) {
                return -1;
            } else {
                return mmsiA.compareTo(mmsiB);
            }
        } else {
            return c;
        }
    }

}
