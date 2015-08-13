package au.gov.amsa.ihs.reader.adhoc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import au.gov.amsa.ihs.reader.IhsReader;
import au.gov.amsa.ihs.reader.Key;

public class SabineMain {

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("/media/an/ship-data/ihs/608750-2015-04-01.zip");

        List<Key> keys = Arrays.asList(Key.LRIMOShipNo, Key.ShipName, Key.ShipbuilderCompanyCode,
                Key.CountryOfBuildCode, Key.MainEngineBuilderCode, Key.MainEngineDesignerCode,
                Key.MainEngineModel, Key.MainEngineType, Key.PropulsionTypeCode, Key.PropulsionType,
                Key.DeathDate, Key.DeliveryDate, Key.DateOfBuild, Key.Deadweight, Key.GrossTonnage,
                Key.LengthOverallLOA, Key.LiquidCapacity, Key.TEU, Key.PassengerCapacity,
                Key.StatCode5, Key.ShipStatus, Key.ShipStatusCode, Key.ShipStatusEffectiveDate,
                Key.FlagCode, Key.ClassificationSocietyCode,
                Key.DocumentofComplianceDOCCompanyCode);
        PrintStream out = new PrintStream("target/ships.txt");
        out.println(keys.stream().map(key -> key.name()).collect(Collectors.joining(",")));
        IhsReader.fromZip(file).map(map -> {
            return keys.stream().map(key -> map.get(key.name())).map(x -> x == null ? "" : x)
                    .collect(Collectors.joining(","));
        })
                //
                .doOnNext(out::println)
                //
                .subscribe();
        out.close();
    }
}
