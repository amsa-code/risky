package au.gov.amsa.risky.format;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.regex.Pattern;

import rx.functions.Action1;
import rx.functions.Func1;
import au.gov.amsa.util.Files;

public class LibSvmMain {

    public static void main(String[] args) throws IOException {

        // open an output writer
        final Writer writer = new FileWriter("target/fixes.libsvm");

        // specify which files have the fixes to process
        List<File> files = Files.find(new File("/media/an/binary-fixes-5-minute/2014"),
                Pattern.compile(".*\\.track"));

        // process the fixes in the files
        BinaryFixes.from(files)
        // just class A vessels
                .filter(classAOnly())
                // only fixes that have course, heading and speed present
                .filter(hasCourseHeadingSpeed())
                // write the fixes in LIBSVM format
                .forEach(writeFix(writer), handleError());

        // close the writer
        writer.close();

        System.out.println("finished");
    }

    private static Action1<Throwable> handleError() {
        return new Action1<Throwable>() {

            @Override
            public void call(Throwable t) {
                t.printStackTrace();
            }
        };
    }

    private static Action1<HasFix> writeFix(final Writer writer) {
        return new Action1<HasFix>() {

            @Override
            public void call(HasFix f) {
                int navStatus;

                if (f.fix().navigationalStatus().isPresent()) {
                    if (f.fix().navigationalStatus().get() == NavigationalStatus.MOORED)
                        navStatus = 1;
                    else if (f.fix().navigationalStatus().get() == NavigationalStatus.AT_ANCHOR)
                        navStatus = 2;
                    else
                        navStatus = 0;
                } else
                    navStatus = 0;
                Fix fix = f.fix();
                float diff = Math.abs(fix.courseOverGroundDegrees().get()
                        - fix.headingDegrees().get());
                LibSvm.write(writer, navStatus, f.fix().lat(), f.fix().lon(), fix
                        .speedOverGroundKnots().get(), diff);
            }
        };
    }

    private static Func1<HasFix, Boolean> hasCourseHeadingSpeed() {
        return new Func1<HasFix, Boolean>() {

            @Override
            public Boolean call(HasFix fix) {
                Fix f = fix.fix();
                return f.courseOverGroundDegrees().isPresent()
                        && f.headingDegrees().isPresent()
                        && f.speedOverGroundKnots().isPresent();
            }
        };
    }

    private static Func1<HasFix, Boolean> classAOnly() {
        return new Func1<HasFix, Boolean>() {

            @Override
            public Boolean call(HasFix fix) {
                Fix f = fix.fix();
                return (f.aisClass() == AisClass.A);

            }
        };
    }

}
