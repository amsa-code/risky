package au.gov.amsa.geo.model;

import java.util.Comparator;

public class PositionComparator implements Comparator<Position> {

    @Override
    public int compare(Position p1, Position p2) {
        int value = Float.compare(p1.lat(), p2.lat());
        if (value == 0)
            return Float.compare(p1.lon(), p2.lon());
        return value;
    }

}
