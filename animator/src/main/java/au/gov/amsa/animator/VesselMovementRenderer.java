package au.gov.amsa.animator;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.lite.StreamingRenderer;

public class VesselMovementRenderer extends StreamingRenderer {

    volatile int n = 0;

    @Override
    public void paint(Graphics2D g, Rectangle paintArea, ReferencedEnvelope mapArea,
            AffineTransform worldToScreen) {
        super.paint(g, paintArea, mapArea, worldToScreen);
        System.out.println("drawing");
        Point2D.Float d = new Point2D.Float();
        worldToScreen.transform(new Point2D.Float(149.1244f, -35.3075f), d);
        g.drawString("Canberra", d.x - (n % 10) * 10, d.y - (n % 10) * 10);
    }

    public void next() {
        n = n + 1;
    }
}
