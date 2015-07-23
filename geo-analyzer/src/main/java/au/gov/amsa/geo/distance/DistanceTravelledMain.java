package au.gov.amsa.geo.distance;

import static au.gov.amsa.geo.distance.DistanceTravelledCalculator.calculateTrafficDensity;
import static au.gov.amsa.geo.distance.Renderer.saveAsPng;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import rx.Observable;
import au.gov.amsa.geo.BinaryCellValuesObservable;
import au.gov.amsa.geo.OperatorCellValuesToBytes;
import au.gov.amsa.geo.Util;
import au.gov.amsa.geo.distance.DistanceTravelledCalculator.CalculationResult;
import au.gov.amsa.geo.model.Bounds;
import au.gov.amsa.geo.model.CellValue;
import au.gov.amsa.geo.model.Options;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.navigation.ShipStaticData;
import au.gov.amsa.navigation.ShipStaticData.Info;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.util.identity.MmsiValidator2;
import au.gov.amsa.util.rx.OperatorWriteBytes;

import com.google.common.base.Charsets;

public class DistanceTravelledMain {
    private static Logger log = Logger.getLogger(DistanceTravelledMain.class);

    private static void run(String directory, Options options, boolean gui) {
        InputStream is;
        try {
            is = new GZIPInputStream(new FileInputStream(
                    "/media/an/ship-data/ais/ship-data-2014.txt.gz"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<Long, Info> shipInfo = ShipStaticData.getMapFromReader(new InputStreamReader(is,
                Charsets.UTF_8));
        final Observable<File> files = Util.getFiles(directory, ".*\\.track")
        // remove bad mmsi numbers
                .filter(file -> {
                    String s = file.getName();
                    String mmsiString = s.substring(0, s.indexOf(".track"));
                    long mmsi = Long.parseLong(mmsiString);
                    Info info = shipInfo.get(mmsi);
                    return MmsiValidator2.INSTANCE.isValid(mmsi) && info != null
                            && info.shipType.isPresent() && info.cls == AisClass.A
                            && (info.shipType.get() >= 60 && info.shipType.get() <= 99);
                });

        CalculationResult result = calculateTrafficDensity(options, files, 1, 1);

        if (gui) {
            DisplayPanel.displayGui(files, options, result);
        }
        String filename = result.getCells()
        // to bytes
                .lift(new OperatorCellValuesToBytes(options))
                // save bytes to a file
                .lift(new OperatorWriteBytes())
                // get filename
                .toBlocking().single();
        log.info("result saved to file " + filename);

        CalculationResult resultFromFile = new CalculationResult(BinaryCellValuesObservable
                .readValues(new File(filename)).skip(1).cast(CellValue.class), result.getMetrics());

        boolean produceImage = true;
        boolean produceDensitiesText = true;

        if (produceImage) {
            // 8:5 is ok ratio
            saveAsPng(Renderer.createImage(options, 2, 12800, resultFromFile), new File(
                    "target/map.png"));
        }

        if (produceDensitiesText) {
            DistanceTravelledCalculator.saveCalculationResultAsText(options, result,
                    "target/densities.txt");
        }
    }

    private static Options createOptions(double cellSizeDegrees) {
        return Options.builder()
        // set origin latitude
                .originLat(0)
                // set origin longitudue
                .originLon(0)
                // square cell size in degrees
                .cellSizeDegrees(cellSizeDegrees)
                // set bounds
                .bounds(new Bounds(15, 67, -60, 179))
                // sabine bounds:
                // .bounds(new Bounds(-10, 110, -45, 158))
                // .bounds(new Bounds(15, 90, -20, 125))
                // set start
                // .startTime("2014-04-20")
                // set finish
                // .finishTime("2014-06-06")
                // set segment options
                .segmentOptions(SegmentOptions.builder()
                // set max speed knots
                        .maxSpeedKnots(100)
                        //
                        .speedCheckDistanceThresholdNm(30)
                        //
                        .speedCheckMinTimeDiff(3, TimeUnit.MINUTES)
                        // set max time per segment
                        .maxTimePerSegment(1, TimeUnit.DAYS)
                        //
                        .maxDistancePerSegmentNm(500.0)
                        // build
                        .build())
                // build options
                .build();
    }

    public static void main(String[] args) throws InterruptedException {
        log.info("starting");
        String directory;
        if (args.length > 0)
            directory = args[0];
        else
            directory = "/media/an/binary-fixes-5-minute/2014";
        // directory = System.getProperty("user.home")
        // + "/Downloads/positions-183-days";
        final double cellSizeDegrees;
        if (args.length > 1)
            cellSizeDegrees = Double.parseDouble(args[1]);
        else
            cellSizeDegrees = 0.02;

        final Options options = createOptions(cellSizeDegrees);
        run(directory, options, false);
    }
}
