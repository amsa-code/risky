package au.gov.amsa.geo.distance;

import static org.junit.Assert.assertEquals;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.junit.Test;

import com.github.davidmoten.grumpy.core.Position;

import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.FixImpl;

public class OperatorEffectiveSpeedCheckerTest {

    @Test
    public void testSpiritOfTasmania() throws ParseException {
        SegmentOptions options = SegmentOptions.builder().acceptAnyFixHours(12L).maxSpeedKnots(50)
                .build();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        Fix a = new FixImpl(1, -39.572643f, 145.38763f, df.parse("2015-01-01T03:36:36").getTime(),
                AisClass.A);
        Fix b = new FixImpl(1, -40.095074f, 145.71864f, df.parse("2015-01-01T04:52:25").getTime(),
                AisClass.A);

        System.out.println(
                Position.create(a.lat(), a.lon()).getDistanceToKm(Position.create(b.lat(), b.lon()))
                        / 1.852 / (75.81666666 / 60) + "knots");

        assertEquals(27.605441702145423,
                EffectiveSpeedChecker.effectiveSpeedKnots(a, b, options).get(), 0.00001);
    }
}
