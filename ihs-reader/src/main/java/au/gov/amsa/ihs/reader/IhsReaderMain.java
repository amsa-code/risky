package au.gov.amsa.ihs.reader;

import java.io.File;

public class IhsReaderMain {

    // private static final Logger log = Logger.getLogger(IhsReaderMain.class);

    private static final String GROSS_TONNAGE = "GrossTonnage";

    public static void main(String[] args) {
        File file = new File("/media/an/ship-data/ihs/608750-2015-04-01.zip");
        // IhsReader.fromZip(file).filter(map -> map.get(GROSS_TONNAGE) ==
        // null).count()
        // .doOnNext(System.out::println).subscribe();
        IhsReader.fromZip(file) //
                .groupBy(map -> {
                    String name = map.get("FlagName");
                    if (name == null)
                        name = "UNKNOWN";
                    return name;
                }) //
                .flatMap(g -> g //
                        .groupBy(map -> map.containsKey(GROSS_TONNAGE)) //
                        .flatMap(g2 -> g2.reduce(0L,
                                (total, map) -> map.get(GROSS_TONNAGE) != null
                                        ? total + Long.parseLong(map.get(GROSS_TONNAGE)) : total)
                                .map(x -> g.getKey() + "\t" + x)))
                .doOnNext(System.out::println) //
                .subscribe();

        IhsReader.fromZip(file) //
                .groupBy(map -> {
                    String name = map.get("FlagName");
                    if (name == null)
                        name = "UNKNOWN";
                    return name;
                }) //
                .flatMap(
                        g -> g //
                                .groupBy(map -> map.containsKey(GROSS_TONNAGE)) //
                                .flatMap(g2 -> g2
                                        .reduce(0L,
                                                (total, map) -> map.get(GROSS_TONNAGE) != null
                                                        ? total + 1 : total)
                                .map(x -> g.getKey() + "\t" + x)))
                .doOnNext(System.out::println) //
                .subscribe();
        // log.info(IhsReader.fromZip(file).count().toBlocking().single() +
        // " ships");
        // log.info(IhsReader.fromZip(file).filter(ship ->
        // ship.getMmsi().isPresent()).count()
        // .toBlocking().single()
        // + " ships");
        // Map<String, Ship> map = IhsReader.fromZipAsMapByMmsi(file).map(x ->
        // IhsReader.toShip(x))
        // .toBlocking().single();
        // System.out.println(map.get("503595000"));
    }
}
