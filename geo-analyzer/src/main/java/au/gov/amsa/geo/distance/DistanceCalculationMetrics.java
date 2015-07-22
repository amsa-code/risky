package au.gov.amsa.geo.distance;

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.AtomicDouble;

public class DistanceCalculationMetrics {
    AtomicLong fixesPassedEffectiveSpeedCheck = new AtomicLong(0);
    AtomicLong fixes = new AtomicLong(0);
    AtomicLong fixesInTimeRange = new AtomicLong();
    AtomicLong fixesWithinRegion = new AtomicLong(0);
    AtomicLong segments = new AtomicLong(0);
    AtomicLong segmentsTimeDifferenceOk = new AtomicLong(0);
    AtomicLong segmentsDistanceOk = new AtomicLong(0);
    AtomicDouble totalNauticalMiles = new AtomicDouble(0);
    AtomicLong segmentCells = new AtomicLong(0);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Metrics [fixes=");
        builder.append(fixes);
        builder.append(", fixesInTimeRange=");
        builder.append(fixesInTimeRange);
        builder.append(", fixesWithinRegion=");
        builder.append(fixesWithinRegion);
        builder.append(", fixesEffectiveSpeedOk=");
        builder.append(fixesPassedEffectiveSpeedCheck.get());
        builder.append(", segments=");
        builder.append(segments);
        builder.append(", segmentsTimeDifferenceOk=");
        builder.append(segmentsTimeDifferenceOk);
        builder.append(", segmentsDistanceOk=");
        builder.append(segmentsDistanceOk);
        builder.append(", segmentCells=");
        builder.append(segmentCells);
        builder.append(", totalNauticalMiles=");
        builder.append(totalNauticalMiles);
        builder.append("]");
        return builder.toString();
    }

}