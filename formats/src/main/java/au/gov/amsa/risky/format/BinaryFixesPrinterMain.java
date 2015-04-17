package au.gov.amsa.risky.format;

import java.io.File;

public class BinaryFixesPrinterMain {

    public static void main(String[] args) {
        File file = new File(System.getProperty("file"));
        BinaryFixes.from(file).doOnNext(System.out::println).subscribe();
    }
}
