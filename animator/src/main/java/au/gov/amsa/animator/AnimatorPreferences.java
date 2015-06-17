package au.gov.amsa.animator;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

public class AnimatorPreferences {

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
