package au.gov.amsa.ihs.reader;

import java.io.File;
import java.util.Map;

import au.gov.amsa.ihs.model.Ship;

public class IhsReaderMain {

    // private static final Logger log = Logger.getLogger(IhsReaderMain.class);

    public static void main(String[] args) {
        File file = new File("/media/an/ship-data/ihs/608750-2015-04-01.zip");
        // log.info(IhsReader.fromZip(file).count().toBlocking().single() +
        // " ships");
        // log.info(IhsReader.fromZip(file).filter(ship ->
        // ship.getMmsi().isPresent()).count()
        // .toBlocking().single()
        // + " ships");
        Map<String, Ship> map = IhsReader.fromZipAsMapByMmsi(file).toBlocking().single();
        System.out.println(map.get("503595000"));
    }
}
