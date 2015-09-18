package au.gov.amsa.risky.format;

import java.io.File;

public class ByMmsiToDailyMain {

    public static void main(String[] args) {
        System.setProperty("input", "/media/an/binary-fixes-5-minute/2014");
        System.setProperty("output", "/media/an/daily-fixes-5-minute/2014");
        File input = new File(System.getProperty("input"));
        File output = new File(System.getProperty("output"));
        ByMmsiToDailyConverter
                .sortFixFile(new File("/media/an/daily-fixes-5-minute/2014/2014-11-19.fix"));
        // ByMmsiToDailyConverter.convert(input, output);
        ByMmsiToDailyConverter.sort(output);
    }
}
