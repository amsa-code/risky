package au.gov.amsa.geo.model;

import org.apache.log4j.Logger;

import au.gov.amsa.util.navigation.Position;
import au.gov.amsa.util.navigation.Position.LongitudePair;

public class GridTraversor {

	private static Logger log = Logger.getLogger(GridTraversor.class);

	private static final double SMALL_INCREMENT_DEGREES = 0.0000001;

	private final Options options;

	public GridTraversor(Options options) {
		this.options = options;
	}

	private static double bearingDegrees(Position a, Position b) {
		double val = a.getBearingDegrees(b);
		if (val == 0 && a.getLat() == b.getLat()) {
			if (b.getLon() > a.getLon())
				val = 90;
			else if (b.getLon() < a.getLon())
				val = 270;
			else
				// same point
				val = 0;
		}
		return val;
	}

	public Position nextPoint(Position a, Position b) {
		if (a.equals(b))
			return b;
		double bearingDegrees = bearingDegrees(a, b);

		// if on left edge heading left or top edge heading up then nudge into
		// next cell
		// TODO is this required?
		Cell cell1 = Cell.cellAt(a.getLat(), a.getLon(), options).get();
		if (bearingDegrees > 180
				&& a.getLon() == cell1.leftEdgeLongitude(options)) {
			a = new Position(a.getLat(), a.getLon() - SMALL_INCREMENT_DEGREES);
			cell1 = Cell.cellAt(a.getLat(), a.getLon(), options).get();
			bearingDegrees = bearingDegrees(a, b);
		} else if ((bearingDegrees > 270 || bearingDegrees < 90)
				&& a.getLat() == cell1.topEdgeLatitude(options)) {
			a = new Position(a.getLat() + SMALL_INCREMENT_DEGREES, a.getLon());
			cell1 = Cell.cellAt(a.getLat(), a.getLon(), options).get();
			bearingDegrees = bearingDegrees(a, b);
		}

		Cell cell2 = Cell.cellAt(b.getLat(), b.getLon(), options).get();
		if (cell1.equals(cell2))
			return b;
		else {

			double targetLon = getTargetLon(cell1, bearingDegrees);

			// check if crosses target lat based on bearing
			double leftLon = cell1.leftEdgeLongitude(options);
			double rightLon = cell1.rightEdgeLongitude(options);
			double targetLat = getTargetLat(cell1, bearingDegrees);

			if (bearingDegrees == 0 || bearingDegrees == 180)
				return Position.create(targetLat, a.getLon());

			{
				Position result = nextPointCrossingLatitude(a, b, leftLon,
						rightLon, targetLat, bearingDegrees);
				if (result != null)
					return result;
			}
			{
				double otherLat = getNonTargetLat(cell1, bearingDegrees);
				Position result = nextPointCrossingLatitude(a, b, leftLon,
						rightLon, otherLat, bearingDegrees);
				if (result != null)
					return result;
			}

			// see if crosses left or right edge
			Double latCrossing = a.getLatitudeOnGreatCircle(b, targetLon);

			if (latCrossing != null) {
				double topLat = cell1.topEdgeLatitude(options);
				double bottomLat = cell1.bottomEdgeLatitude(options);
				if (topLat >= latCrossing && bottomLat <= latCrossing)
					return Position.create(latCrossing, targetLon);
			}

			log.warn("unexpected! Could not calculate next point for segment between\n a = "
					+ a + " b = " + b + "\noptions=" + options);
			return b;

		}
	}

	private double getNonTargetLat(Cell cell, double bearingDegrees) {
		if (bearingDegrees >= 270 || bearingDegrees < 90)
			return cell.bottomEdgeLatitude(options);
		else
			return cell.topEdgeLatitude(options);
	}

	private Position nextPointCrossingLatitude(Position a, Position b,
			double leftLon, double rightLon, double targetLat,
			double bearingDegrees) {
		LongitudePair lonCrossingCandidates = a.getLongitudeOnGreatCircle(b,
				targetLat);
		if (lonCrossingCandidates != null) {
			double lonCrossing;
			// choose candidate closest in longitude to point a along path to b
			boolean candidate1ok = leftLon <= lonCrossingCandidates.getLon1()
					&& lonCrossingCandidates.getLon1() <= rightLon
					&& !(lonCrossingCandidates.getLon1() == a.getLon() && targetLat == a
							.getLat());

			boolean candidate2ok = leftLon <= lonCrossingCandidates.getLon2()
					&& lonCrossingCandidates.getLon2() <= rightLon
					&& !(lonCrossingCandidates.getLon2() == a.getLon() && targetLat == a
							.getLat());
			if (candidate1ok && candidate2ok) {
				// choose the best of the candidates

				// no units used in calculations because only doing comparisons
				// so don't care if it's km or nautical miles
				double distance1ToB = new Position(targetLat,
						lonCrossingCandidates.getLon1()).getDistanceToKm(b);
				double distance2ToB = new Position(targetLat,
						lonCrossingCandidates.getLon2()).getDistanceToKm(b);
				double distanceAToB = a.getDistanceToKm(b);
				if (distance1ToB > distanceAToB)
					candidate1ok = false;
				if (distance2ToB > distanceAToB)
					candidate2ok = false;
				if (candidate1ok && candidate2ok) {
					if (distance1ToB < distance2ToB) {
						candidate1ok = false;
					} else
						candidate2ok = false;
				}
				if (candidate1ok)
					lonCrossing = lonCrossingCandidates.getLon1();
				else if (candidate2ok)
					lonCrossing = lonCrossingCandidates.getLon2();
				else
					// neither of the candidates are on the way to b!
					return null;
			} else if (candidate1ok) {
				lonCrossing = lonCrossingCandidates.getLon1();
			} else if (candidate2ok)
				lonCrossing = lonCrossingCandidates.getLon2();
			else
				return null;
			// check that lonCrossing is on the segment a to b
			double bearingDegreesTest = bearingDegrees(a,
					Position.create(targetLat, lonCrossing));
			double diff = Position.getBearingDifferenceDegrees(bearingDegrees,
					bearingDegreesTest);
			if (Math.abs(diff) > 90)
				return null;
			return Position.create(targetLat, lonCrossing);
		} else
			return null;
	}

	private double getTargetLon(Cell cell, double bearingDegrees) {
		if (bearingDegrees >= 0 && bearingDegrees < 180)
			return cell.rightEdgeLongitude(options);
		else
			return cell.leftEdgeLongitude(options);
	}

	private double getTargetLat(Cell cell, double bearingDegrees) {
		if (bearingDegrees >= 270 || bearingDegrees < 90)
			return cell.topEdgeLatitude(options);
		else
			return cell.bottomEdgeLatitude(options);
	}

}
