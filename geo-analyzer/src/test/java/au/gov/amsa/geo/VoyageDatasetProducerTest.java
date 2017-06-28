package au.gov.amsa.geo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import au.gov.amsa.geo.VoyageDatasetProducer.EezWaypoint;
import au.gov.amsa.geo.VoyageDatasetProducer.Port;
import au.gov.amsa.geo.VoyageDatasetProducer.TimedLeg;
import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.risky.format.Fix;
import rx.observers.AssertableSubscriber;
import rx.subjects.PublishSubject;

public class VoyageDatasetProducerTest {

    private static final String EEZ_WAYPOINT_NAME = "test";

    @Test
    public void test() throws IOException {
        long aTime = 1498199825552L;
        long bTime = aTime + TimeUnit.DAYS.toMillis(2);
        long cTime = bTime + TimeUnit.DAYS.toMillis(4);
        Shapefile eezLine = VoyageDatasetProducer.loadEezLine();
        Shapefile eezPolygon = VoyageDatasetProducer.loadEezPolygon();
        assertTrue(eezPolygon.contains(-35, 149));
        assertTrue(!eezPolygon.contains(-35, 175));

        Fix a = createOutOfEez(aTime);

        Fix b = createInEez(bTime);

        PublishSubject<Fix> fixes = PublishSubject.create();
        Collection<Port> ports = VoyageDatasetProducer.loadPorts();
        Collection<EezWaypoint> eezWaypoints = Collections
                .singleton(new EezWaypoint(EEZ_WAYPOINT_NAME, -35, 151.0, Optional.empty()));
        AssertableSubscriber<TimedLeg> ts = VoyageDatasetProducer
                .toLegs(eezLine, eezPolygon, ports, eezWaypoints, fixes) //
                .test();
        fixes.onNext(a);
        fixes.onNext(b);
        ts.assertNoValues();

        // in sydney port
        Fix c = createInSydneyPort(cTime);

        fixes.onNext(c);
        ts.assertNoTerminalEvent() //
                .assertValueCount(1);
        {
            TimedLeg leg = ts.getOnNextEvents().get(0);
            System.out.println(leg);
            assertTrue(leg.a.waypoint instanceof EezWaypoint);
            assertTrue(leg.b.waypoint instanceof Port);
            assertEquals(EEZ_WAYPOINT_NAME, leg.a.waypoint.name());
            assertEquals("Sydney", leg.b.waypoint.name());
            assertEquals(cTime, leg.b.time);
            assertTrue(leg.a.time > a.time());
            assertTrue(leg.a.time < b.time());
            assertTrue(leg.a.time < leg.b.time);
        }

        // another sydney report, the next leg should start with this timestamp
        long dTime = cTime + TimeUnit.DAYS.toMillis(1);
        Fix d = createInSydneyPort(dTime);
        fixes.onNext(d);
        ts.assertNoTerminalEvent() //
                .assertValueCount(1);

        // out of sydney
        long eTime = dTime + TimeUnit.DAYS.toMillis(1);
        Fix e = createInEez(eTime);
        fixes.onNext(e);
        ts.assertNoTerminalEvent() //
                .assertValueCount(1);

        // out of eez
        long fTime = eTime + TimeUnit.DAYS.toMillis(1);
        Fix f = createOutOfEez(fTime);
        fixes.onNext(f);
        ts.assertNoTerminalEvent() //
                .assertValueCount(2);
        {
            TimedLeg leg = ts.getOnNextEvents().get(1);
            System.out.println(leg);
            assertTrue(leg.a.waypoint instanceof Port);
            assertEquals("Sydney", leg.a.waypoint.name());
            assertTrue(leg.b.waypoint instanceof EezWaypoint);
            assertEquals(EEZ_WAYPOINT_NAME, leg.b.waypoint.name());
            assertEquals(dTime, leg.a.time);
        }
    }

    private static Fix createOutOfEez(long bTime) {
        Fix b = Mockito.mock(Fix.class);
        when(b.lat()).thenReturn(-35f);
        when(b.lon()).thenReturn(175f);
        when(b.time()).thenReturn(bTime);
        return b;
    }

    private static Fix createInEez(long aTime) {
        Fix a = Mockito.mock(Fix.class);
        when(a.lat()).thenReturn(-35f);
        when(a.lon()).thenReturn(149f);
        when(a.time()).thenReturn(aTime);
        return a;
    }

    private static Fix createInSydneyPort(long aTime) {
        Fix a = Mockito.mock(Fix.class);
        when(a.lat()).thenReturn(-33.8523f);
        when(a.lon()).thenReturn(151.2108f);
        when(a.time()).thenReturn(aTime);
        return a;
    }
}
