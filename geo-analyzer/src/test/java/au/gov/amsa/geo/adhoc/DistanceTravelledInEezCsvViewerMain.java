package au.gov.amsa.geo.adhoc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import au.gov.amsa.ais.rx.Streams;

public final class DistanceTravelledInEezCsvViewerMain {

    public static void main(String[] args) throws FileNotFoundException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd");
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));

        {
            Streams.nmeaFrom(new FileInputStream("target/output.csv")) //
                    // .doOnNext(System.out::println)
                    // .take(3) //
                    .skip(1) //
                    .filter(line -> line.trim().length() > 0) //
                    .map(x -> x.split(",")) //
                    .groupBy(x -> x[0]) //
                    .flatMap(o -> o.count().map(y -> {
                        try {
                            return iso.format(sdf.parse(o.getKey())) + ": " + y;
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    })) //
                    .sorted() //
                    .doOnNext(System.out::println) //
                    .toBlocking() //
                    .subscribe();
        }
        {
            Streams.nmeaFrom(new FileInputStream("target/output.csv")) //
                    // .doOnNext(System.out::println)
                    // .take(3) //
                    .skip(1) //
                    .filter(line -> line.trim().length() > 0) //
                    .map(x -> x.split(",")) //
                    // .filter(x -> x[2].equals("B")) //
                    .groupBy(x -> x[1])
                    .flatMap(o -> o.reduce(0.0, (sum, x) -> sum + Double.parseDouble(x[4]))
                            .map(x -> new VesselDistance(o.getKey(), x)))//
                    // .filter(x -> speedKnots(x) > 30) //
                    // .doOnNext(x -> mmsis.add(x[1])) //
                    // .map(x -> Arrays.toString(x) + ": avg speed in knots = " + speedKnots(x)) //
                    .sorted((a, b) -> Double.compare(a.distanceNm, b.distanceNm)) //
                    .toList() //
                    .doOnNext(list -> System.out.println("number of vessels = " + list.size())) //
                    .doOnNext(list -> System.out.println("median = " + list.get(list.size() / 2)))//
                    .doOnNext(list -> System.out.println("averageKm = "
                            + Math.round(average(list.stream().map(x -> x.distanceNm).collect(Collectors.toList()))))) //
                    .doOnNext(list -> System.out.println(
                            "totalNmInEez = " + Math.round(list.stream().mapToDouble(x -> x.distanceNm).sum()))) //
                    .toBlocking() //
                    .subscribe();
        }
        {
            Streams.nmeaFrom(new FileInputStream("target/output.csv")) //
                    // .doOnNext(System.out::println)
                    // .take(3) //
                    .skip(1) //
                    .filter(line -> line.trim().length() > 0) //
                    .map(x -> x.split(",")) //
                    .filter(x -> x[2].equals("A")) //
                    .map(x -> Double.parseDouble(x[4])) //
                    .reduce(0.0, (sum, x) -> sum + x) //
                    .doOnNext(System.out::println) //
                    .toBlocking().subscribe();
        }
        System.out.println("finished");
    }

    private static double average(List<Double> list) {
        double sum = 0;
        for (double d : list) {
            sum += d;
        }
        return sum / list.size();
    }

    private static double speedKnots(String[] x) {
        return Double.parseDouble(x[4]) / Double.parseDouble(x[5]);
    }

    static final class VesselDistance {
        final String mmsi;
        final double distanceNm;

        VesselDistance(String mmsi, double distanceNm) {
            this.mmsi = mmsi;
            this.distanceNm = distanceNm;
        }

        @Override
        public String toString() {
            return mmsi + ": " + Math.round(distanceNm) + "km";
        }

    }

}
