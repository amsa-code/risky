package au.gov.amsa.animator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
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
import org.geotools.renderer.lite.RendererUtilities;
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
    private final Model model;
    private final View view = new View();
    private volatile BufferedImage image;
    private volatile BufferedImage backgroundImage;
    private volatile ReferencedEnvelope bounds;
    final JPanel panel = createMapPanel();
    final MapContent map;
    private final SubscriptionList subscriptions;
    private final Worker worker;
    private volatile BufferedImage offScreenImage;
    private volatile AffineTransform worldToScreen;

    public Animator(Model model) {
        this.model = model;
        map = createMap();
        bounds = new ReferencedEnvelope(90, 175, -50, 0, DefaultGeographicCRS.WGS84);
        subscriptions = new SubscriptionList();
        worker = Schedulers.newThread().createWorker();
        subscriptions.add(worker);
    }

    ReferencedEnvelope getBounds() {
        return bounds;
    }

    private JPanel createMapPanel() {
        final JPanel panel = new JPanel() {
            private static final long serialVersionUID = 3824694997015022298L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, null);
            }
        };
        MouseAdapter listener = createMouseListener();
        panel.addMouseListener(listener);
        panel.addMouseWheelListener(listener);
        return panel;
    }

    private MouseAdapter createMouseListener() {
        return new MouseAdapter() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                Point2D.Float p = toWorld(e);
                boolean zoomIn = notches < 0;
                for (int i = 0; i < Math.min(Math.abs(notches), 8); i++) {
                    if (zoomIn)
                        zoom(p, 0.9);
                    else
                        zoom(p, 1.1);
                }
                worker.schedule(() -> {
                    redrawAll();
                }, 100, TimeUnit.MILLISECONDS);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                boolean shiftDown = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK;
                Point2D.Float p = toWorld(e);
                if (e.getClickCount() == 2) {
                    if (shiftDown) {
                        // zoom out centred on p
                        zoom(p, 2.5);
                    } else {
                        // zoom in centred on p
                        zoom(p, 0.4);
                    }
                    redrawAll();
                } else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                    System.out.println(p.getX() + " " + p.getY());
                }
            }

            private void zoom(Point2D.Float p, double factor) {
                double w = bounds.getWidth() * factor;
                double h = bounds.getHeight() * factor;
                if (w >= map.getMaxBounds().getWidth() || h >= map.getMaxBounds().getHeight())
                    bounds = map.getMaxBounds();
                bounds = new ReferencedEnvelope(p.getX() - w / 2, p.getX() + w / 2, p.getY() - h
                        / 2, p.getY() + h / 2, bounds.getCoordinateReferenceSystem());
            }

            private Point2D.Float toWorld(MouseEvent e) {
                Point2D.Float a = new Point2D.Float(e.getX(), e.getY());
                Point2D.Float b = new Point2D.Float();
                try {
                    worldToScreen.inverseTransform(a, b);
                } catch (NoninvertibleTransformException e1) {
                    throw new RuntimeException(e1);
                }
                return b;
            }

        };
    }

    public void start() {
        SwingScheduler.getInstance().createWorker().schedule(() -> {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            synchronized (panel) {
                frame.setContentPane(panel);
            }
            FramePreferences.restoreLocationAndSize(frame, 100, 100, 800, 600, Animator.class);
            bounds = FramePreferences.restoreBounds(90, 175, -50, 0, frame, Animator.this);
            frame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    redrawAll();
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    super.componentShown(e);
                    redrawAll();
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

    private void redrawAll() {
        backgroundImage = null;
        redraw();
    }

    private synchronized void redraw() {

        if (backgroundImage == null) {
            // get the frame width and height
            int width = panel.getParent().getWidth();
            double ratio = bounds.getHeight() / bounds.getWidth();
            int proportionalHeight = (int) Math.round(width * ratio);
            Rectangle imageBounds = new Rectangle(0, 0, width, proportionalHeight);
            image = createImage(imageBounds);
            BufferedImage backgroundImage = createImage(imageBounds);
            Graphics2D gr = backgroundImage.createGraphics();
            gr.setPaint(Color.WHITE);
            gr.fill(imageBounds);
            StreamingRenderer renderer = new StreamingRenderer();
            renderer.setMapContent(map);
            renderer.paint(gr, imageBounds, bounds);
            this.backgroundImage = backgroundImage;
            this.offScreenImage = createImage(imageBounds);
            worldToScreen = RendererUtilities.worldToScreenTransform(bounds, new Rectangle(0, 0,
                    backgroundImage.getWidth(), backgroundImage.getHeight()));
        }
        redrawAnimationLayer();

    }

    private static BufferedImage createImage(Rectangle imageBounds) {
        BufferedImage img = new BufferedImage(imageBounds.width, imageBounds.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setBackground(Color.white);
        return img;
    }

    private synchronized void redrawAnimationLayer() {
        // if (backgroundImage != null && offscreenImage != null) {
        if (offScreenImage != null) {
            offScreenImage.getGraphics().drawImage(backgroundImage, 0, 0, null);
            view.draw(model, (Graphics2D) offScreenImage.getGraphics(), worldToScreen);
            BufferedImage temp = offScreenImage;
            offScreenImage = image;
            image = temp;
        }
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
        new Animator(new ModelManyCraft()).start();
    }

}