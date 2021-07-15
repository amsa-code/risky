package au.gov.amsa.gt;

import java.util.Collection;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;

public final class GeometryUtil {

    public static boolean contains(GeometryFactory gf, Collection<PreparedGeometry> geometries,
            double lat, double lon) {
        Point point = gf.createPoint(new Coordinate(lon, lat));
        for (PreparedGeometry g : geometries) {
            if (g.contains(point))
                return true;
        }
        return false;
    }
}