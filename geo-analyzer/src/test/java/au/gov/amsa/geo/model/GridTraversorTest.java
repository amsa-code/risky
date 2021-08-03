package au.gov.amsa.geo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

import au.gov.amsa.util.navigation.Position;

public class GridTraversorTest {

	private static final double PRECISION = 0.000001;

	/************************************************************************
	 * Test going from (-30.8,140.2) to (-31.7,142.6) on a single degree grid
	 * and back again!
	 ************************************************************************/

	@Test
	public void testGridNextPointOnBottomEdgeStartingInsideCell() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-30.8, 140.2);
		Position b = Position.create(-31.7, 142.6);
		assertEquals(114.303575, a.getBearingDegrees(b), PRECISION);
		Position p = grid.nextPoint(a, b);
		assertEquals(-31, p.getLat(), PRECISION);
		assertEquals(140.719351, p.getLon(), PRECISION);
	}

	@Test
	public void testGridNextPointOnRightEdgeStartingTopEdge() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-30.8, 140.2);
		Position b = Position.create(-31.7, 142.6);
		Position p = grid.nextPoint(a, b);
		Position p2 = grid.nextPoint(p, b);
		assertEquals(-31.106868, p2.getLat(), PRECISION);
		assertEquals(141.0, p2.getLon(), PRECISION);
	}

	@Test
	public void testGridNextPointOnRightEdgeStartingLeftEdge() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-30.8, 140.2);
		Position b = Position.create(-31.7, 142.6);
		Position p = grid.nextPoint(a, b);
		Position p2 = grid.nextPoint(p, b);
		Position p3 = grid.nextPoint(p2, b);
		assertEquals(-31.480781, p3.getLat(), PRECISION);
		assertEquals(142.0, p3.getLon(), PRECISION);
	}

	@Test
	public void testGridNextPointIsDestinationStartingLeftEdge() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-30.8, 140.2);
		Position b = Position.create(-31.7, 142.6);
		Position p = grid.nextPoint(a, b);
		Position p2 = grid.nextPoint(p, b);
		Position p3 = grid.nextPoint(p2, b);
		Position p4 = grid.nextPoint(p3, b);
		assertEquals(b.getLat(), p4.getLat(), PRECISION);
		assertEquals(b.getLon(), p4.getLon(), PRECISION);
	}

	/**********************************************
	 * Now go backwards!!
	 **********************************************/

	@Test
	public void testGridNextPointOnLeftEdgeStartingInside() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-31.7, 142.6);
		Position b = Position.create(-30.8, 140.2);
		Position p = grid.nextPoint(a, b);
		assertEquals(-31.480781, p.getLat(), PRECISION);
		assertEquals(142.0, p.getLon(), PRECISION);
	}

	@Test
	public void testGridNextPointOnLeftEdgeStartingRightEdge() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-31.7, 142.6);
		Position b = Position.create(-30.8, 140.2);
		Position p = grid.nextPoint(a, b);
		Position p2 = grid.nextPoint(p, b);
		assertEquals(-31.106868, p2.getLat(), PRECISION);
		assertEquals(141.0, p2.getLon(), PRECISION);
	}

	@Test
	public void testGridNextPointOnTopEdgeStartingRightEdge() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-31.7, 142.6);
		Position b = Position.create(-30.8, 140.2);
		Position p = grid.nextPoint(a, b);
		Position p2 = grid.nextPoint(p, b);
		Position p3 = grid.nextPoint(p2, b);
		assertEquals(-31, p3.getLat(), PRECISION);
		assertEquals(140.719351, p3.getLon(), PRECISION);
	}

	@Test
	public void testGridNextPointOnDestinationStartingBottomEdge() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-31.7, 142.6);
		Position b = Position.create(-30.8, 140.2);
		Position p = grid.nextPoint(a, b);
		Position p2 = grid.nextPoint(p, b);
		Position p3 = grid.nextPoint(p2, b);
		System.out.println("p3lat=" + p3.getLat());
		Position p4 = grid.nextPoint(p3, b);
		assertEquals(-30.8, p4.getLat(), PRECISION);
		assertEquals(140.2, p4.getLon(), PRECISION);
	}

	/**********************************************
	 * More tests
	 **********************************************/

	@Test
	public void testGridNextPointOnSameLatCanFindNextPointWithoutThrowingException() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-33.00055827109084, 172.0);
		// Position b = Position.create(-32.99916, 173.6);
		Position b = Position.create(-32.99169921875, 173.6);
		grid.nextPoint(a, b);
	}

	@Test
	public void testGridNextPointCloseToBottomEdge() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-41.9999999, 162.6359903399796);
		Position b = Position.create(-41.258392333984375, 127.83787536621094);
		Position p = grid.nextPoint(a, b);
		assertEquals(-42.0000001, p.getLat(), PRECISION);
		System.out.println("bearing=" + a.getBearingDegrees(b));
		Double lat = a.getLatitudeOnGreatCircle(b, 162.0);
		assertNotNull(lat);
	}

	@Test
	public void testGridNextPoint() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-43.999937317518835, 147.0000001);
		Position b = Position.create(-44.000099182128906, 147.17105102539062);
		Position p = grid.nextPoint(a, b);
		System.out.println("next=" + p);
		assertEquals(147.04147500160147, p.getLon(), PRECISION);
		assertEquals(-44.0, p.getLat(), PRECISION);
		System.out.println("bearing=" + a.getBearingDegrees(b));
		Double lat = a.getLatitudeOnGreatCircle(b, 162.0);
		assertNotNull(lat);
	}

	@Test
	public void testGridNextPoint2() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-1.0, 123.06508474558565);
		Position b = Position.create(-0.03392666578292847, 124.23638916015625);

		Position p = grid.nextPoint(a, b);
		System.out.println("next=" + p);
		assertEquals(-0.22893013277855734, p.getLat(), PRECISION);
		assertEquals(124.0, p.getLon(), PRECISION);
	}

	@Test
	public void testGridNextPointIsStraightNorth() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-28.119400024414062, 174.07943725585938);
		Position b = Position.create(-27.804515838623047, 174.07943725585938);
		Position p = grid.nextPoint(a, b);
		System.out.println("next=" + p);
		assertEquals(174.07943725585938, p.getLon(), PRECISION);
		assertEquals(-28.0, p.getLat(), PRECISION);
	}

	@Test
	public void testGridNextPoint3() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-41.0, 145.85554504394528);
		Position b = Position.create(-41.0, 145.8555450439453);

		Position p = grid.nextPoint(a, b);
		System.out.println("next=" + p);
		assertEquals(-41.0, p.getLat(), PRECISION);
		assertEquals(145.8555450439453, p.getLon(), 0.00000000000001);

	}

	@Test
	public void testGridNextPoint4() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-25.000009536743164, 154.06651306152344);
		Position b = Position.create(-24.990570068359375, 154.0664520263672);

		Position p = grid.nextPoint(a, b);
		System.out.println("next=" + p);
		assertEquals(-25.0, p.getLat(), PRECISION);
		assertEquals(154.06651299985458, p.getLon(), PRECISION);
	}

	@Test
	public void testGridNextPoint5() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-31.0, 153.233360669649);
		Position b = Position.create(-31.691329956054688, 152.9971160888672);

		Position p = grid.nextPoint(a, b);
		System.out.println("next=" + p);
		assertEquals(-31.68295452254073, p.getLat(), PRECISION);
		assertEquals(153.0, p.getLon(), PRECISION);
	}

	@Test
	@Ignore
	// TODO works on Java 8 not Java 11 (precision differences?)
	public void testGridNextPoint6() {
		GridTraversor grid = createGrid();
		Position a = Position.create(-33.0, 151.89463780840157);
		Position b = Position.create(-33.001766204833984, 152.011962890625);

		Position p = grid.nextPoint(a, b);
		System.out.println("next=" + p);
		assertEquals(152.0, p.getLon(), PRECISION);
		assertEquals(-33.001591143678596, p.getLat(), PRECISION);
	}

	@Test
	public void testGridNextPoint7() {
		GridTraversor grid = new GridTraversor(Options.builder()
				.bounds(new Bounds(15.0, 67, -60, 179.0)).originLat(0)
				.originLon(0).cellSizeDegrees(0.2).build());
		Position a = Position.create(-38.0, 138.47666931152344);
		Position b = Position.create(-38.04766845703125, 138.7101593017578);

		Position p = grid.nextPoint(a, b);
		System.out.println("next=" + p);
		assertEquals(138.6, p.getLon(), PRECISION);
		assertEquals(-38.02524399767118, p.getLat(), PRECISION);
	}

	/**********************************************
	 * Test utilities
	 **********************************************/
	private static GridTraversor createGrid() {
		return new GridTraversor(createOptions());
	}

	private static Options.Builder createOptionsBuilder() {
		return Options.builder()
		// setup origin
				.originLat(0).originLon(0)
				// set cell size
				.cellSizeDegrees(1.0)
				// set bounds
				.bounds(new Bounds(0, 100, -60, 175));
	}

	private static Options createOptions() {
		return createOptionsBuilder().build();
	}

}
