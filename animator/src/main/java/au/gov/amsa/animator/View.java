package au.gov.amsa.animator;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

public interface View {

    void draw(Model model, Graphics2D g, AffineTransform worldToScreen);

}