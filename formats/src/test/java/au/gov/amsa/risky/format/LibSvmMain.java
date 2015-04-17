package au.gov.amsa.risky.format;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import rx.functions.Action1;
import rx.functions.Func1;
import au.gov.amsa.risky.format.OperatorMinEffectiveSpeedThreshold.FixWithPreAndPostEffectiveSpeed;
import au.gov.amsa.util.Files;

import com.github.davidmoten.rx.slf4j.Logging;

public class LibSvmMain {

    public static void main(String[] args) throws IOException {

        // open an output writer
        final Writer writer = new FileWriter("/media/an/fixes.libsvm");

        // specify which files have the fixes to process
        List<File> files = Files.find(new File("/media/an/binary-fixes-5-minute/2014"),
                Pattern.compile(".*\\.track"));

        // process the fixes in the files
        BinaryFixes.from(files)
        // log
                .lift(Logging.<HasFix> logger().showCount().every(1000000).log())
                // just class A vessels
                .filter(classAOnly())
                // only fixes that have course, heading and speed present
                .filter(hasCourseHeadingSpeed())
                // emit with params
                .lift(new OperatorMinEffectiveSpeedThreshold(TimeUnit.HOURS.toMillis(1)))
                // log
                // write the fixes in LIBSVM format
                .forEach(writeFix(writer), t -> t.printStackTrace());

        // close the writer
        writer.close();

        System.out.println("finished");
    }

    private static Action1<FixWithPreAndPostEffectiveSpeed> writeFix(final Writer writer) {
        return f -> {
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
            float diff = Math.abs(fix.courseOverGroundDegrees().get() - fix.headingDegrees().get());
            LibSvm.write(writer, navStatus, f.fix().lat(), f.fix().lon(), fix
                    .speedOverGroundKnots().get(), diff, f.preEffectiveSpeedKnots(), f.preError(),
                    f.postEffectiveSpeedKnots(), f.postError());
        };
    }

    private static Func1<HasFix, Boolean> hasCourseHeadingSpeed() {
        return fix -> {
            Fix f = fix.fix();
            return f.courseOverGroundDegrees().isPresent() && f.headingDegrees().isPresent()
                    && f.speedOverGroundKnots().isPresent();
        };
    }

    private static Func1<HasFix, Boolean> classAOnly() {
        return fix -> {
            Fix f = fix.fix();
            return (f.aisClass() == AisClass.A);
        };
    }

}
