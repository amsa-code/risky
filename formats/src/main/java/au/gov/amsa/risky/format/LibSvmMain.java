package au.gov.amsa.risky.format;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import au.gov.amsa.util.Files;

public class LibSvmMain {

    public static void main(String[] args) throws IOException {
        final Writer writer = new FileWriter("target/fixes.libsvm");
        Observable<Fix> fixes = BinaryFixes.from(Files.find(new File(
                "/media/an/binary-fixes-5-minute/2014"), Pattern.compile(".*\\.track")));
        fixes.filter(new Func1<HasFix, Boolean>() {

            @Override
            public Boolean call(HasFix fix) {
                Fix f = fix.fix();
                return (f.getAisClass() == AisClass.A)
                        && f.getCourseOverGroundDegrees().isPresent()
                        && f.getHeadingDegrees().isPresent()
                        && f.getSpeedOverGroundKnots().isPresent();

            }
        }).take(1000000).forEach(new Action1<HasFix>() {

            @Override
            public void call(HasFix f) {
                int navStatus;

                if (f.fix().getNavigationalStatus().isPresent()) {
                    if (f.fix().getNavigationalStatus().get() == NavigationalStatus.MOORED)
                        navStatus = 1;
                    else if (f.fix().getNavigationalStatus().get() == NavigationalStatus.AT_ANCHOR)
                        navStatus = 2;
                    else
                        navStatus = 0;
                } else
                    navStatus = 0;
                Fix fix = f.fix();
                float diff = Math.abs(fix.getCourseOverGroundDegrees().get()
                        - fix.getHeadingDegrees().get());
                LibSvm.write(writer, navStatus, f.fix().getLat(), f.fix().getLon(), fix
                        .getSpeedOverGroundKnots().get(), diff);
            }
        }, new Action1<Throwable>() {

            @Override
            public void call(Throwable t) {
                t.printStackTrace();
            }
        });
        writer.close();
        System.out.println("finished");
    }

}
