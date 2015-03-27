import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

public class SimpleApp {
    public static void main(String[] args) {
        String logFile = "/var/log/syslog"; // Should be some file on your
                                            // system
        SparkConf conf = new SparkConf().setAppName("Simple Application");
        conf.setMaster("local");
        JavaSparkContext sc = new JavaSparkContext(conf);
        JavaRDD<String> logData = sc.textFile(logFile).cache();

        long numAs = logData.filter(new Function<String, Boolean>() {
            public Boolean call(String s) {
                return s.contains("a");
            }
        }).count();

        long numZs = logData.filter(new Function<String, Boolean>() {
            public Boolean call(String s) {
                return s.contains("z");
            }
        }).count();

        System.out.println("Lines with a: " + numAs + ", lines with z: " + numZs);
    }
}
