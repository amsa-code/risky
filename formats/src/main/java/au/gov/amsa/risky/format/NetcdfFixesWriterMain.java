package au.gov.amsa.risky.format;

import java.io.File;
import java.util.regex.Pattern;

public class NetcdfFixesWriterMain {

    public static void main(String[] args) {
        try {
            File input = new File(System.getProperty("input"));
            File output = new File(System.getProperty("output"));
            Pattern pattern = Pattern.compile(System.getProperty("pattern"));
            NetcdfFixesWriter.convertToNetcdf(input, output, pattern).count().toBlocking().single();
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.out
                    .println("Usage: -Dinput=<input directory> -Doutput=<output directory> -Dpattern=<filename pattern>");
        }
    }
}
