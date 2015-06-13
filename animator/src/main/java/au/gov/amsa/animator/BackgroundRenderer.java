package au.gov.amsa.animator;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.lite.StreamingRenderer;

public class BackgroundRenderer extends StreamingRenderer {

	volatile AffineTransform worldToScreen;

	@Override
	public void paint(Graphics2D g, Rectangle paintArea,
			ReferencedEnvelope mapArea, AffineTransform worldToScreen) {
		this.worldToScreen = worldToScreen;
		super.paint(g, paintArea, mapArea, worldToScreen);
	}

	public AffineTransform worldToScreen() {
		return worldToScreen;
	}

}
