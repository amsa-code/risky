package au.gov.amsa.animator;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import rx.Observable;

public class ModelEmpty implements Model {

    volatile long timeStep = 0;

    public ModelEmpty() {
    }

    @Override
    public void updateModel(long timeStep) {
        this.timeStep = timeStep;
    }

    @Override
    public Map<Integer, Collection<Fix>> recent() {
        return Collections.emptyMap();
    }

    @Override
    public long stepNumber() {
        return timeStep;
    }

    public static void main(String[] args) {
        File file = new File("/media/an/binary-fixes-5-minute/2014/565187000.track");
        Observable<Fix> source = BinaryFixes.from(file, true);
        source.subscribe(System.out::println);
    }

}
