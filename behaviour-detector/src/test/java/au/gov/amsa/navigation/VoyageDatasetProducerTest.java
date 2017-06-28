package au.gov.amsa.navigation;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.navigation.VoyageDatasetProducer.EezWaypoint;
import au.gov.amsa.navigation.VoyageDatasetProducer.Port;
import au.gov.amsa.navigation.VoyageDatasetProducer.TimedLeg;
import au.gov.amsa.risky.format.Fix;
import rx.observers.AssertableSubscriber;
import rx.subjects.PublishSubject;

public class VoyageDatasetProducerTest {

    @Test
    public void test() throws IOException {
        long aTime = 1498199825552L;
        long bTime = aTime + TimeUnit.DAYS.toMillis(2);
        long cTime = bTime + TimeUnit.DAYS.toMillis(4);
        Shapefile eezLine = VoyageDatasetProducer.loadEezLine();
        Shapefile eezPolygon = VoyageDatasetProducer.loadEezPolygon();
        assertTrue(eezPolygon.contains(-35, 149));
        assertTrue(!eezPolygon.contains(-35, 175));

        // in eez
        Fix a = createOutOfEez(aTime);

        // out of eez
        Fix b = createInEez(bTime);

        PublishSubject<Fix> fixes = PublishSubject.create();
        Collection<Port> ports = VoyageDatasetProducer.loadPorts();
        Collection<EezWaypoint> eezWaypoints = Collections
                .singleton(new EezWaypoint("test", -32.0, 151.0, Optional.empty()));
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
        
        TimedLeg leg = ts.getOnNextEvents().get(0);
        System.out.println(leg);
        assertTrue(leg.a.waypoint instanceof EezWaypoint);
        assertTrue(leg.b.waypoint instanceof Port);

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
