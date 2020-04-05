package au.gov.amsa.animator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;

public class ImageDemo {

    public static void main(String[] args) throws IOException {
        // Coastlines, country borders and a star on Canberra
        MapContent map = Map.createMap();

        // set the view bounds
        double minLon = 90;
        double maxLon = 175;
        double minLat = -50;
        double maxLat = 0;
        ReferencedEnvelope bounds = new ReferencedEnvelope(minLon, maxLon, minLat, maxLat, DefaultGeographicCRS.WGS84);

        // just set the width of the output image, the height will be set so that
        // referenced envelope fits exactly in the image bounds
        int width = 800;
        double ratio = (maxLat - minLat) / (maxLon - minLon);
        int proportionalHeight = (int) Math.round(width * ratio);

        // create a buffered image
        Rectangle imageBounds = new Rectangle(0, 0, width, proportionalHeight);
        BufferedImage image = createImage(imageBounds);
        Graphics2D gr = image.createGraphics();
        gr.setPaint(Color.WHITE);
        gr.fill(imageBounds);
        
        // paint the MapContent on to the image
        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(map);
        renderer.paint(gr, imageBounds, bounds);
        
        // the transform converts lat, long to image coordinates
        AffineTransform worldToScreen = RendererUtilities.worldToScreenTransform(bounds,
                new Rectangle(0, 0, image.getWidth(), image.getHeight()));

        // Draw the string Canberra
        Float p = toScreen(worldToScreen, -35.2809, 149.1300);
        gr.setColor(Color.blue);
        gr.drawString("Canberra", p.x + 5, p.y);
        
        // output the buffered image to a file
        ImageIO.write(image, "png", new File("target/image.png"));
    }

    private static BufferedImage createImage(Rectangle imageBounds) {
        BufferedImage img = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setBackground(Color.white);
        return img;
    }

    private static Point2D.Float toScreen(AffineTransform worldToScreen, double lat, double lon) {
        Point2D.Float a = new Point2D.Float((float) lon, (float) lat);
        Point2D.Float b = new Point2D.Float();
        worldToScreen.transform(a, b);
        return b;
    }

}
