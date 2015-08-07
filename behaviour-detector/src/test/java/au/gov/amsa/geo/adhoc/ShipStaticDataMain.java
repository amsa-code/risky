package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.regex.Pattern;

import au.gov.amsa.navigation.ShipStaticDataCreator;
import au.gov.amsa.util.Files;

public class ShipStaticDataMain {

    public static void main(String[] args) throws FileNotFoundException {
        File outputFile = new File("target/out.txt");
        // String filename = "/media/an/nmea/2014/NMEA_ITU_20140815.gz";
        List<File> files = Files.find(new File("/media/an/nmea/2014/"),
                Pattern.compile("NMEA_ITU.*.gz"));

        ShipStaticDataCreator.writeStaticDataToFile(files, outputFile).count().toBlocking()
                .single();

    }

}
