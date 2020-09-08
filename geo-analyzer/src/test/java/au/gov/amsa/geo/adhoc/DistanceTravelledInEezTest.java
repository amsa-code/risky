package au.gov.amsa.geo.adhoc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DistanceTravelledInEezTest {

    @Test
    public void test() {
        assertEquals("01-Feb-2019", DistanceTravelledInEezMain.formattedDate("2019-02-01"));
    }

}
