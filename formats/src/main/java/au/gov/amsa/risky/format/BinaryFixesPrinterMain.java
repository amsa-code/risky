package au.gov.amsa.risky.format;

import java.io.File;

import rx.functions.Action1;

public class BinaryFixesPrinterMain {

    public static void main(String[] args) {
        File file = new File(System.getProperty("file"));
        BinaryFixes.from(file).doOnNext(new Action1<Fix>() {
            @Override
            public void call(Fix fix) {
                System.out.println(fix);
            }
        });
    }

}
