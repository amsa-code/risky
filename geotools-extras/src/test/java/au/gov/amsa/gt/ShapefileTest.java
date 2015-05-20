package au.gov.amsa.gt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

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

}
