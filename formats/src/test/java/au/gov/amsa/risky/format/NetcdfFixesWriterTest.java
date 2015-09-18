package au.gov.amsa.risky.format;

import static com.google.common.base.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.Test;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

public class NetcdfFixesWriterTest {

    private static final float PRECISION = 0.001f;

    @Test
    public void testWriting() {
        Fix f1 = createFix(TimeUnit.DAYS.toMillis(1), -10.5f, 135f);
        FixImpl f2 = createFix(TimeUnit.DAYS.toMillis(2), -10.8f, 136f);
        List<HasFix> fixes = Arrays.asList((HasFix) f1, f2);
        NetcdfFixesWriter.writeFixes(fixes, new File("target/test.nc"));
    }

    @Test
    public void testNetcdfConverter() throws IOException, InvalidRangeException {
        TestingUtil.writeTwoBinaryFixes("target/987654321.track", BinaryFixesFormat.WITHOUT_MMSI);
        int count = NetcdfFixesWriter.convertToNetcdf(new File("target"), new File("target/nc"),
                Pattern.compile("987654321.track")).count().toBlocking().single();
        assertEquals(1, count);
        // test output
        File ncFile = new File("target/nc/987654321.nc");
        assertTrue(ncFile.exists());
        assertTrue(ncFile.length() > 0);
        NetcdfFile nc = NetcdfFile.open(ncFile.getCanonicalPath());
        List<Attribute> attributes = nc.findGroup(null).getAttributes();
        System.out.println(attributes);
        assertFalse(attributes.isEmpty());
        System.out.println(nc.getDimensions());

        {
            Array array = nc.readSection("latitude");
            assertEquals(-10f, array.getFloat(0), PRECISION);
            assertEquals(-10.1f, array.getFloat(1), PRECISION);
            assertEquals(2, array.getSize());
        }
        {
            Array array = nc.readSection("longitude");
            assertEquals(135f, array.getFloat(0), PRECISION);
            assertEquals(135.2f, array.getFloat(1), PRECISION);
            assertEquals(2, array.getSize());
        }
        {
            Array array = nc.readSection("source");
            assertEquals((short) 1, array.getShort(0));
            assertEquals((short) 2, array.getShort(1));
            assertEquals(2, array.getSize());
        }
        {
            Array array = nc.readSection("latency");
            assertEquals(12, array.getInt(0));
            assertEquals(13, array.getInt(1));
            assertEquals(2, array.getSize());
        }
        {
            Array array = nc.readSection("navigational_status");
            assertEquals(NavigationalStatus.ENGAGED_IN_FISHING.ordinal(), array.getByte(0));
            assertEquals(NavigationalStatus.AT_ANCHOR.ordinal(), array.getByte(1));
            assertEquals(2, array.getSize());
        }
        {
            Array array = nc.readSection("rate_of_turn");
            assertEquals(-128, array.getByte(0));
            assertEquals(-128, array.getByte(1));
            assertEquals(2, array.getSize());
        }
        {
            Array array = nc.readSection("speed_over_ground");
            assertEquals(75, array.getInt(0));
            assertEquals(45, array.getInt(1));
            assertEquals(2, array.getSize());
        }
        {
            Array array = nc.readSection("course_over_ground");
            assertEquals(450, array.getShort(0));
            assertEquals(200, array.getShort(1));
            assertEquals(2, array.getSize());
        }
        {
            Array array = nc.readSection("heading");
            assertEquals(46, array.getShort(0));
            assertEquals(30, array.getShort(1));
            assertEquals(2, array.getSize());
        }
        {
            Array array = nc.readSection("ais_class");
            assertEquals(1, array.getInt(0));
            assertEquals(1, array.getInt(1));
            assertEquals(2, array.getSize());
        }
    }

    private static FixImpl createFix(long time, float lat, float lon) {
        return new FixImpl(213456789, lat, lon, time, of(12), of((short) 1),
                of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f), of(46f), AisClass.B);
    }

}
