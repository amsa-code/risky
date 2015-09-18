package au.gov.amsa.risky.format;

import static java.lang.System.setProperty;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class DownsampleMainTest {

    @Test
    public void test() {
        String filename = "target/downsample-main-test/123456789.track";
        File file = new File(filename);
        file.getParentFile().mkdirs();
        File outFile = new File("target/ds-output/123456789.track");
        TestingUtil.writeTwoBinaryFixes(filename, BinaryFixesFormat.WITHOUT_MMSI);
        setProperty("input", file.getParent());
        setProperty("output", outFile.getParent());
        setProperty("pattern", ".*.track");
        setProperty("ms", "" + TimeUnit.MINUTES.toMillis(500000));
        DownsampleMain.main(new String[] {});
        // only one file should be there
        assertEquals(1, outFile.getParentFile().list().length);
        // only one of the fixes should make it through to the output file
        assertEquals(BinaryFixes.recordSize(BinaryFixesFormat.WITHOUT_MMSI), outFile.length());
    }
}
