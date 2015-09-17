package au.gov.amsa.animator;

import java.util.Collection;
import java.util.Map;

import au.gov.amsa.risky.format.Fix;

public interface Model {

    void updateModel(long timeStep);

    /**
     * Return recent fixes in ascending time order keyed by mmsi.
     * 
     * @return
     */
    Map<Integer, Collection<Fix>> recent();

    long stepNumber();

}