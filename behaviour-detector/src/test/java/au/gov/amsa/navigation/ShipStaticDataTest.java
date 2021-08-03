package au.gov.amsa.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import au.gov.amsa.navigation.ShipStaticData.Info;
import au.gov.amsa.risky.format.AisClass;

public class ShipStaticDataTest {

    @Test
    public void testParse() {
        InputStreamReader isr = new InputStreamReader(
                ShipStaticDataTest.class.getResourceAsStream("/ship-data.txt"), StandardCharsets.UTF_8);
        List<Info> list = ShipStaticData.fromAndClose(isr).toList().toBlocking().single();
        assertEquals(2, list.size());
        Info a = list.get(0);
        Info b = list.get(1);
        // a
        assertEquals(636014423, a.mmsi);
        assertFalse(a.imo.isPresent());
        assertEquals(AisClass.B, a.cls);
        assertEquals(81, (int) a.shipType.get());
        assertEquals(8.5, a.maxDraftMetres.get(), 0.00001);
        assertEquals(202, (int) a.dimensionAMetres.get());
        assertEquals(46, (int) a.dimensionBMetres.get());
        assertEquals(18, (int) a.dimensionCMetres.get());
        assertEquals(22, (int) a.dimensionDMetres.get());
        assertEquals(248, (int) a.lengthMetres().get());
        assertEquals(40, (int) a.widthMetres().get());
        assertFalse(a.name.isPresent());
        // b
        assertEquals(548777000, b.mmsi);
        assertEquals("9363821", b.imo.get());
        assertEquals(AisClass.A, b.cls);
        assertEquals(89, (int) b.shipType.get());
        assertEquals(6.4, b.maxDraftMetres.get(), 0.00001);
        assertEquals(120, (int) b.dimensionAMetres.get());
        assertEquals(25, (int) b.dimensionBMetres.get());
        assertEquals(18, (int) b.dimensionCMetres.get());
        assertEquals(6, (int) b.dimensionDMetres.get());
        assertEquals(145, (int) b.lengthMetres().get());
        assertEquals(24, (int) b.widthMetres().get());
        assertEquals("AS ORELIA", b.name.get());
    }

}
