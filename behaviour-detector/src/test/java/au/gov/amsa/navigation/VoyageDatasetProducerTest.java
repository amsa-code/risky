package au.gov.amsa.navigation;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.navigation.VoyageDatasetProducer.EezWaypoint;
import au.gov.amsa.navigation.VoyageDatasetProducer.Port;
import au.gov.amsa.navigation.VoyageDatasetProducer.TimedWaypoint;
import au.gov.amsa.risky.format.Fix;
import rx.Observable;

public class VoyageDatasetProducerTest {

    @Test
    public void test() {
        long aTime = 1498199825552L;
        long bTime = 1498199825552L + TimeUnit.DAYS.toMillis(2);
        Shapefile eezLine = VoyageDatasetProducer.loadEezLine();
        Shapefile eezPolygon = VoyageDatasetProducer.loadEezPolygon();
        assertTrue(eezPolygon.contains(-35, 149));
        Fix a = Mockito.mock(Fix.class);
        when(a.lat()).thenReturn(-35f);
        when(a.lon()).thenReturn(149f);
        when(a.time()).thenReturn(aTime);
        Fix b = Mockito.mock(Fix.class);
        when(b.lat()).thenReturn(-35f);
        when(b.lon()).thenReturn(175f);
        when(b.time()).thenReturn(bTime);
        Observable<Fix> fixes = Observable.just(a, b);
        Collection<Port> ports = Collections.emptyList();
        Collection<EezWaypoint> eezWaypoints = Collections
                .singleton(new EezWaypoint("test", -32.0, 151.0, Optional.empty()));
        List<List<TimedWaypoint>> list = VoyageDatasetProducer.toWaypoints(eezLine, eezPolygon, ports, eezWaypoints, fixes) //
                .toList() //
                .toBlocking() //
                .single();
        System.out.println(list);
    }
}
