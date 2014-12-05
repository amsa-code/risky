package au.gov.amsa.geo.consensus;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Test;

import au.gov.amsa.geo.model.Fix;

public class ConsensusUtilTest {

	@Test
	public void test() {
		String craftId = "1935104788";
		DateTime dt = DateTime.parse("2014-05-06T01:44:38.129");
		Fix fix1 = new Fix(craftId, -35.8923633333333, 138.308131666667,
				dt.getMillis());
		DateTime dt2 = DateTime.parse("2014-05-06T01:47:15.527");
		Fix fix2 = new Fix(craftId, -35.9026783333333, 138.31747,
				dt2.getMillis());
		DateTime dt3 = DateTime.parse("2014-05-06T02:27:47.000");
		Fix fix3 = new Fix(craftId, -39.05924, 138.464111666667,
				dt3.getMillis());
		Options options = Options.builder().before(10).after(10)
				.maxSpeedKnots(50)
				.adjustmentLowerLimitMillis(TimeUnit.HOURS.toMillis(-4))
				.adjustmentUpperLimitMillis(TimeUnit.HOURS.toMillis(4)).build();
		assertTrue(Consensus.consensus(fix1, fix2, options) > 1);
		assertTrue(Consensus.consensus(fix1, fix3, options) < 0.25);
	}
}
