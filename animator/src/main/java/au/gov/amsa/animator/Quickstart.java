package au.gov.amsa.animator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wms.WebMapServer;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.map.WMSLayer;
import org.geotools.ows.ServiceException;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.wms.WMSLayerChooser;

/**
 * Prompts the user for a shapefile and displays the contents on the screen in a
 * map frame.
 * <p>
 * This is the GeoTools Quickstart application used in documentationa and
 * tutorials. *
 */
public class Quickstart {

    /**
     * GeoTools Quickstart demo application. Prompts the user for a shapefile
     * and displays its contents on the screen in a map frame
     */
    public static void main(String[] args) throws Exception {
        // System.setProperty("http.proxyHost", "proxy.amsa.gov.au");
        // System.setProperty("http.proxyPort", "8080");
        // System.setProperty("https.proxyHost", "proxy.amsa.gov.au");
        // System.setProperty("https.proxyPort", "8080");
        File file = new File(
                "/home/dxm/Downloads/shapefile-australia-coastline-polygon/cstauscd_r.shp");

        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        SimpleFeatureSource featureSource = store.getFeatureSource();

        // Create a map context and add our shapefile to it
        MapContent map = new MapContent();

        map.setTitle("Animator");
        Style style = SLD.createSimpleStyle(featureSource.getSchema());
        Layer layer = new FeatureLayer(featureSource, style);
        map.addLayer(layer);
        // addWms(map);

        // Now display the map
        JMapFrame.showMap(map);

    }

    private static void addWms(MapContent map) {
        // URL wmsUrl = WMSChooser.showChooseWMS();

        WebMapServer wms;
        try {
            wms = new WebMapServer(new URL("http://sarapps.amsa.gov.au:8080/cts-gis/wms"));
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

}