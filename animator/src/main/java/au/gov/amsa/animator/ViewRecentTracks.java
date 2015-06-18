package au.gov.amsa.animator;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import au.gov.amsa.risky.format.Fix;

public class ViewRecentTracks implements View {

    @Override
    public void draw(Model model, Graphics2D g, AffineTransform worldToScreen) {
        if (worldToScreen == null)
            return;
        long n = model.stepNumber();
        int size = 20;
        long r = n % size;
        if (r > size / 2)
            r = size - r;
        g.setColor(Color.red);
        // g.drawString("Hello", 100 + r, 100 + r);
        // g.drawString("there", 200 + r, 150 - r);
        // g.drawString("how", 130 + r, 180 - r);
        Point2D.Float p = toScreen(worldToScreen, -35.25f, 149.0f);
        g.drawString("Canberra", p.x - size / 4 + r, p.y - size / 4 + r);
        g.setColor(Color.BLUE);
        Map<Long, Collection<Fix>> fixGroups = model.recent();

        for (Collection<Fix> fixes : fixGroups.values()) {
            Long lastTime = null;
            for (Fix fix : fixes) {
                lastTime = fix.time();
            }
            Point2D.Float previous = null;
            int i = 1;
            for (Fix fix : fixes) {
                if (lastTime == null || fixes.size() == 1
                        || fix.time() + TimeUnit.HOURS.toMillis(1) > lastTime) {
                    g.setColor(Color.getHSBColor(0.7833f,
                            (float) Math.pow(i / (double) fixes.size(), 2), 1f));
                    Point2D.Float position = toScreen(worldToScreen, fix.lat(), fix.lon());
                    if (previous != null) {
                        g.drawLine(Math.round(previous.x), Math.round(previous.y),
                                Math.round(position.x), Math.round(position.y));
                    }
                    previous = position;
                    if (i == fixes.size()) {
                        final int sz = 2;
                        g.drawArc(Math.round(position.x - sz / 2), Math.round(position.y - sz / 2),
                                sz, sz, 0, 360);
                    }
                }
                i++;
            }
        }
    }

    static Point2D.Float toScreen(AffineTransform worldToScreen, float lat, float lon) {
        Point2D.Float a = new Point2D.Float(lon, lat);
        Point2D.Float b = new Point2D.Float();
        worldToScreen.transform(a, b);
        return b;
    }
}
