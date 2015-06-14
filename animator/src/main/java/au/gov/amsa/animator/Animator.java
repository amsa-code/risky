package au.gov.amsa.animator;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wms.WebMapServer;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
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
import org.geotools.swing.JMapPane;
import org.geotools.swing.RenderingExecutorEvent;
import org.geotools.swing.RenderingExecutorListener;
import org.geotools.swing.event.MapMouseAdapter;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.wms.WMSLayerChooser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import rx.Observable;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class Animator {

    private static final float CANBERRA_LAT = -35.3075f;
    private static final float CANBERRA_LONG = 149.1244f;
    private final Model model = new Model();
    private final View view = new View();
    private volatile int width, height = 0;
    private volatile Image offScreenImage;
    private volatile JMapPane mapPane;
    private volatile BufferedImage backgroundImage;

    public void start() {
        // Create a map context and add our shapefile to it
        final MapContent map = createMap();

        // Now display the map using the custom renderer
        // display(map);

        // System.exit(0);
        width = 800;
        height = 600;

        Rectangle imageBounds = new Rectangle(0, 0, width, height);
        BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = image.createGraphics();
        gr.setPaint(Color.WHITE);
        gr.fill(imageBounds);
        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(map);
        ReferencedEnvelope mapBounds = map.getMaxBounds();
        renderer.paint(gr, imageBounds, mapBounds);
        backgroundImage = image;
        JFrame frame = new JFrame();
        final JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, null);
            }
        };
        panel.setPreferredSize(new Dimension(width, height));
        frame.add(panel);
        frame.setSize(width, height);
        frame.setVisible(true);

        // // animate
        // offScreenImage = createImage(width, height);
        // long timeStep = 0;
        // long frameMs = 50;
        // while (true) {
        // long t = System.currentTimeMillis();
        // // mapPane.repaint();
        // model.updateModel(timeStep);
        // view.draw(model, offScreenImage);
        // timeStep++;
        // sleep(Math.max(0, t + frameMs - System.currentTimeMillis()));
        // }
    }

    private RenderingExecutorListener createListener(CountDownLatch latch) {
        return new RenderingExecutorListener() {

            @Override
            public void onRenderingStarted(RenderingExecutorEvent ev) {

            }

            @Override
            public void onRenderingFailed(RenderingExecutorEvent ev) {

            }

            @Override
            public void onRenderingCompleted(RenderingExecutorEvent ev) {
                latch.countDown();
            }
        };
    }

    private static BufferedImage createImage(int width, int height) {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(width, height, Transparency.TRANSLUCENT);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    private void display(final MapContent map) {
        EventQueue.invokeLater(() -> {
            // setup custom rendering over the top of the map
                BackgroundRenderer renderer = new BackgroundRenderer();
                mapPane = new JMapPane(map) {
                    int n = 0;

                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (renderer.worldToScreen() != null) {
                            Graphics2D g2 = (Graphics2D) g;
                            n++;
                            Point2D.Float p = new Point2D.Float(CANBERRA_LONG, CANBERRA_LAT);
                            Point2D.Float q = new Point2D.Float();
                            renderer.worldToScreen().transform(p, q);
                            g2.drawString("Hello", q.x + n % 100, q.y + n % 100);
                        }
                    }
                };
                final JMapFrame frame = new JMapFrame(map, mapPane);
                frame.getMapPane().setRenderer(renderer);
                frame.enableStatusBar(true);
                frame.enableToolBar(true);
                frame.initComponents();
                frame.setSize(800, 600);
                FramePreferences.restoreLocationAndSize(frame, 100, 100, 800, 600, Animator.class);
                frame.getMapPane().addMouseListener(new MapMouseAdapter() {

                    @Override
                    public void onMouseClicked(MapMouseEvent event) {
                        DirectPosition2D p = event.getWorldPos();
                        System.out.println(p);
                    }

                });

                frame.setVisible(true);
                Observable.interval(20, TimeUnit.MILLISECONDS).forEach(n -> {
                    frame.getMapPane().repaint();
                });
            });
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