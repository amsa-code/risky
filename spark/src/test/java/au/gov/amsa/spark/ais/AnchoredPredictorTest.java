package au.gov.amsa.spark.ais;

import static org.junit.Assert.assertEquals;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.junit.Test;

import au.gov.amsa.spark.ais.AnchoredPredictor.Status;

public class AnchoredPredictorTest {

    @Test
    public void testLoad() {
        SparkConf sparkConf = new SparkConf().setAppName("AnchoragePredictor");
        // just run this locally
        sparkConf.setMaster("local[" + Runtime.getRuntime().availableProcessors() + "]");
        JavaSparkContext sc = new JavaSparkContext(sparkConf);
        AnchoredPredictor predictor = new AnchoredPredictor(sc);
        // can do 10 million of these a second!
        Status status = predictor.predict(-10, 145, 10, 0, 8.0, 10, 8.0, 7);
        assertEquals(Status.OTHER, status);
    }

}
