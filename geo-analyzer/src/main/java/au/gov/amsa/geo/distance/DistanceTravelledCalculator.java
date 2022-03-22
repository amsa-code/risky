package au.gov.amsa.geo.distance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.rx.slf4j.Logging;
import com.google.common.annotations.VisibleForTesting;

import au.gov.amsa.geo.model.Bounds;
import au.gov.amsa.geo.model.Cell;
import au.gov.amsa.geo.model.CellValue;
import au.gov.amsa.geo.model.GridTraversor;
import au.gov.amsa.geo.model.Options;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.geo.model.Util;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.risky.format.HasPosition;
import au.gov.amsa.util.navigation.Position;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.Index2D;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

public class DistanceTravelledCalculator {

    private static Logger log = LoggerFactory.getLogger(DistanceTravelledCalculator.class);

    private final Options options;
    private final DistanceCalculationMetrics metrics;

    public DistanceTravelledCalculator(Options options, DistanceCalculationMetrics metrics) {
        this.options = options;
        this.metrics = metrics;
    }

    /**
     * Returns distinct cells and the the total nautical miles travelled in that
     * cell. Uses RxJava {@link Observable}s to maximize throughput and will
     * scale to use all available processors and to handle a large number of
     * files (number of open file handles should be limited by number of
     * available processors).
     * 
     * @param files
     * @return
     */
    public Observable<CellAndDistance> calculateDistanceByCellFromFiles(Observable<File> files) {
        // use a map-reduce approach where the parallel method shares out
        // ('maps') the fixes by craft to multiple threads (number determined by
        // available processors) and is passed the 'reduce'
        // calculateDistanceByCellFromFile() method to combine the results.
        int numFiles = files.count().toBlocking().single();
        log.info("numFiles=" + numFiles);
        AtomicLong fileCount = new AtomicLong();
        AtomicLong cellCount = new AtomicLong(1);
        return files
                // buffer for parallel processing of groups of files
                .buffer(Math.max(1,
                        (int) Math.round(
                                Math.ceil(numFiles / Runtime.getRuntime().availableProcessors()))))
                .flatMap(fileList -> extractCellDistances(fileCount, cellCount, fileList))
                // sum distances into global map
                .collect(bigMapFactory(), collectCellDistances())
                // report the cell distances for the grid
                .flatMap(listCellDistances())
                // record total nm in metrics
                .doOnNext(sumNauticalMiles());
    }

    private Func1<? super HashMap<Cell, Double>, Observable<CellAndDistance>> listCellDistances() {
        return map -> Observable.from(map.entrySet())
                .map(entry -> new CellAndDistance(entry.getKey(), entry.getValue()));
    }

    private Func0<HashMap<Cell, Double>> bigMapFactory() {
        return () -> new HashMap<Cell, Double>(20_000_000, 1.0f);
    }

    private Action2<HashMap<Cell, Double>, Map<Cell, Double>> collectCellDistances() {
        return (a, b) -> {
            // put all entries in b into a
            long t = System.currentTimeMillis();
            log.info("reducing");
            for (Entry<Cell, Double> entry : b.entrySet()) {
                Double val = a.putIfAbsent(entry.getKey(), entry.getValue());
                if (val != null) {
                    a.put(entry.getKey(), val + entry.getValue());
                }
            }
            log.info("reduced in " + (System.currentTimeMillis() - t) + "ms");
        };
    }

    private Observable<Map<Cell, Double>> extractCellDistances(AtomicLong fileCount,
            AtomicLong cellCount, List<File> fileList) {
        return // extract fixes from each file
        Observable.from(fileList)
                .lift(Logging.<File> logger().showCount(fileCount).every(1000).log())
                .map(file -> BinaryFixes.from(file))
                // for one craft aggregate distance (not a
                // problem with SerializedObserver buffering
                // because each file relatively small), also
                // subscribes on computation() to get
                // concurrency
                .flatMap(toCraftCellAndDistances)
                // log
                .lift(Logging.<CellAndDistance> logger().showCount("cellsReceived", cellCount)
                        .every(1_000_000).showMemory().log())
                // sum cell distances and emit maps of up to
                // 100K entries
                .lift(OperatorSumCellDistances.create(1_000_000))
                .subscribeOn(Schedulers.computation());
    }

    private final Func1<Observable<Fix>, Observable<CellAndDistance>> toCraftCellAndDistances = new Func1<Observable<Fix>, Observable<CellAndDistance>>() {

        @Override
        public Observable<CellAndDistance> call(Observable<Fix> allFixesForASingleCraft) {

            return allFixesForASingleCraft
                    // count fixes
                    .doOnNext(incrementFixesCount)
                    // filter on time between startTime and finishTime if exist
                    .filter(inTimeRange)
                    // restrict to fixes in filter bounds
                    .filter(inRegion)
                    // sort fixes by position time
                    // .toSortedList(au.gov.amsa.geo.Util.COMPARE_FIXES_BY_POSITION_TIME)
                    // convert list to Observable and flatten
                    // .concatMap(au.gov.amsa.geo.Util.TO_OBSERVABLE)
                    // keep only positions that pass effective speed
                    .lift(filterOnEffectiveSpeedOk())
                    // filter on passed effective speed check
                    .filter(check -> check.isOk())
                    // map back to the fix again
                    .map(check -> check.fix())
                    // update metrics with fixes passing effective speed check
                    .doOnNext(countFixesPassedEffectiveSpeedCheck)
                    // pair them up again
                    .buffer(2, 1)
                    // segments only
                    .filter(PAIRS_ONLY)
                    // count segments
                    .doOnNext(segment -> metrics.segments.incrementAndGet())
                    // remove segments with invalid time separation
                    .filter(timeDifferenceOk)
                    // remove segments with invalid distance separation
                    .filter(distanceOk)
                    // calculate distances
                    .flatMap(toCellAndDistance)
                    // update counts of cells in each segment
                    .doOnNext(countSegmentCells)
                    // use memory to buffer if producing fast
                    .onBackpressureBuffer();
        }

    };

    private OperatorEffectiveSpeedChecker filterOnEffectiveSpeedOk() {
        return new OperatorEffectiveSpeedChecker(options.getSegmentOptions());
    }

    private final Func1<Fix, Boolean> inRegion = new Func1<Fix, Boolean>() {
        @Override
        public Boolean call(Fix fix) {
            boolean in = options.getFilterBounds().contains(fix);
            if (in)
                metrics.fixesWithinRegion.incrementAndGet();
            return in;
        }
    };

    private final Func1<Fix, Boolean> inTimeRange = new Func1<Fix, Boolean>() {
        @Override
        public Boolean call(Fix fix) {

            boolean lowerBoundOk = !options.getStartTime().isPresent()
                    || fix.time() >= options.getStartTime().get();
            boolean upperBoundOk = !options.getFinishTime().isPresent()
                    || fix.time() < options.getFinishTime().get();
            boolean result = lowerBoundOk && upperBoundOk;
            if (result)
                metrics.fixesInTimeRange.incrementAndGet();
            return result;
        }
    };

    private final Func1<List<Fix>, Boolean> timeDifferenceOk = new Func1<List<Fix>, Boolean>() {
        @Override
        public Boolean call(List<Fix> pair) {
            Preconditions.checkArgument(pair.size() == 2);
            Fix a = pair.get(0);
            Fix b = pair.get(1);
            boolean ok = timeDifferenceOk(a, b, options.getSegmentOptions());
            if (ok)
                metrics.segmentsTimeDifferenceOk.incrementAndGet();
            return ok;
        }
    };

    private final Func1<List<Fix>, Boolean> distanceOk = new Func1<List<Fix>, Boolean>() {
        @Override
        public Boolean call(List<Fix> pair) {
            Preconditions.checkArgument(pair.size() == 2);
            Fix a = pair.get(0);
            Fix b = pair.get(1);
            boolean ok = distanceOk(a, b, options.getSegmentOptions());
            if (ok)
                metrics.segmentsDistanceOk.incrementAndGet();
            return ok;
        }
    };

    private static boolean timeDifferenceOk(Fix a, Fix b, SegmentOptions o) {
        long timeDiffMs = Math.abs(a.time() - b.time());
        return o.maxTimePerSegmentMs() == null || timeDiffMs <= o.maxTimePerSegmentMs();
    }

    private static boolean distanceOk(Fix a, Fix b, SegmentOptions o) {
        return o.maxDistancePerSegmentNm() > Util.greatCircleDistanceNm(a, b);
    }

    private static final Func1<List<Fix>, Boolean> PAIRS_ONLY = new Func1<List<Fix>, Boolean>() {

        @Override
        public Boolean call(List<Fix> list) {
            return list.size() == 2;
        }
    };

    private final Func1<List<Fix>, Observable<CellAndDistance>> toCellAndDistance = new Func1<List<Fix>, Observable<CellAndDistance>>() {

        @Override
        public Observable<CellAndDistance> call(List<Fix> pair) {
            Preconditions.checkArgument(pair.size() == 2);
            Fix fix1 = pair.get(0);
            Fix fix2 = pair.get(1);
            return getCellDistances(fix1, fix2, options);
        }

    };

    private final Action1<CellAndDistance> countSegmentCells = new Action1<CellAndDistance>() {

        @Override
        public void call(CellAndDistance cd) {
            metrics.segmentCells.incrementAndGet();
        }

    };

    @VisibleForTesting
    static final Observable<CellAndDistance> getCellDistances(HasPosition a, HasPosition b,
            Options options) {
        return getCellDistances(Util.toPos(a), Util.toPos(b), options);
    }

    @VisibleForTesting
    static final Observable<CellAndDistance> getCellDistances(final Position a, final Position b,
            final Options options) {

        return Observable.create(new OnSubscribe<CellAndDistance>() {

            @Override
            public void call(Subscriber<? super CellAndDistance> subscriber) {
                try {
                    GridTraversor grid = new GridTraversor(options);
                    boolean keepGoing = true;
                    Position p1 = a;
                    Position destination = b;
                    int count = 0;
                    while (keepGoing) {
                        Position p2 = grid.nextPoint(p1, destination);
                        double distanceNm = p1.getDistanceToKm(p2) / 1.852;
                        // report cell and distance
                        Optional<Cell> cell = Cell.cellAt(p1.getLat(), p1.getLon(), options);
                        if (cell.isPresent())
                            subscriber.onNext(new CellAndDistance(cell.get(), distanceNm));
                        keepGoing = p2.getLat() != destination.getLat()
                                || p2.getLon() != destination.getLon();
                        keepGoing = keepGoing && !subscriber.isUnsubscribed();
                        p1 = p2;
                        count++;
                        checkCount(p1, destination, count, options);
                    }
                    subscriber.onCompleted();
                } catch (Throwable t) {
                    // TODO resolve all problems so that this will revert to a
                    // call to onError
                    log.warn(t.getMessage(), t);
                    subscriber.onCompleted();
                    // subscriber.onError(t);
                }
            }

        });
    }

    private static void checkCount(Position p1, Position destination, int count, Options options) {
        if (count > 100000)
            throw new RuntimeException("unexpectedly stuck in loop p1=" + p1 + ",destination="
                    + destination + ",options=" + options);
    }

    public DistanceCalculationMetrics getMetrics() {
        return metrics;
    }

    public static class CalculationResult {
        private final Observable<CellValue> cells;
        private final DistanceCalculationMetrics metrics;

        public CalculationResult(Observable<CellValue> cells, DistanceCalculationMetrics metrics) {
            this.cells = cells;
            this.metrics = metrics;
        }

        public Observable<CellValue> getCells() {
            return cells;
        }

        public DistanceCalculationMetrics getMetrics() {
            return metrics;
        }

    }

    public static Observable<CellValue> calculateDensityByCellFromFiles(Options options,
            Observable<File> files, int horizontal, int vertical,
            DistanceCalculationMetrics metrics) {
        Observable<CellValue> cells = partition(options, horizontal, vertical)
                // get results (blocks to limit memory use)
                .concatMap(calculateDistanceTravelled(files, metrics));

        if (horizontal > 1 || vertical > 1)
            // aggregate using a file backed map
            return cells.lift(new OperatorSumCellValues(true));
        else
            return cells;
    }

    private static Func1<Options, Observable<CellValue>> calculateDistanceTravelled(
            final Observable<File> files, final DistanceCalculationMetrics metrics) {
        return new Func1<Options, Observable<CellValue>>() {

            @Override
            public Observable<CellValue> call(Options options) {
                log.info("running distance calculation on " + options);
                DistanceTravelledCalculator c = new DistanceTravelledCalculator(options, metrics);
                // blocks to return answer, this is desirable because we need to
                // back the results with a file because they can get so large
                return Observable.from(c.calculateDistanceByCellFromFiles(files)
                        // as cell density values
                        .map(toCellDensityValue(options))
                        // as list
                        .toList()
                        // block and get
                        .toBlocking().single());
            }
        };
    }

    public static CalculationResult calculateTrafficDensity(Options options,
            Observable<File> files) {
        return calculateTrafficDensity(options, files, 1, 1);
    }

    public static CalculationResult calculateTrafficDensity(Options options, Observable<File> files,
            int horizontal, int vertical) {
        int maxNumCells = (int) Math.round(options.getBounds().getWidthDegrees()
                * options.getBounds().getHeightDegrees() / options.getCellSizeDegreesAsDouble()
                / options.getCellSizeDegreesAsDouble());
        log.info("maxNumCells=" + maxNumCells);
        DistanceCalculationMetrics metrics = new DistanceCalculationMetrics();
        final Observable<CellValue> cells = DistanceTravelledCalculator
                .calculateDensityByCellFromFiles(options, files, horizontal, vertical, metrics);
        return new CalculationResult(cells, metrics);
    }

    private static Func1<CellAndDistance, CellValue> toCellDensityValue(final Options options) {
        return new Func1<CellAndDistance, CellValue>() {

            @Override
            public CellValue call(CellAndDistance cd) {
                return new CellValue(cd.getCell().getCentreLat(options),
                        cd.getCell().getCentreLon(options), cd.getTrafficDensity(options));
            }
        };
    }

    public static void saveCalculationResultAsText(Options options,
            CalculationResult calculationResult, String filename) {
        try {
            final PrintWriter out = new PrintWriter(filename);
            Bounds b = options.getBounds();
            out.println(
                    "#originLat, originLon, cellSizeDegrees, topLefLat, topLeftLon, bottomRightLat, bottomRightLon");
            out.format("%s\t%s\t%s\t%s\t%s\t%s\t%s\n", options.getOriginLat(),
                    options.getOriginLon(), options.getCellSizeDegrees(), b.getTopLeftLat(),
                    b.getTopLeftLon(), b.getBottomRightLat(), b.getBottomRightLon());
            out.println("#centreLat, centreLon, distanceNmPerNm2");

            calculationResult.getCells().subscribe(new Observer<CellValue>() {

                @Override
                public void onCompleted() {
                    out.close();
                }

                @Override
                public void onError(Throwable e) {
                    out.close();
                }

                @Override
                public void onNext(CellValue cell) {
                    out.format("%s\t%s\t%s\n", cell.getCentreLat(), cell.getCentreLon(),
                            cell.getValue());
                }
            });

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveCalculationResultAsNetcdf(Options options,
            CalculationResult calculationResult, String filename) {

        List<CellValue> list = calculationResult.getCells().toList().toBlocking().single();

        int maxLonIndex = list.stream()
                .map(cell -> options.getGrid().cellAt(cell.getCentreLat(), cell.getCentreLon()))
                .filter(x -> x.isPresent()) //
                .map(x -> x.get().getLonIndex())
                .max(Comparator.<Long> naturalOrder()) //
                .get().intValue();
        int maxLatIndex = list.stream()
                .map(cell -> options.getGrid().cellAt(cell.getCentreLat(), cell.getCentreLon()))
                .filter(x -> x.isPresent()).map(x -> x.get().getLatIndex())
                .max(Comparator.<Long> naturalOrder()).get().intValue();

        File file = new File(filename);

        // Create the file.
        NetcdfFileWriter f = null;
        try {
            // Create new netcdf-3 file with the given filename
            f = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, file.getPath());

            // In addition to the latitude and longitude dimensions, we will
            // also create latitude and longitude netCDF variables which will
            // hold the actual latitudes and longitudes. Since they hold data
            // about the coordinate system, the netCDF term for these is:
            // "coordinate variables."
            Dimension dimLat = f.addDimension(null, "latitude", maxLatIndex + 1);
            Dimension dimLon = f.addDimension(null, "longitude", maxLonIndex + 1);

            List<Dimension> dims = new ArrayList<Dimension>();
            dims.add(dimLat);
            dims.add(dimLon);

            // coordinate variables
            Variable vLat = f.addVariable(null, "latitude", DataType.DOUBLE, "latitude");
            Variable vLon = f.addVariable(null, "longitude", DataType.DOUBLE, "longitude");

            // value variables
            Variable vDensity = f.addVariable(null, "traffic_density", DataType.DOUBLE, dims);

            // Define units attributes for coordinate vars. This attaches a
            // text attribute to each of the coordinate variables, containing
            // the units.

            vLon.addAttribute(new Attribute("units", "degrees_east"));
            vLat.addAttribute(new Attribute("units", "degrees_north"));

            // Define units attributes for variables.
            vDensity.addAttribute(new Attribute("units", "nm-1"));
            vDensity.addAttribute(new Attribute("long_name", ""));

            // Write the coordinate variable data. This will put the latitudes
            // and longitudes of our data grid into the netCDF file.
            f.create();
            {
                Array dataLat = Array.factory(DataType.DOUBLE, new int[] { dimLat.getLength() });
                Array dataLon = Array.factory(DataType.DOUBLE, new int[] { dimLon.getLength() });

                // set latitudes
                for (int i = 0; i <= maxLatIndex; i++) {
                    dataLat.setDouble(i, options.getGrid().centreLat(i));
                }

                // set longitudes
                for (int i = 0; i <= maxLonIndex; i++) {
                    dataLon.setDouble(i, options.getGrid().centreLon(i));
                }

                f.write(vLat, dataLat);
                f.write(vLon, dataLon);
            }

            // write the value variable data
            {
                int[] iDim = new int[] { dimLat.getLength(), dimLon.getLength() };
                Array dataDensity = ArrayDouble.D2.factory(DataType.DOUBLE, iDim);

                Index2D idx = new Index2D(iDim);

                for (CellValue point : list) {
                    Optional<Cell> cell = options.getGrid().cellAt(point.getCentreLat(),
                            point.getCentreLon());
                    if (cell.isPresent()) {
                        idx.set((int) cell.get().getLatIndex(), (int) cell.get().getLonIndex());
                        dataDensity.setDouble(idx, point.getValue());
                    }
                }
                f.write(vDensity, dataDensity);
            }
        } catch (IOException | InvalidRangeException e) {
            throw new RuntimeException(e);
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
    }

    static List<Double> makeConstantDifference(List<Double> list) {
        List<Double> result = new ArrayList<>();
        double diff = list.get(1) - list.get(0);
        Double previous = null;
        for (int i = 0; i < list.size(); i++) {
            double next;
            if (previous == null) {
                next = list.get(0);
            } else {
                next = previous + diff;
            }
            result.add(next);
            previous = next;
        }
        return result;
    }

    // public static void saveCalculationResultAsBinary(Options options,
    // CalculationResult calculationResult, String filename) {
    // try {
    // List<CellValue> cells = calculationResult.getCells();
    // DataOutputStream out = new DataOutputStream(
    // new BufferedOutputStream(new FileOutputStream(filename)));
    //
    // Bounds b = options.getBounds();
    // out.writeDouble(options.getCellSizeDegreesAsDouble());
    // out.writeDouble(b.getTopLeftLat());
    // out.writeDouble(b.getTopLeftLon());
    // out.writeDouble(b.getBottomRightLat());
    // out.writeDouble(b.getBottomRightLon());
    //
    // for (CellValue cell : cells) {
    // out.writeDouble(cell.getCentreLat());
    // out.writeDouble(cell.getCentreLon());
    // out.writeDouble(cell.getValue());
    // }
    // out.close();
    // } catch (FileNotFoundException e) {
    // throw new RuntimeException(e);
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    // }

    private Action1<CellAndDistance> sumNauticalMiles() {
        return new Action1<CellAndDistance>() {
            @Override
            public void call(CellAndDistance cell) {
                metrics.totalNauticalMiles.addAndGet(cell.getDistanceNm());
            }
        };
    }

    private final Action1<? super Fix> incrementFixesCount = new Action1<Fix>() {

        @Override
        public void call(Fix fix) {
            metrics.fixes.incrementAndGet();
        }
    };

    private final Action1<? super Fix> countFixesPassedEffectiveSpeedCheck = new Action1<Fix>() {

        @Override
        public void call(Fix fix) {
            metrics.fixesPassedEffectiveSpeedCheck.incrementAndGet();
        }
    };

    /**
     * Returns a sequence of {@link Options} that are same as the source apart
     * from the {@link Bounds} which are partitioned according to horizontal and
     * vertical parameters. For map-reduce purposes we need to be able to
     * partition the bounds of Options. Passing horizontal=1 and vertical=1 will
     * return one item only being a copy of the source {@link Options}.
     * 
     * @param options
     * @param horizontal
     *            number of regions (with longitude)
     * @param vertical
     *            number of regions (with latitude)
     * @return
     */
    public static Observable<Options> partition(final Options options, final int horizontal,
            final int vertical) {
        List<Options> list = new ArrayList<>();
        Bounds bounds = options.getBounds();
        double h = bounds.getWidthDegrees() / horizontal;
        double v = bounds.getHeightDegrees() / vertical;
        for (int i = 0; i < horizontal; i++) {
            for (int j = 0; j < vertical; j++) {
                double lat = bounds.getTopLeftLat() - j * v;
                double lon = bounds.getTopLeftLon() + i * h;
                Bounds b = new Bounds(lat, lon, lat - v, lon + h);
                list.add(options.buildFrom().bounds(b).filterBounds(b.expand(7, 7)).build());
            }
        }
        return Observable.from(list);
    }

}
