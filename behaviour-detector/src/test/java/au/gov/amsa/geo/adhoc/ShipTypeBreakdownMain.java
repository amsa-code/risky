package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.rx.slf4j.Logging;
import com.google.common.base.Optional;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.ShipTypeDecoder;
import au.gov.amsa.ais.Timestamped;
import au.gov.amsa.ais.message.AisPosition;
import au.gov.amsa.ais.message.AisPositionA;
import au.gov.amsa.ais.message.AisShipStatic;
import au.gov.amsa.ais.message.AisShipStaticA;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.ais.rx.Streams.TimestampedAndLine;
import rx.Observable;
import rx.functions.Func1;

public class ShipTypeBreakdownMain {

    private static Logger log = LoggerFactory.getLogger(ShipTypeBreakdownMain.class);

    public static void main(String[] args) throws FileNotFoundException, IOException {

        // load a shapefile

        final GeometryFactory gf = new GeometryFactory();
        File file = new File("/home/dxm/temp/srr.shp");
        Map<String, Serializable> map = new HashMap<>();
        map.put("url", file.toURI().toURL());
        DataStore datastore = DataStoreFinder.getDataStore(map);
        String typeName = datastore.getTypeNames()[0];
        System.out.println(typeName);
        SimpleFeatureSource source = datastore.getFeatureSource(typeName);
        final SimpleFeatureCollection features = source.getFeatures();
        final List<PreparedGeometry> geometries = new ArrayList<>();

        SimpleFeatureIterator it = features.features();
        while (it.hasNext()) {
            SimpleFeature feature = it.next();
            Geometry g = (Geometry) feature.getDefaultGeometry();
            geometries.add(PreparedGeometryFactory.prepare(g));
        }

        // System.exit(0);

        String filename = "/media/analysis/nmea/2014/NMEA_ITU_20140815.gz";
        final Set<Long> mmsi = new HashSet<Long>();
        final Set<Long> mmsiA = new HashSet<Long>();
        final Set<Long> mmsiB = new HashSet<Long>();

        Streams.extract(Streams.nmeaFromGzip(filename)).flatMap(aisPositionsOnly).lift(
                Logging.<TimestampedAndLine<AisPosition>> logger().showCount().every(100000).log())
                .doOnNext(m -> {
                    AisPosition p = m.getMessage().get().message();
                    if (p.getLatitude() != null && p.getLongitude() != null
                            && contains(gf, geometries, p.getLatitude(), p.getLongitude())) {
                        long mmsiNo = m.getMessage().get().message().getMmsi();
                        mmsi.add(mmsiNo);
                        if (m.getMessage().get().message() instanceof AisPositionA)
                            mmsiA.add(mmsiNo);
                        else
                            mmsiB.add(mmsiNo);
                    }
                }).subscribe();

        final Map<ShipTypeClass, Set<Integer>> countsByShipType = new ConcurrentHashMap<>();

        Streams.extract(Streams.nmeaFromGzip(filename)).flatMap(aisShipStaticOnly).doOnNext(m -> {
            AisShipStatic s = m.getMessage().get().message();
            if (mmsi.contains(s.getMmsi())) {
                boolean isClassA = s instanceof AisShipStaticA;
                ShipTypeClass shipTypeClass = new ShipTypeClass(isClassA, s.getShipType());
                if (countsByShipType.get(shipTypeClass) == null)
                    countsByShipType.put(shipTypeClass, new HashSet<Integer>());
                else
                    countsByShipType.get(shipTypeClass).add(s.getMmsi());
            }
        }).subscribe();

        System.out.println(countsByShipType);
        Set<String> set = new TreeSet<String>();
        int sizeA = 0;
        int sizeB = 0;
        for (Entry<ShipTypeClass, Set<Integer>> s : countsByShipType.entrySet()) {
            set.add(ShipTypeDecoder.getShipType(s.getKey().shipType) + "\t"
                    + (s.getKey().isClassA ? "A" : "B") + "\t" + s.getValue().size());
            if (s.getKey().isClassA)
                sizeA += s.getValue().size();
            else
                sizeB += s.getValue().size();
        }
        set.stream().forEach(System.out::println);
        System.out.println("Unknown\tA\t" + (mmsiA.size() - sizeA));
        System.out.println("Unknown\tB\t" + (mmsiB.size() - sizeB));
        log.info("finished");
    }

    private static Func1<TimestampedAndLine<AisMessage>, Observable<TimestampedAndLine<AisPosition>>> aisPositionsOnly = m -> {
        Optional<Timestamped<AisMessage>> message = m.getMessage();
        if (message.isPresent() && message.get().message() instanceof AisPosition) {
            @SuppressWarnings("unchecked")
            TimestampedAndLine<AisPosition> m2 = (TimestampedAndLine<AisPosition>) (TimestampedAndLine<?>) m;
            return Observable.just(m2);
        } else
            return Observable.empty();
    };

    private static Func1<TimestampedAndLine<AisMessage>, Observable<TimestampedAndLine<AisShipStatic>>> aisShipStaticOnly = m -> {
        Optional<Timestamped<AisMessage>> message = m.getMessage();
        if (message.isPresent() && message.get().message() instanceof AisShipStatic) {
            @SuppressWarnings("unchecked")
            TimestampedAndLine<AisShipStatic> m2 = (TimestampedAndLine<AisShipStatic>) (TimestampedAndLine<?>) m;
            return Observable.just(m2);
        } else
            return Observable.empty();
    };

    private static class ShipTypeClass {
        boolean isClassA;

        @Override
        public String toString() {
            return "ShipTypeClass [class=" + (isClassA ? "A" : "B") + ", shipType=" + shipType
                    + "]";
        }

        int shipType;

        ShipTypeClass(boolean isClassA, int shipType) {
            super();
            this.isClassA = isClassA;
            this.shipType = shipType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (isClassA ? 1231 : 1237);
            result = prime * result + shipType;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ShipTypeClass other = (ShipTypeClass) obj;
            if (isClassA != other.isClassA)
                return false;
            if (shipType != other.shipType)
                return false;
            return true;
        }
    }

    private static boolean contains(GeometryFactory gf, Collection<PreparedGeometry> geometries,
            double lat, double lon) {
        for (PreparedGeometry g : geometries) {
            if (g.contains(gf.createPoint(new Coordinate(lon, lat))))
                return true;
        }
        return false;
    }

}
