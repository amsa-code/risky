package au.gov.amsa.geo.model;

import java.util.Comparator;

public class PositionComparator implements Comparator<Position> {

	@Override
	public int compare(Position p1, Position p2) {
		int value = ((Double) p1.getLat()).compareTo(p2.getLat());
		if (value == 0)
			return ((Double) p1.getLon()).compareTo(p2.getLon());
		return value;
	}

}
