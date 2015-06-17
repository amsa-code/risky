package au.gov.amsa.animator;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.JFrame;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

public final class FramePreferences {

    public static void restoreLocationAndSize(JFrame frame, int defaultX, int defaultY,
            int defaultWidth, int defaultHeight, Class<?> cls) {
        Preferences prefs = Preferences.userNodeForPackage(cls);
        int x = Integer.parseInt(prefs.get("frame.x", defaultX + ""));
        int y = Integer.parseInt(prefs.get("frame.y", defaultY + ""));
        int width = Integer.parseInt(prefs.get("frame.width", defaultWidth + ""));
        int height = Integer.parseInt(prefs.get("frame.height", defaultHeight + ""));
        frame.setLocation(x, y);
        frame.setSize(width, height);
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                prefs.put("frame.x", frame.getX() + "");
                prefs.put("frame.y", frame.getY() + "");
                prefs.put("frame.width", frame.getWidth() + "");
                prefs.put("frame.height", frame.getHeight() + "");
            }

        });

    }

    public static ReferencedEnvelope restoreBounds(double defaultX1, double defaultX2,
            double defaultY1, double defaultY2, Window window, Animator animator) {
        Preferences prefs = Preferences.userNodeForPackage(Animator.class);
        double x1 = Double.parseDouble(prefs.get("bounds.x1", defaultX1 + ""));
        double x2 = Double.parseDouble(prefs.get("bounds.x2", defaultX2 + ""));
        double y1 = Double.parseDouble(prefs.get("bounds.y1", defaultY1 + ""));
        double y2 = Double.parseDouble(prefs.get("bounds.y2", defaultY2 + ""));
        window.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                ReferencedEnvelope bounds = animator.getBounds();
                prefs.put("bounds.x1", bounds.getMinX() + "");
                prefs.put("bounds.x2", bounds.getMaxX() + "");
                prefs.put("bounds.y1", bounds.getMinY() + "");
                prefs.put("bounds.y2", bounds.getMaxY() + "");
            }

        });
        return new ReferencedEnvelope(x1, x2, y1, y2, DefaultGeographicCRS.WGS84);
    }
}
