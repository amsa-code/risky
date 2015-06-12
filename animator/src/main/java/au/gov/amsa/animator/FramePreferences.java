package au.gov.amsa.animator;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.JFrame;

public final class FramePreferences {

    public static void restoreLocationAndSize(JFrame frame, int defaultX, int defaultY,
            int defaultWidth, int defaultHeight, Class<?> cls) {
        Preferences prefs = Preferences.userNodeForPackage(Animator.class);
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

}
