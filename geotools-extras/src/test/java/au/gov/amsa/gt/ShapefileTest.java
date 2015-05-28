package au.gov.amsa.gt;

import static java.lang.Double.parseDouble;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.junit.Test;

import au.gov.amsa.streams.Strings;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;

public class ShapefileTest {

	@Test
	public void testLoadShapefileFromFileSystem() {
		Shapefile shape = Shapefile.from(new File(
				"src/test/resources/shapefile-srr-polygon/srr.shp"));
		assertTrue(shape.contains(-20, 135));
		assertFalse(shape.contains(0, 0));
		assertEquals(4, shape.geometries().size());
		shape.close();

	}

	@Test
	public void testLoadShapefileFromZippedInputStream() throws Exception {
		Shapefile shape = Shapefile.fromZip(Shapefile.class
				.getResourceAsStream("/shapefile-srr-polygon.zip"));
		assertFalse(shape.contains(0, 0));
		assertEquals(4, shape.geometries().size());
		shape.close();
	}

	@Test
	public void testCreatePolygonShapefile() throws IOException {
		try (InputStreamReader r = new InputStreamReader(
				Shapefile.class.getResourceAsStream("/southwest.txt"))) {
			// read coordinates from classpath
			List<Coordinate> coords = Strings
					.lines(r)
					.map(line -> line.trim())
					.filter(line -> line.length() > 0)
					.filter(line -> line.charAt(0) != '#')
					.doOnNext(System.out::println)
					.map(line -> line.split(" "))
					.map(items -> new Coordinate(parseDouble(items[0]),
							parseDouble(items[1]))).toList().map(list -> {
						List<Coordinate> list2 = Lists.newArrayList(list);
						list2.add(list2.get(0));
						return list2;
					}).toBlocking().single();
			ShapefileCreator.createPolygon(coords, new File(
					"target/shapefile-test.shp"));
		}
	}

}
