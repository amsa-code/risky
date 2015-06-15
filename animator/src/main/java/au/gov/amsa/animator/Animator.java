package au.gov.amsa.animator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wms.WebMapServer;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.map.WMSLayer;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.wms.WMSLayerChooser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import rx.Scheduler.Worker;
import rx.internal.util.SubscriptionList;
import rx.schedulers.Schedulers;
import rx.schedulers.SwingScheduler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class Animator {

    private static final float CANBERRA_LAT = -35.3075f;
    private static final float CANBERRA_LONG = 149.1244f;
    private final Model model = new Model();
    private final View view = new View();
    private volatile BufferedImage image;
    private volatile BufferedImage backgroundImage;
    private volatile BufferedImage offscreenImage;

    final JPanel panel = createMapPanel();
    final MapContent map;
    private final SubscriptionList subscriptions;
    private final Worker worker;
    private BufferedImage offScreenImage;

    public Animator() {

        map = createMap();
        subscriptions = new SubscriptionList();
        worker = Schedulers.newThread().createWorker();
        subscriptions.add(worker);
    }

    private JPanel createMapPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, null);
            }
        };
        panel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

            }

        });
        return panel;
    }

    public void start() {
        SwingScheduler.getInstance().createWorker().schedule(() -> {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(panel);
            FramePreferences.restoreLocationAndSize(frame, 100, 100, 800, 600, Animator.class);
            frame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    backgroundImage = null;
                    redraw();
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    super.componentShown(e);
                    backgroundImage = null;
                    redraw();
                }
            });
            frame.setVisible(true);
        });
        final AtomicInteger timeStep = new AtomicInteger();
        worker.schedulePeriodically(() -> {
            model.updateModel(timeStep.getAndIncrement());
            redrawAnimationLayer();
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void redraw() {

        ReferencedEnvelope mapBounds = map.getMaxBounds();
        // get the frame width and height
        int width = panel.getParent().getWidth();
        double ratio = mapBounds.getHeight() / mapBounds.getWidth();
        int proportionalHeight = (int) Math.round(width * ratio);
        Rectangle imageBounds = new Rectangle(0, 0, width, proportionalHeight);
        if (backgroundImage == null) {
            BufferedImage backgroundImage = new BufferedImage(imageBounds.width,
                    imageBounds.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D gr = backgroundImage.createGraphics();
            gr.setPaint(Color.WHITE);
            gr.fill(imageBounds);
            StreamingRenderer renderer = new StreamingRenderer();
            renderer.setMapContent(map);
            renderer.paint(gr, imageBounds, mapBounds);
            this.backgroundImage = backgroundImage;
            this.offScreenImage = new BufferedImage(imageBounds.width, imageBounds.height,
                    BufferedImage.TYPE_INT_RGB);
            offScreenImage.createGraphics();
        }
        redrawAnimationLayer();
    }

    private void redrawAnimationLayer() {
        // if (backgroundImage != null && offscreenImage != null) {
        if (offScreenImage != null) {
            System.out.println("animation draw");
            offScreenImage.getGraphics().drawImage(backgroundImage, 0, 0, null);
            view.draw(model, (Graphics2D) offScreenImage.getGraphics());
        }
        this.image = offScreenImage;
        panel.repaint();
    }

    private MapContent createMap() {
        final MapContent map = new MapContent();
        map.setTitle("Animator");
        map.getViewport();
        map.addLayer(createCoastlineLayer());
        map.addLayer(createExtraFeatures());
        // addWms(map);
        return map;
    }

    private Layer createCoastlineLayer() {
        try {
            // File file = new File(
            // "/home/dxm/Downloads/shapefile-australia-coastline-polygon/cstauscd_r.shp");
            File file = new File("src/main/resources/shapes/countries.shp");
            FileDataStore store = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource featureSource = store.getFeatureSource();

            Style style = SLD.createSimpleStyle(featureSource.getSchema());
            Layer layer = new FeatureLayer(featureSource, style);
            return layer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Layer createExtraFeatures() {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("Location");
        b.setCRS(DefaultGeographicCRS.WGS84);
        // picture location
        b.add("geom", Point.class);
        final SimpleFeatureType TYPE = b.buildFeatureType();

        GeometryFactory gf = JTSFactoryFinder.getGeometryFactory();
        Point point = gf.createPoint(new Coordinate(CANBERRA_LONG, CANBERRA_LAT));

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(TYPE);
        builder.add(point);
        SimpleFeature feature = builder.buildFeature("Canberra");
        DefaultFeatureCollection features = new DefaultFeatureCollection(null, null);
        features.add(feature);

        Style style = SLD.createPointStyle("Star", Color.BLUE, Color.BLUE, 0.3f, 10);

        return new FeatureLayer(features, style);
    }

    static void addWms(MapContent map) {
        // URL wmsUrl = WMSChooser.showChooseWMS();

        WebMapServer wms;
        try {
            String url = "http://129.206.228.72/cached/osm?Request=GetCapabilities";
            // String url = "http://sarapps.amsa.gov.au:8080/cts-gis/wms";
            wms = new WebMapServer(new URL(url));
        } catch (ServiceException | IOException e) {
            throw new RuntimeException(e);
        }
        List<org.geotools.data.ows.Layer> wmsLayers = WMSLayerChooser.showSelectLayer(wms);
        for (org.geotools.data.ows.Layer wmsLayer : wmsLayers) {
            System.out.println("adding " + wmsLayer.getTitle());
            WMSLayer displayLayer = new WMSLayer(wms, wmsLayer);
            map.addLayer(displayLayer);
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("http.proxyHost", "proxy.amsa.gov.au");
        System.setProperty("http.proxyPort", "8080");
        System.setProperty("https.proxyHost", "proxy.amsa.gov.au");
        System.setProperty("https.proxyPort", "8080");
        new Animator().start();
    }

}