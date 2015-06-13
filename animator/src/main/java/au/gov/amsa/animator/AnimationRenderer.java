package au.gov.amsa.animator;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;

import org.geotools.swing.JMapPane;

public class AnimationRenderer {

	public void render(Image image, Model model, JMapPane mapPane,
			BackgroundRenderer renderer) {
		Graphics2D g = (Graphics2D) image.getGraphics();
		mapPane.paint(g);
		System.out.println("drawing");
		Point2D.Float d = new Point2D.Float();
		renderer.worldToScreen().transform(
				new Point2D.Float(149.1244f, -35.3075f), d);
		g.drawString("Canberra", d.x, d.y);
	};

}
