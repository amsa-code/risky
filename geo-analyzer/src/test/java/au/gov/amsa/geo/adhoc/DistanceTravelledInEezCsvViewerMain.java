package au.gov.amsa.geo.adhoc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import au.gov.amsa.ais.rx.Streams;

public class DistanceTravelledInEezCsvViewerMain {

    public static void main(String[] args) throws FileNotFoundException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        Streams.nmeaFrom(new FileInputStream("target/output.csv")) //
                // .doOnNext(System.out::println)
                // .take(3) //
                .skip(1) //
                .filter(line -> line.trim().length() > 0) //
                .map(x -> x.split(",")) //
                .filter(x -> x[2].equals("A")) //
                .groupBy(x -> x[1])
                .flatMap(o -> o.reduce(0.0, (sum, x) -> sum + Double.parseDouble(x[4]))
                        .map(x -> new VesselDistance(o.getKey(), x)))//
                // .filter(x -> speedKnots(x) > 30) //
                // .doOnNext(x -> mmsis.add(x[1])) //
                // .map(x -> Arrays.toString(x) + ": avg speed in knots = " + speedKnots(x)) //
                .sorted((a, b) -> Double.compare(a.distanceKm, b.distanceKm)) //
                .doOnNext(System.out::println) //
                .toList() //
                .doOnNext(list -> System.out.println("median = " + list.get(list.size() / 2)))//
                .doOnNext(list -> System.out.println("average = " + average(list.stream().map(x -> x.distanceKm).collect(Collectors.toList())))) //
                .toBlocking() //
                .subscribe();
        System.out.println("finished");
    }

    
    private static double average(List<Double> list) {
        double sum = 0;
        for (double d: list) {
            sum += d;
        }
        return sum/list.size();
    }
    
    private static double speedKnots(String[] x) {
        return Double.parseDouble(x[4]) / Double.parseDouble(x[5]);
    }

    static final class VesselDistance {
        final String mmsi;
        final double distanceKm;

        VesselDistance(String mmsi, double distanceKm) {
            this.mmsi = mmsi;
            this.distanceKm = distanceKm;
        }

        @Override
        public String toString() {
            return mmsi + ": " + Math.round(distanceKm) + "km";
        }
    }

}
