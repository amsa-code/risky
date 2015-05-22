package au.gov.amsa.navigation;

import static org.junit.Assert.assertEquals;

import java.io.InputStreamReader;

import org.junit.Test;

import com.google.common.base.Charsets;

public class ShipStaticDataTest {

    @Test
    public void testParse() {
        InputStreamReader isr = new InputStreamReader(
                ShipStaticDataTest.class.getResourceAsStream("/ship-data.txt"), Charsets.UTF_8);
        assertEquals(2, (int) ShipStaticData.fromAndClose(isr).count().toBlocking().single());
    }

}
