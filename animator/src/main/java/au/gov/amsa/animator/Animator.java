package au.gov.amsa.animator;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.swing.Timer;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wms.WebMapServer;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.map.WMSLayer;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.event.MapMouseAdapter;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.wms.WMSLayerChooser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class Animator {

    volatile Timer timer;

    public void start() {
        // Create a map context and add our shapefile to it
        final MapContent map = createMap();

        // Now display the map using the custom renderer
        display(map);
    }

    private MapContent createMap() {
        final MapContent map = new MapContent();
        map.setTitle("Animator");
        map.addLayer(createCoastlineLayer());
        map.addLayer(createExtraFeatures());
        // addWms(map);
        return map;
    }

    private void display(final MapContent map) {
        EventQueue.invokeLater(() -> {
            // setup custom rendering over the top of the map
                VesselMovementRenderer renderer = new VesselMovementRenderer();
                final JMapFrame frame = new JMapFrame(map);
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
                timer = new Timer(1000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        System.out.println("timer");
                        renderer.next();
                        frame.getMapPane().repaint();
                    }
                });
                timer.start();
            });
    }

    private Layer createCoastlineLayer() {
        try {
            File file = new File(
                    "/home/dxm/Downloads/shapefile-australia-coastline-polygon/cstauscd_r.shp");

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
        Point point = gf.createPoint(new Coordinate(149.1244, -35.3075));

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