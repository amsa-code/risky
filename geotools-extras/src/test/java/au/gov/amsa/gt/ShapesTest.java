package au.gov.amsa.gt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;

public class ShapesTest {

    @Test
    public void testCreate() {
        new Shapes();
    }

    @Test
    public void testPointInSrrAndCoralSeaAtba() {
        assertEquals(Arrays.asList("SRR", "Coral Sea ATBA"),
                new Shapes().containing(-17.050, 149.678).collect(Collectors.toList()));
    }

    @Test
    public void testPointInSrrAndNotInCoralSeaAtba() {
        assertEquals(Arrays.asList("SRR"),
                new Shapes().containing(-17.050, 100).collect(Collectors.toList()));
    }

    @Test
    public void testPointInNeitherSrrNorInCoralSeaAtba() {
        assertTrue(new Shapes().containing(30, 100).collect(Collectors.toList()).isEmpty());
    }
}
