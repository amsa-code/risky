package au.gov.amsa.animator;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public interface View {

    void draw(Model model, Graphics2D g, AffineTransform worldToScreen);
    
    static Point2D.Float toScreen(AffineTransform worldToScreen, float lat, float lon) {
        Point2D.Float a = new Point2D.Float(lon, lat);
        Point2D.Float b = new Point2D.Float();
        worldToScreen.transform(a, b);
        return b;
    }

}