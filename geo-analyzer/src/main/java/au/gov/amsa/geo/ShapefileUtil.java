package au.gov.amsa.geo;

import com.github.davidmoten.grumpy.core.Position;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.prep.PreparedGeometry;

import au.gov.amsa.gt.Shapefile;
import au.gov.amsa.risky.format.Fix;

public class ShapefileUtil {
    
    public static TimedPosition findRegionCrossingPoint(Shapefile region, Fix fix1, Fix fix2) {

        Coordinate[] coords = new Coordinate[] { new Coordinate(fix1.lon(), fix1.lat()),
                new Coordinate(fix2.lon(), fix2.lat()) };
        LineString line = new GeometryFactory().createLineString(coords);
        for (PreparedGeometry g : region.geometries()) {
            if (g.crosses(line)) {
                Geometry intersection = g.getGeometry().intersection(line);
                // expecting just one point
                Coordinate coord = intersection.getCoordinate();
                double longitude = coord.x;
                double latitude = coord.y;
                Position a = Position.create(fix1.lat(), fix1.lon());
                Position b = Position.create(fix2.lat(), fix2.lon());
                Position c = Position.create(latitude, longitude);
                double ac = a.getDistanceToKm(c);
                double bc = b.getDistanceToKm(c);
                if (ac == 0) {
                    return new TimedPosition(fix1.lat(), fix1.lon(), fix1.time());
                } else if (bc == 0) {
                    return new TimedPosition(fix2.lat(), fix2.lon(), fix2.time());
                } else {
                    // predict the timestamp based on distance from a and b
                    long diff = fix2.time() - fix1.time();
                    long t = Math.round(fix1.time() + ac * diff / (ac + bc));
                    return new TimedPosition(latitude, longitude, t);
                }
            }
        }
        throw new RuntimeException("crossing not found");
    }

}
