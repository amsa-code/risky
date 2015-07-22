package au.gov.amsa.geo.distance;

import static au.gov.amsa.geo.distance.DistanceTravelledCalculator.calculateTrafficDensity;
import static au.gov.amsa.geo.distance.Renderer.saveAsPng;

import java.io.File;
import java.util.concurrent.TimeUnit;

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
import au.gov.amsa.util.rx.OperatorWriteBytes;

public class DistanceTravelledMain {
    private static Logger log = Logger.getLogger(DistanceTravelledMain.class);

    private static void run(String directory, Options options, boolean gui) {

        final Observable<File> files = Util.getFiles(directory, ".*\\.track");

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
        // 8:5 is ok ratio
        saveAsPng(Renderer.createImage(options, 2, 1280, resultFromFile),
                new File("target/map.png"));

        // DistanceTravelledCalculator.saveCalculationResultAsText(options,
        // result,
        // "target/densities.txt");
        // DistanceTravelledCalculator.saveCalculationResultAsBinary(options,
        // result,
        // "target/densities.bin");
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
                        .maxSpeedKnots(1000)
                        // set max time per segment
                        .maxTimePerSegmentMs(TimeUnit.DAYS.toMillis(1))
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
            cellSizeDegrees = 0.5;

        final Options options = createOptions(cellSizeDegrees);
        run(directory, options, false);
    }
}
