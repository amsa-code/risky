package au.gov.amsa.geo.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.FixImpl;

public class CellTest {

    private static final Fix f1 = new FixImpl(1, -35f, 142.0f, 0, AisClass.A);
    private static final double PRECISION = 0.0000001;

    private static final Options options = Options.builder().originLat(0).originLon(0)
            .cellSizeDegrees(1.0).bounds(new Bounds(0, 100, -60, 175)).build();

    @Test
    public void testLeftEdgeLongitude() {

        Cell cell = Cell.cellAt(f1, options).get();
        assertEquals(f1.lon(), cell.leftEdgeLongitude(options), PRECISION);
    }

    @Test
    public void testTopEdgeLatitude() {
        Cell cell = Cell.cellAt(f1, options).get();
        assertEquals(f1.lat(), cell.topEdgeLatitude(options), PRECISION);

    }

    @Test
    public void testRightEdgeLongitude() {
        Cell cell = Cell.cellAt(f1, options).get();
        assertEquals(f1.lon() + options.getCellSizeDegreesAsDouble(),
                cell.rightEdgeLongitude(options), PRECISION);

    }

    @Test
    public void testBottomEdgeLatitude() {
        Cell cell = Cell.cellAt(f1, options).get();
        assertEquals(f1.lat() - options.getCellSizeDegreesAsDouble(),
                cell.bottomEdgeLatitude(options), PRECISION);
    }

    @Test
    public void testPositionAtLeftEdgeShouldNotResolveToTheCellToItsLeft() {
        // Note that 142.1/0.1 =
        Options options = Options.builder().originLat(0).originLon(0).cellSizeDegrees(0.1)
                .bounds(new Bounds(0, 100, -60, 175)).build();
        Cell cell = Cell.cellAt(0, 142.1001, options).get();
        Cell cell2 = Cell.cellAt(0, cell.leftEdgeLongitude(options), options).get();
        assertEquals(cell, cell2);
        assertEquals(142.1, cell.leftEdgeLongitude(options), PRECISION);
    }

    @Test
    public void testAreaInNauticalMilesOfOneDegreeCellNearEquator() {
        Options options = Options.builder().originLat(0).originLon(0).cellSizeDegrees(1.0)
                .bounds(new Bounds(0, 100, -60, 175)).build();
        Cell cell = Cell.cellAt(0, 142, options).get();
        assertEquals(3604.6847966219016, cell.areaNauticalMiles(options), PRECISION);
    }

    @Test
    public void testAreaInNauticalMilesOfOneDegreeCellAt45South() {
        Options options = Options.builder().originLat(0).originLon(0).cellSizeDegrees(1.0)
                .bounds(new Bounds(0, 100, -60, 175)).build();
        Cell cell = Cell.cellAt(-45, 142, options).get();
        assertEquals(2526.6531760439616, cell.areaNauticalMiles(options), PRECISION);
    }
}
