package au.gov.amsa.spark.ais;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

public class SampleApp {
    public static void main(String[] args) {
        String logFile = "/var/log/syslog"; // Should be some file on your
                                            // system
        SparkConf conf = new SparkConf().setAppName("Simple Application");
        conf.setMaster("local");
        JavaSparkContext sc = new JavaSparkContext(conf);
        JavaRDD<String> logData = sc.textFile(logFile).cache();

        long numAs = logData.filter(s -> s.contains("a")).count();

        long numZs = logData.filter(s -> s.contains("z")).count();

        System.out.println("Lines with a: " + numAs + ", lines with z: " + numZs);
    }
}
