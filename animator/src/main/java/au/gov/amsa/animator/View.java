package au.gov.amsa.animator;

import java.awt.Color;
import java.awt.Graphics2D;

public class View {

    public void draw(Model model, Graphics2D g) {
        long n = model.timeStep;
        long r = n % 100;
        if (r > 50)
            r = 100 - r;
        g.setColor(Color.red);
        g.drawString("Hello", 100 + r, 100 + r);
        g.drawString("there", 200 + r, 150 - r);
        g.drawString("how", 130 + r, 180 - r);

    }
}
