package au.gov.amsa.risky.format;

import static com.google.common.base.Optional.of;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TestingUtil {

    static void writeTwoBinaryFixes(String filename, BinaryFixesFormat format) {
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(filename));
            long t = 1421708455237L;
            Fix fix1 = new FixImpl(213456789, -10f, 135f, t, of(12), of((short) 1),
                    of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f), of(46f),
                    AisClass.B);
            Fix fix2 = new FixImpl(213456789, -10.1f, 135.2f, t + 1000 * 3600 * 2L, of(13),
                    of((short) 2), of(NavigationalStatus.AT_ANCHOR), of(4.5f), of(20f), of(30f),
                    AisClass.B);
            BinaryFixes.write(fix1, os, format);
            BinaryFixes.write(fix2, os, format);
            os.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
