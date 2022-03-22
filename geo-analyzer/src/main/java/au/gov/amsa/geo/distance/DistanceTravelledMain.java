package au.gov.amsa.geo.distance;

import static au.gov.amsa.geo.distance.Renderer.saveAsPng;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import rx.Observable;
import rx.functions.Func1;

public class DistanceTravelledMain {
    private static Logger log = LoggerFactory.getLogger(DistanceTravelledMain.class);

    private static void run(String directory, Options options, boolean gui, String dataSetName) {
        InputStream is;
        try {
            is = new GZIPInputStream(
                    new FileInputStream("/media/an/ship-data/ais/ship-data-2014-v2.txt.gz"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<Integer, Info> shipInfo = ShipStaticData
                .getMapFromReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        List<Setting> settings = new ArrayList<>();
        settings.add(Setting.create(30, 30, "fishing"));
        settings.add(Setting.create(52, 52, "tug"));
        settings.add(Setting.create(60, 69, "passenger"));
        settings.add(Setting.create(70, 79, "cargo"));
        settings.add(Setting.create(80, 89, "tanker"));
        settings.add(Setting.create(90, 99, "other"));
        settings.add(Setting.create(-1, -1, "class_b"));
        settings.add(Setting.create(0, 100, "all"));

        for (Setting setting : settings) {
            // filter out undesired mmsi numbers and ship types
            Func1<Info, Boolean> shipSelector = info -> info != null
                    && ((info.cls == AisClass.B && setting.lowerBound == -1)
                            || (info.cls == AisClass.A && (info.shipType.isPresent()
                                    && info.shipType.get() >= setting.lowerBound
                                    && info.shipType.get() <= setting.upperBound)))
                    && MmsiValidator2.INSTANCE.isValid(info.mmsi);
            calculateTrafficDensity(directory, options, gui, shipInfo, shipSelector,
                    setting.name + "-" + dataSetName);
        }
    }

    private static class Setting {
        final int lowerBound;
        final int upperBound;
        final String name;

        Setting(int lowerBound, int upperBound, String name) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.name = name;
        }

        public static Setting create(int lowerBound, int upperBound, String name) {
            return new Setting(lowerBound, upperBound, name);
        }
    }

    private static void calculateTrafficDensity(String directory, Options options, boolean gui,
            Map<Integer, Info> shipInfo, Func1<Info, Boolean> shipSelector, String name) {
        System.out.println("-----------------------------------------------------");
        System.out.println("------ " + name);
        System.out.println("-----------------------------------------------------");

        final Observable<File> files = Util.getFiles(directory, ".*\\.track")
                //
                .filter(file -> {
                    String s = file.getName();
                    String mmsiString = s.substring(0, s.indexOf(".track"));
                    long mmsi = Long.parseLong(mmsiString);
                    Info info = shipInfo.get(mmsi);
                    return shipSelector.call(info);
                });

        CalculationResult result = DistanceTravelledCalculator.calculateTrafficDensity(options,
                files, 1, 1);

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

        boolean produceImage = false;
        boolean produceDensitiesText = false;
        boolean produceDensitiesNetcdf = true;

        File outputDirectory = new File("/media/an/traffic-density/netcdf");

        if (produceImage) {
            // 8:5 is ok ratio
            saveAsPng(Renderer.createImage(options, 2, 12800, resultFromFile),
                    new File(outputDirectory, name + "-map.png"));
        }

        if (produceDensitiesText) {
            DistanceTravelledCalculator.saveCalculationResultAsText(options, result,
                    new File(outputDirectory, name + "-densities.txt").getAbsolutePath());
        }

        if (produceDensitiesNetcdf) {
            DistanceTravelledCalculator.saveCalculationResultAsNetcdf(options, result,
                    new File(outputDirectory, name + "-densities.nc").getAbsolutePath());
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

        final double cellSizeDegrees;
        if (args.length > 1)
            cellSizeDegrees = Double.parseDouble(args[1]);
        else
            cellSizeDegrees = 0.02;

        final Options options = createOptions(cellSizeDegrees);
        for (int i = 0; i <= 10; i++)
            System.out.println(options.getGrid().centreLon(i));

        for (int i = 2014; i <= 2016; i++) {
            String directory = "/media/an/binary-fixes-5-minute/" + i;
            run(directory, options, false, i + "");
        }
    }
}
