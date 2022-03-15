package au.gov.amsa.geo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

public class CsvSplitterByYear {

    String currentYear;
    PrintStream out;

    public void split() throws IOException {
        File input = new File("/home/dave/export.txt");
        File outputDirectory = new File("target/yearly");
        try {
            Files.lines(input.toPath()) //
                    .forEach(line -> {
                        int i = line.indexOf(',');
                        String year = line.substring(i + 1, i + 5);
                        // first ensure that reports from previous year that have been 
                        // delayed are ignored (some satellite reports)
                        if (currentYear == null || year.compareTo(currentYear) >= 0) {
                            if (!year.equals(currentYear)) {
                                currentYear = year;
                                try {
                                    out = new PrintStream(new GZIPOutputStream(
                                            new FileOutputStream(new File(outputDirectory, year + ".txt.gz"))));
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }
                            out.println(line);
                        }
                    });
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new CsvSplitterByYear().split();
    }

}
