package au.gov.amsa.geo.distance;

import static au.gov.amsa.geo.model.Util.formatDate;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AtomicDouble;

import au.gov.amsa.geo.distance.DistanceTravelledCalculator.CalculationResult;
import au.gov.amsa.geo.model.Bounds;
import au.gov.amsa.geo.model.CellValue;
import au.gov.amsa.geo.model.Options;
import au.gov.amsa.geo.model.Position;
import au.gov.amsa.geo.projection.FeatureUtil;
import au.gov.amsa.geo.projection.Projector;
import au.gov.amsa.geo.projection.ProjectorBounds;
import au.gov.amsa.geo.projection.ProjectorTarget;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Draws a vessel traffic density plot into a {@link Graphics2D}.
 *
 */
public class Renderer {

    private static Logger log = LoggerFactory.getLogger(Renderer.class);
    private final static boolean coloured = false;
    private static final String UNICODE_GREATER_THAN_OR_EQUAL = "\u2265 ";

    public static void paintAll(Graphics2D g, Options options, int numberStandardDeviations, int w,
            int h, CalculationResult calculationResult, boolean addLegend, boolean addParameters) {
        paintMap(g, options.getBounds(), options.getCellSizeDegreesAsDouble(),
                numberStandardDeviations, w, h, calculationResult.getCells(), addLegend);
        if (addParameters)
            paintParameters(g, options, calculationResult, w, h);
    }

    private static Func1<Position, Point> epsg4326Locator(Bounds b, int w, int h) {

        ProjectorBounds projectorBounds = new ProjectorBounds(FeatureUtil.EPSG_4326,
                b.getTopLeftLon(), b.getBottomRightLat(), b.getBottomRightLon(), b.getTopLeftLat());
        ProjectorTarget projectorTarget = new ProjectorTarget(w, h);
        final Projector projector = new Projector(projectorBounds, projectorTarget);

        return new Func1<Position, Point>() {

            @Override
            public Point call(Position p) {
                return projector.toPoint(p.lat(), p.lon());
            }
        };
    }

    public static void paintMap(Graphics2D g, final Bounds b, double cellSizeDegrees,
            double numberStandardDeviationsForHighValue, final int w, final int h,
            Observable<CellValue> cells, boolean addLegend) {

        paintMap(g, b, cellSizeDegrees, numberStandardDeviationsForHighValue, w, h, cells,
                addLegend, epsg4326Locator(b, w, h));
    }

    public static void paintMap(final Graphics2D g, Bounds b, final double cellSizeDegrees,
            double numberStandardDeviationsForHighValue, final int w, final int h,
            Observable<CellValue> cells, final boolean addLegend,
            final Func1<Position, Point> locator) {

        final Statistics metrics = getStatistics(cells);
        final double maxNmForColour = metrics.mean + numberStandardDeviationsForHighValue
                * metrics.sd;

        final double minSaturation = 0.05;
        cells.observeOn(Schedulers.immediate()).subscribeOn(Schedulers.immediate())
        // add to sum and sumSquares
                .doOnNext(new Action1<CellValue>() {
                    @Override
                    public void call(CellValue cell) {
                        double topLeftLat = cell.getCentreLat() + cellSizeDegrees / 2;
                        double topLeftLon = cell.getCentreLon() - cellSizeDegrees / 2;
                        double bottomRightLat = cell.getCentreLat() - cellSizeDegrees / 2;
                        double bottomRightLon = cell.getCentreLon() + cellSizeDegrees / 2;
                        Point topLeft = locator.call(new Position(topLeftLat, topLeftLon));
                        Point bottomRight = locator.call(new Position(bottomRightLat,
                                bottomRightLon));
                        double d = cell.getValue();
                        double prop = Math.min(d, maxNmForColour) / maxNmForColour;
                        Color color = toColor(minSaturation, prop);
                        g.setColor(color);
                        g.fillRect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y
                                - topLeft.y);
                    }
                })
                // count
                .count()
                // block and get
                .toBlocking().single();

        if (addLegend)
            paintLegend(g, cellSizeDegrees, metrics, maxNmForColour, minSaturation, w, h);

    }

    public static void paintParameters(Graphics2D g, Options options,
            CalculationResult calculationResult, int w, int h) {

        g.setColor(Color.darkGray);
        Font font = g.getFont();
        g.setFont(font.deriveFont(10f));
        {
            String label = "startTime=" + formatDate(options.getStartTime()) + ", finishTime="
                    + formatDate(options.getFinishTime()) + ", "
                    + options.getSegmentOptions().toString();
            int labelWidth = g.getFontMetrics().stringWidth(label);
            g.drawString(label, (w - labelWidth) / 2, h - 50);
        }
        {
            String label = metricsToString(calculationResult);
            int labelWidth = g.getFontMetrics().stringWidth(label);
            g.drawString(label, (w - labelWidth) / 2, h - 50 + g.getFontMetrics().getHeight());
        }

        g.setFont(font);
    }

    private static String metricsToString(CalculationResult r) {
        StringBuilder s = new StringBuilder();
        // s.append("cells=" + r.getCells().size());
        DistanceCalculationMetrics m = r.getMetrics();
        s.append(", fixes=" + m.fixes.get());
        s.append(", inTime=" + m.fixesInTimeRange.get());
        s.append(", inRegion=" + m.fixesWithinRegion.get());
        s.append(", effSpdOk=" + m.fixesPassedEffectiveSpeedCheck.get());
        s.append(", segs=" + m.segments.get());
        s.append(", segsTimeDiffOk=" + m.segmentsTimeDifferenceOk.get());
        s.append(", segCells=" + m.segmentCells.get());
        s.append(", totalNm=" + m.totalNauticalMiles);
        return s.toString();
    }

    private static void paintLegend(Graphics2D g, double cellSizeDegrees, Statistics metrics,
            double maxNmForColour, double minSaturation, int w, int h) {
        int legendWidth = 120;
        int rightMargin = 50;
        int legendHeight = 200;
        int topMargin = 50;
        g.setColor(Color.darkGray);
        int legendLeft = w - rightMargin - legendWidth;
        int legendTop = topMargin;
        g.clearRect(legendLeft, legendTop, legendWidth, legendHeight);
        g.drawRect(legendLeft, legendTop, legendWidth, legendHeight);
        int innerTopMargin = 15;
        int innerBottomMargin = 15;
        int innerRightMargin = 5;
        int innerHeight = legendHeight - 2 - innerTopMargin - innerBottomMargin;
        int innerWidth = (legendWidth - innerRightMargin) / 2;

        for (int i = 1; i < innerHeight; i++) {
            int y = legendTop + innerHeight + innerTopMargin - i;
            double prop = (double) i / innerHeight;
            Color color = toColor(minSaturation, prop);
            g.setColor(color);
            g.drawLine(legendLeft + legendWidth - innerWidth - innerRightMargin, y, legendLeft
                    + legendWidth - innerRightMargin, y);
        }
        int numMarkers = 5;
        int markerLeftMargin = 5;
        Font font = g.getFont();
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.setColor(Color.black);
        DecimalFormat df = new DecimalFormat("0.##E0");
        int markerLineLength = 8;
        for (int i = 0; i <= numMarkers; i++) {
            int y = legendTop + innerHeight + innerTopMargin
                    - (int) Math.round((double) i * innerHeight / numMarkers);
            String label = df.format(maxNmForColour * i / numMarkers);
            if (i == numMarkers)
                label = UNICODE_GREATER_THAN_OR_EQUAL + label;
            g.drawString(label, legendLeft + markerLeftMargin, y);

            int markerLineX = legendLeft + legendWidth - innerRightMargin - innerWidth
                    - markerLineLength / 2;
            g.drawLine(markerLineX, y, markerLineX + markerLineLength, y);
        }
        g.drawString("nm/nm2", legendLeft + legendWidth - innerRightMargin - innerWidth + 5,
                legendTop + innerTopMargin - 2);
        g.drawString("cellSize=" + new DecimalFormat("0.###").format(cellSizeDegrees) + "degs",
                legendLeft + markerLeftMargin, legendTop + legendHeight - 3);
        g.setFont(font);

    }

    private static Color toColor(double minSaturation, double prop) {
        if (coloured) {
            return Color.getHSBColor((float) (1 - prop) * 0.5f, 1.0f, 1.0f);
        } else {
            return Color.getHSBColor(0.0f, (float) (prop * (1 - minSaturation) + minSaturation),
                    1.0f);
        }
    }

    private static Statistics getStatistics(Observable<CellValue> cells) {
        final AtomicDouble sum = new AtomicDouble(0);
        final AtomicDouble sumSquares = new AtomicDouble(0);
        log.info("calculating mean and sd");
        long count = cells
        // add to sum and sumSquares
                .doOnNext(new Action1<CellValue>() {
                    @Override
                    public void call(CellValue cell) {
                        sum.addAndGet(cell.getValue());
                        sumSquares.addAndGet(cell.getValue() * cell.getValue());
                    }
                })
                // count
                .count()
                // block and get
                .toBlocking().single();
        double mean = sum.get() / count;
        double variance = sumSquares.get() / count - mean * mean;
        double sd = Math.sqrt(variance);
        log.info("calculated");
        Statistics stats = new Statistics(mean, sd, count);
        log.info(stats.toString());
        return stats;
    }

    public static BufferedImage createImage(Options options, int numStandardDeviations, int width,
            CalculationResult calculationResult) {
        log.info("creating image");
        int height = (int) Math.round(width / options.getBounds().getWidthDegrees()
                * options.getBounds().getHeightDegrees());
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setBackground(Color.white);
        g.clearRect(0, 0, width, height);
        Renderer.paintAll(g, options, numStandardDeviations, width, height, calculationResult,
                true, true);
        log.info("created image");
        return image;
    }

    public static void saveAsPng(BufferedImage image, File file) {
        log.info("saving png to " + file);
        try {
            ImageIO.write(image, "png", file);
            log.info("saved");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Statistics {
        final double mean;
        final double sd;
        private final long count;

        Statistics(double mean, double sd, long count) {
            this.mean = mean;
            this.sd = sd;
            this.count = count;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Statistics [mean=");
            builder.append(mean);
            builder.append(", sd=");
            builder.append(sd);
            builder.append(", count=");
            builder.append(count);
            builder.append("]");
            return builder.toString();
        }

    }

}
