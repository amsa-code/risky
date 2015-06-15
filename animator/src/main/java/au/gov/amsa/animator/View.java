package au.gov.amsa.animator;

import java.awt.Graphics2D;

public class View {

    public void draw(Model model, Graphics2D g) {
        long n = model.timeStep;
        long r = n % 100;
        if (r > 50)
            r = 100 - r;
        System.out.println("drawing hello r=" + r);
        g.drawString("Hello", 100 + r, 100 + r);
    }
}
