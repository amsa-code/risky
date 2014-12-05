package au.gov.amsa.geo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class GridTest {

	private static final Options.Builder builder = Options.builder()
			.originLat(0).originLon(0).cellSizeDegrees(0.1)
			.bounds(new Bounds(0, 100, -60, 175))
			.filterBounds(new Bounds(0, 100, -60, 175));
	private static final Options options = builder.build();
	private static final double PRECISION = 0.0000000001;

	@Test
	public void testCellAtIfLatAboveBoundsThrowsException() {
		Grid grid = new Grid(options);
		assertFalse(grid.cellAt(1, 1).isPresent());
	}

	@Test
	public void testCellAtIfLatBelowBoundsThrowsException() {
		Grid grid = new Grid(options);
		assertFalse(grid.cellAt(-61.0, 1).isPresent());
	}

	@Test
	public void testCellAtIfLonLeftOfBoundsReturnsAbsent() {
		Grid grid = new Grid(options);
		assertFalse(grid.cellAt(-1, -59).isPresent());
	}

	@Test
	public void testCellAtIfLonRightOfBoundsThrowsException() {
		Grid grid = new Grid(options);
		assertFalse(grid.cellAt(-1, 176).isPresent());
	}

	@Test
	public void testStartLatGivenOriginLatEqualsTopLeftLatOfBoundsReturnsOriginLat() {
		assertEquals(0, Grid.getStartLat(builder.originLat(0.0).build())
				.doubleValue(), PRECISION);
	}

	@Test
	public void testStartLatGivenOriginLatAboveTopLeftLatOfBoundsReturnsOriginLat() {
		assertEquals(1.0, Grid.getStartLat(builder.originLat(1.0).build())
				.doubleValue(), PRECISION);
	}

	@Test
	public void testStartLatGivenOriginLatBelowTopLeftLatOfBoundsReturnsOriginLatMinusEnoughToGetAboveBounds() {
		assertEquals(0.1, Grid.getStartLat(builder.originLat(-1.0).build())
				.doubleValue(), 0.000000000000000001);
	}

	@Test
	public void testStartLonGivenOriginLonEqualsTopLeftLonOfBoundsReturnsOriginLon() {
		assertEquals(100, Grid.getStartLon(builder.originLon(100).build())
				.doubleValue(), PRECISION);
	}

	@Test
	public void testStartLonGivenOriginLonLeftOfTopLeftLonOfBoundsReturnsOriginLon() {
		assertEquals(99.0, Grid.getStartLon(builder.originLon(99.0).build())
				.doubleValue(), PRECISION);
	}

	@Test
	public void testStartLonGivenOriginLonRightOfTopLeftLonOfBoundsReturnsOriginLonMinusEnoughToGetLeftOfBounds() {
		assertEquals(99.9, Grid.getStartLon(builder.originLon(101.0).build())
				.doubleValue(), 0.000000000000000001);
	}

	@Test
	public void testLeftEdgeLongitude() {
		Grid grid = new Grid(options);
		Cell cell = grid.cellAt(0, 100).get();
		System.out.println(cell);
		assertEquals(100.0, cell.leftEdgeLongitude(options), PRECISION);
	}

	@Test
	public void testGridGoingOver180Longitude() {
		Options o = options.buildFrom().bounds(new Bounds(0, 100, -60, 175))
				.filterBounds(new Bounds(0, 100, -60, -174)).build();
		Grid grid = new Grid(o);
		System.out.println(grid.rightEdgeLongitude(Cell.cellAt(-10, 100, o)
				.get()));
		System.out.println(grid.rightEdgeLongitude(Cell.cellAt(-10, 100, o)
				.get()));
	}
}
