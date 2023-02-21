package au.gov.amsa.ais;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import au.gov.amsa.ais.message.AisPositionA;
import au.gov.amsa.ais.message.AisPositionGPS;

public class HasNavigationalStatusTest {

    @Test
    public void testAisPositionA() {
        List<Class<?>> list = Arrays.asList(AisPositionA.class.getInterfaces());
        assertTrue(list.contains(HasNavigationalStatus.class));
    }

    @Test
    public void testAisPositionGPS() {
        List<Class<?>> list = Arrays.asList(AisPositionGPS.class.getInterfaces());
        assertTrue(list.contains(HasNavigationalStatus.class));
    }

}
