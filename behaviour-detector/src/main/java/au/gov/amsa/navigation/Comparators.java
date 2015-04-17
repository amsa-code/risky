package au.gov.amsa.navigation;

import java.util.Comparator;

class Comparators {

    static final Comparator<VesselPosition> timeIdMessageIdComparator = (p1, p2) -> {
        int value = ((Long) p1.time()).compareTo(p2.time());
        if (value == 0) {
            value = ((Long) p1.id().uniqueId()).compareTo(p2.id().uniqueId());
            if (value == 0)
                return ((Long) p1.messageId()).compareTo(p2.messageId());
            else
                return value;
        } else
            return value;
    };

}
