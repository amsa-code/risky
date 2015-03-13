package au.gov.amsa.craft.analyzer.wms;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;
import au.gov.amsa.geo.BinaryFixesObservable;
import au.gov.amsa.geo.consensus.Consensus;
import au.gov.amsa.geo.consensus.ConsensusValue;
import au.gov.amsa.geo.consensus.Options;
import au.gov.amsa.geo.consensus.Options.Builder;
import au.gov.amsa.geo.distance.EffectiveSpeedChecker;
import au.gov.amsa.geo.model.Fix;
import au.gov.amsa.geo.model.SegmentOptions;
import au.gov.amsa.navigation.VesselPosition;

import com.github.davidmoten.grumpy.projection.Projector;
import com.github.davidmoten.grumpy.wms.Layer;
import com.github.davidmoten.grumpy.wms.LayerFeatures;
import com.github.davidmoten.grumpy.wms.WmsRequest;
import com.github.davidmoten.grumpy.wms.WmsUtil;
import com.google.common.base.Optional;

public class AnalyzeLayer implements Layer {

    private static Logger log = LoggerFactory.getLogger(AnalyzeLayer.class);

    private final ConcurrentLinkedQueue<VesselPosition> queue = new ConcurrentLinkedQueue<VesselPosition>();

    public AnalyzeLayer() {
        log.info("creating Analyse layer");

        // collect drifting candidates

        List<String> filenames = new ArrayList<String>();
        for (int i = 1; i <= 31; i++) {
            String filename = "/media/analysis/nmea/2014/NMEA_ITU_201407"
                    + new DecimalFormat("00").format(i) + ".gz";
            if (new File(filename).exists())
                filenames.add(filename);
        }

        // Observable.from(filenames)
        // .buffer(Math.max(1, filenames.size() /
        // Runtime.getRuntime().availableProcessors()))
        // // TODO should not round robin on files!
        // .flatMap(new Func1<List<String>, Observable<VesselPosition>>() {
        // @Override
        // public Observable<VesselPosition> call(List<String> filenames) {
        // return Observable.from(filenames).concatMap(
        // new Func1<String, Observable<VesselPosition>>() {
        // @Override
        // public Observable<VesselPosition> call(String filename) {
        // return Streams.nmeaFromGzip(filename)
        // // extract positions from nmea
        // .compose(AisVesselPositions.positions())
        // // detect drift
        // .compose(DriftingDetector.detectDrift())
        // .subscribeOn(Schedulers.computation());
        // }
        // });
        // }
        // })

        Sources.fixes()
                // group by id and date
                .groupBy(new Func1<VesselPosition, String>() {
                    @Override
                    public String call(VesselPosition p) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        return p.id().uniqueId() + sdf.format(new Date(p.time()));
                    }
                })
                // grab the first position each day by ship identifier
                .flatMap(
                        new Func1<GroupedObservable<String, VesselPosition>, Observable<VesselPosition>>() {

                            @Override
                            public Observable<VesselPosition> call(
                                    GroupedObservable<String, VesselPosition> positions) {
                                return positions.first();
                            }
                        })
                // add to queue
                .doOnNext(new Action1<VesselPosition>() {

                    @Override
                    public void call(VesselPosition p) {
                        // System.out.println(p.lat() + "\t" + p.lon() + "\t"
                        // + p.id().uniqueId());
                        System.out.println(p);
                        queue.add(p);
                    }
                })
                // run in background
                .subscribeOn(Schedulers.newThread())
                // subscribe
                .subscribe();
    }

    @Override
    public LayerFeatures getFeatures() {
        return LayerFeatures.builder().crs("EPSG:4326").crs("EPSG:3857").name("Analyze")
                .queryable().build();
    }

    @Override
    public String getInfo(Date arg0, WmsRequest arg1, Point arg2, String arg3) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void render(Graphics2D g, WmsRequest request) {
        String craftId = request.getParam("craftId");
        craftId = "1935104788";
        // craftId = "5495305793";
        String filename = System.getProperty("user.home")
                + "/Downloads/positions-365-days-2/craft-" + craftId;
        File file = new File(filename);
        final long startTime = DateTime.parse("2013-02-01").getMillis();
        final long finishTime = DateTime.parse("2014-08-08").getMillis();
        Observable<Fix> source = BinaryFixesObservable.TO_FIXES.call(file);
        // Observable<Fix> source = getSource(file);

        Observable<Fix> fixes = source.filter(new Func1<Fix, Boolean>() {

            @Override
            public Boolean call(Fix fix) {
                return fix.getTime() >= startTime && fix.getTime() < finishTime;
            }
        })
        // sort by time
                .toSortedList(new Func2<Fix, Fix, Integer>() {
                    @Override
                    public Integer call(Fix a, Fix b) {
                        return ((Long) a.getTime()).compareTo(b.getTime());
                    }
                })
                // flatten
                .flatMap(new Func1<List<Fix>, Observable<Fix>>() {

                    @Override
                    public Observable<Fix> call(List<Fix> list) {
                        return Observable.from(list);
                    }
                });

        // Observable.from(list).doOnNext(renderFix(g, request)).subscribe();

        log.info("drawing " + queue.size() + " positions");
        log.info("request=" + request);
        final Projector projector = WmsUtil.getProjector(request);
        for (VesselPosition p : queue) {
            try {
                Point point = projector.toPoint(p.lat(), p.lon());
                g.setColor(Color.red);
                g.drawRect(point.x, point.y, 1, 1);
            } catch (RuntimeException e) {
                System.out.println("error point p=" + p);
                e.printStackTrace();
            }
        }
        log.info("drawn");

    }

    private Observable<Fix> getSource(File file) {
        Builder options = Options.builder().before(5).after(5).maxSpeedKnots(30)
                .adjustmentLowerLimitMillis(TimeUnit.HOURS.toMillis(-1))
                .adjustmentUpperLimitMillis(TimeUnit.HOURS.toMillis(1));
        Observable<Fix> fixes = BinaryFixesObservable.TO_FIXES.call(file);
        NavigableSet<Fix> improved = Consensus.improveConsensus(new TreeSet<>(fixes.toList()
                .toBlocking().single()), options.build());
        improved = Consensus.improveConsensus(new TreeSet<>(new ArrayList<>(improved)), options
                .before(3).after(3).build());
        return Observable.from(improved);
    }

    private Action1<ConsensusValue> renderFix(final Graphics2D g, final WmsRequest request) {
        final Projector projector = WmsUtil.getProjector(request);
        return new Action1<ConsensusValue>() {

            ConsensusValue previous = null;
            Point previousPoint = null;

            boolean flip = false;

            @Override
            public void call(ConsensusValue c) {
                Fix fix = c.getFix();
                Point point = projector.toPoint(fix.getPosition().getLat(), fix.getPosition()
                        .getLon());
                if (previous != null) {
                    Optional<Double> speedKnots = EffectiveSpeedChecker.effectiveSpeedKnots(
                            previous.getFix(), fix, SegmentOptions.builder().build());
                    g.setColor(Color.blue);
                    if (Math.abs(previousPoint.x - point.x) <= request.getWidth())
                        g.drawLine(previousPoint.x, previousPoint.y, point.x, point.y);

                    if (!speedKnots.isPresent() || speedKnots.get() < 80) {
                        g.setColor(Color.black);
                        g.fillRect(point.x - 2, point.y - 2, 4, 4);
                    } else {

                        g.setColor(Color.red);
                        g.fillRect(point.x - 2, point.y - 2, 4, 4);
                        DecimalFormat df = new DecimalFormat("0");
                        // g.setFont(g.getFont().deriveFont(9f));
                        flip = !flip;
                        int y;
                        if (flip)
                            y = point.y + 17;
                        else
                            y = point.y - 5;
                        g.drawString(df.format(speedKnots.get()) + "kts", point.x + 2, y);

                        boolean extras = false;
                        if (extras) {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd HH:mm");
                            // sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                            g.drawString(sdf.format(new Date(fix.getTime())), point.x + 2, y
                                    + g.getFontMetrics().getHeight());
                            g.drawString(sdf.format(new Date(previous.getFix().getTime())),
                                    point.x + 2, y + 2 * g.getFontMetrics().getHeight());
                            g.drawString("c1=" + previous.getValue(), point.x + 2, y + 3
                                    * g.getFontMetrics().getHeight());
                            g.drawString("c2=" + c.getValue(), point.x + 2, y + 4
                                    * g.getFontMetrics().getHeight());
                        }
                    }
                }
                previous = c;
                previousPoint = point;
            }
        };
    }
}
