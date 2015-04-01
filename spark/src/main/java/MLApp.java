import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.DecisionTree;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.mllib.util.MLUtils;

public class MLApp {

    public static void main(String[] args) throws IOException {

        SparkConf sparkConf = new SparkConf().setAppName("JavaDecisionTree");
        // just run this locally
        sparkConf.setMaster("local[6]");
        JavaSparkContext sc = new JavaSparkContext(sparkConf);

        // Load and parse the data file.
        String datapath = "/media/an/fixes.libsvm";

        // the feature names are substituted into the model debugString later to
        // make it readable
        List<String> names = Arrays.asList("lat", "lon", "speedKnots", "courseHeadingDiff",
                "preEffectiveSpeedKnots", "preError", "postEffectiveSpeedKnots", "postError");

        JavaRDD<LabeledPoint> data = MLUtils.loadLibSVMFile(sc.sc(), datapath).toJavaRDD();
        // Split the data into training and test sets (30% held out for testing)
        JavaRDD<LabeledPoint>[] splits = data.randomSplit(new double[] { 0.7, 0.3 });
        JavaRDD<LabeledPoint> trainingData = splits[0];
        JavaRDD<LabeledPoint> testData = splits[1];

        // Set parameters.
        // Empty categoricalFeaturesInfo indicates all features are continuous.
        Integer numClassifications = 3;
        Map<Integer, Integer> categoricalFeaturesInfo = new HashMap<Integer, Integer>();
        String impurity = "gini";
        Integer maxDepth = 6;
        Integer maxBins = 32;

        // Train a DecisionTree model for classification.
        final DecisionTreeModel model = DecisionTree.trainClassifier(trainingData,
                numClassifications, categoricalFeaturesInfo, impurity, maxDepth, maxBins);

        // Evaluate model on test instances and compute test error
        Double testErr = (double) testData
        // pair up actual and predicted classification numerical representation
                .map(toPredictionAndActual(model))
                // get the ones that don't match
                .filter(predictionWrong())
                // count them
                .count()
        // divide by total count to get ratio failing test
                / testData.count();

        System.out.println("Test Error: " + testErr);
        System.out.println("Learned classification tree model:\n"
                + useNames(model.toDebugString(), names));

        // Save and load model to demo possible usage in prediction mode
        String modelPath = "target/myModelPath";
        FileUtils.deleteDirectory(new File(modelPath));
        model.save(sc.sc(), modelPath);
        DecisionTreeModel sameModel = DecisionTreeModel.load(sc.sc(), modelPath);

    }

    private static String useNames(String s, List<String> names) {
        String result = s;
        for (int i = names.size() - 1; i >= 0; i--) {
            result = result.replace("feature " + i, names.get(i));
        }
        return result;
    }

    private static Function<PredictionAndActual, Boolean> predictionWrong() {
        return new Function<PredictionAndActual, Boolean>() {
            @Override
            public Boolean call(PredictionAndActual p) {
                return p.prediction != p.actual;
            }
        };
    }

    private static Function<LabeledPoint, PredictionAndActual> toPredictionAndActual(
            final DecisionTreeModel model) {
        return new Function<LabeledPoint, PredictionAndActual>() {
            @Override
            public PredictionAndActual call(LabeledPoint p) {
                return new PredictionAndActual(model.predict(p.features()), p.label());
            }
        };
    }

    private static class PredictionAndActual {
        final double prediction;
        final double actual;

        PredictionAndActual(double prediction, double actual) {
            this.prediction = prediction;
            this.actual = actual;
        }

    }
}