package au.gov.amsa.animator;

import java.util.List;
import java.util.Map;

import au.gov.amsa.risky.format.Fix;

public interface Model {

    void updateModel(long timeStep);

    /**
     * Return recent fixes in ascending time order keyed by mmsi.
     * 
     * @return
     */
    Map<Long, List<Fix>> recent();

    long stepNumber();

}