package au.gov.amsa.spark.ais;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.linalg.DenseVector;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;

public class AnchoredPredictor {

    private DecisionTreeModel model;

    public AnchoredPredictor(JavaSparkContext sc) {
        String dataPath = AnchoredPredictor.class.getResource("/anchoredOrMooredModel").toString();
        model = DecisionTreeModel.load(sc.sc(), dataPath);
    }

    public static enum Status {
        OTHER, MOORED, ANCHORED;
    }

    public Status predict(double lat, double lon, double speedKnots, double courseMinusHeading,
            double preEffectiveSpeedKnots, double preError, double postEffectiveSpeedKnots,
            double postError) {
        Vector features = new DenseVector(new double[] { lat, lon, speedKnots, courseMinusHeading,
                preEffectiveSpeedKnots, preError, postEffectiveSpeedKnots, postError });
        double prediction = model.predict(features);

        if (is(prediction, 1))
            return Status.MOORED;
        else if (is(prediction, 2))
            return Status.ANCHORED;
        else
            return Status.OTHER;
    }

    private static boolean is(double a, double b) {
        return Math.abs(a - b) < 0.0001;
    }
}
