package au.gov.amsa.gt;

import java.util.Collection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;

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