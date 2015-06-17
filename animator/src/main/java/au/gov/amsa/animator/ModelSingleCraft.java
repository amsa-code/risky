package au.gov.amsa.animator;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import rx.Observable;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;

public class ModelSingleCraft implements Model {

    volatile long timeStep = 0;

    public ModelSingleCraft() {
    }

    @Override
    public void updateModel(long timeStep) {
        this.timeStep = timeStep;
    }

    @Override
    public Map<Long, Collection<Fix>> recent() {
        return Collections.emptyMap();
    }

    @Override
    public long stepNumber() {
        return timeStep;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        File file = new File("/media/an/binary-fixes-5-minute/2014/565187000.track");
        Observable<Fix> source = BinaryFixes.from(file, true);
        source.subscribe(System.out::println);
    }

}
