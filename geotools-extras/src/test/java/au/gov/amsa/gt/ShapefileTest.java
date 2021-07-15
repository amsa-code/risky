package au.gov.amsa.gt;

import static java.lang.Double.parseDouble;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.Test;

import com.google.common.collect.Lists;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import au.gov.amsa.streams.Strings;

public class ShapefileTest {

    @Test
    public void testLoadShapefileFromFileSystem() {
        Shapefile shape = Shapefile
                .from(new File("src/test/resources/shapefile-srr-polygon/srr.shp"));
        assertTrue(shape.contains(-20, 135));
        assertFalse(shape.contains(0, 0));
        assertEquals(4, shape.geometries().size());
        shape.close();

    }

    @Test
    public void testLoadShapefileFromZippedInputStream() throws Exception {
        Shapefile shape = Shapefile
                .fromZip(Shapefile.class.getResourceAsStream("/shapefile-srr-polygon.zip"));
        assertFalse(shape.contains(0, 0));
        assertEquals(4, shape.geometries().size());
        shape.close();
    }

    @Test
    public void testContainsInSrr() {
        // northern border is -12, 113
        Shapefile shape = Shapefile
                .fromZip(Shapefile.class.getResourceAsStream("/shapefile-srr-polygon.zip"));

        assertTrue(shape.contains(-13, 113));
        assertFalse(shape.contains(-10, 113));
    }

    @Test
    public void testShowDistanceBetweenPoints() {
        GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
        Point a = gf.createPoint(new Coordinate(-10, 114.0));
        Point b = gf.createPoint(new Coordinate(-11, 113.0));
        System.out.println("distance=" + a.distance(b));
    }

    @Test
    public void testContainsInSrrWithBuffer() {
        // northern border is -12, 113
        Shapefile shape = Shapefile
                .fromZip(Shapefile.class.getResourceAsStream("/shapefile-srr-polygon.zip"), 3);

        assertTrue(shape.contains(-13, 113));
        assertTrue(shape.contains(-10, 113));
        assertFalse(shape.contains(-8, 113));
    }

    @Test
    public void testCreatePolygonShapefile() throws IOException {
        try (InputStreamReader r = new InputStreamReader(
                Shapefile.class.getResourceAsStream("/southwest.txt"))) {
            // read coordinates from classpath
            List<Coordinate> coords = Strings.lines(r).map(line -> line.trim())
                    .filter(line -> line.length() > 0).filter(line -> line.charAt(0) != '#')
                    .doOnNext(System.out::println).map(line -> line.split(" "))
                    .map(items -> new Coordinate(parseDouble(items[0]), parseDouble(items[1])))
                    .toList().map(list -> {
                        List<Coordinate> list2 = Lists.newArrayList(list);
                        list2.add(new Coordinate(list.get(0).x, list.get(0).y));
                        return list2;
                    }).toBlocking().single();
            ShapefileCreator.createPolygon(coords, new File("target/southwest.shp"));
        }
    }

    @Test
    public void testToGeoJson() throws IOException {
        Shapefile shape = Shapefile
                .fromZip(Shapefile.class.getResourceAsStream("/shapefile-srr-polygon.zip"), 3);
        FileWriter writer = new FileWriter("target/srr.geojson");
        shape.writeGeoJson(writer, "EPSG:4326");
        writer.close();
    }

    @Test
    public void testMbr() throws IOException {
        Shapefile shape = Shapefile
                .fromZip(Shapefile.class.getResourceAsStream("/shapefile-srr-polygon.zip"));
        System.out.println(shape.mbr());
    }

    public static void main(String[] args) throws IOException {
        Shapefile shape = Shapefile
                .from(new File(System.getProperty("user.home") + "/temp/amb06_map_eez_pl.shp"));
        FileWriter writer = new FileWriter("target/geojson-eez.txt");
        shape.writeGeoJson(writer, "EPSG:3857");
        writer.close();
    }
}
