package au.gov.amsa.geo.distance;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.gov.amsa.geo.distance.DistanceTravelledCalculator.CalculationResult;
import au.gov.amsa.geo.model.Bounds;
import au.gov.amsa.geo.model.Options;
import rx.Observable;

/**
 * Swing application that allows viewing of traffic density plots. Supports
 * zooming via double-click.
 *
 */
public class DisplayPanel extends JPanel {

	private static final long serialVersionUID = 7844558863774822599L;

	private static Logger log = LoggerFactory.getLogger(DisplayPanel.class);

	private Options options;
	private int numStandardDeviations;

	private CalculationResult calculationResult;

	public DisplayPanel(int numberStandardDeviations, final CellsUpdater updater) {
		this.numStandardDeviations = numberStandardDeviations;
		setPreferredSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
		setBackground(Color.white);
		setFocusable(true);
		addKeyListener(createKeyListener());
		addMouseListener(createMouseListener(updater));
	}

	public void setCalculationResult(CalculationResult calculationResult,
			Options options) {
		this.calculationResult = calculationResult;
		this.options = options;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics gOld) {
		super.paintComponent(gOld);
		Graphics2D g = (Graphics2D) gOld;
		Renderer.paintAll(g, options, numStandardDeviations, getWidth(),
				getHeight(), calculationResult, true, true);
	}

	public static interface CellsUpdater {

		void update(DisplayPanel display, Bounds bounds);
	}

	private KeyListener createKeyListener() {
		return new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {

			}

			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				if (keyCode == 49) {
					numStandardDeviations = 1;
					repaint();
				} else if (keyCode == 50) {
					numStandardDeviations = 2;
					repaint();
				} else if (keyCode == 51) {
					numStandardDeviations = 3;
					repaint();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {

			}
		};
	}

	private MouseListener createMouseListener(final CellsUpdater updater) {
		return new MouseListener() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2) {
					log.info("double clicked");
					Thread t = new Thread(new Runnable() {

						@Override
						public void run() {
							log.info("updating cells");
							double propX = (double) e.getX() / getWidth();
							double propY = (double) e.getY() / getHeight();
							double zoomFactor = 0.5;
							Bounds b = options.getBounds();
							double topLeftLat = b.getTopLeftLat() - propY
									* (b.getHeightDegrees()) + zoomFactor
									* b.getHeightDegrees() / 2;
							double topLeftLon = b.getTopLeftLon() + propX
									* b.getWidthDegrees() - zoomFactor
									* b.getWidthDegrees() / 2;
							double bottomRightLat = topLeftLat - zoomFactor
									* b.getHeightDegrees();
							double bottomRightLon = topLeftLon + zoomFactor
									* b.getWidthDegrees();
							updater.update(DisplayPanel.this, new Bounds(
									topLeftLat, topLeftLon, bottomRightLat,
									bottomRightLon));
							repaint();
						}
					});
					t.start();
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {

			}

			@Override
			public void mouseReleased(MouseEvent e) {

			}

			@Override
			public void mouseEntered(MouseEvent e) {

			}

			@Override
			public void mouseExited(MouseEvent e) {

			}
		};
	}

	public void run() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				final JFrame f = new JFrame("Vessel Traffic Density");
				// Sets the behavior for when the window is closed
				f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				// Add a layout manager so that the button is not placed on top
				// of the
				// label
				f.setLayout(new FlowLayout());
				// // Add a label and a button
				// f.add(new JLabel("Hello, world!"));
				// f.add(new JButton("Press me!"));
				f.add(DisplayPanel.this);
				// Arrange the components inside the window
				f.pack();
				// By default, the window is not visible. Make it visible.
				f.setVisible(true);

				f.addComponentListener(new java.awt.event.ComponentAdapter() {
					@Override
					public void componentResized(ComponentEvent e) {
						DisplayPanel.this.setPreferredSize(new Dimension(f
								.getWidth(), f.getHeight()));
					}
				});
			}
		});

	}

	public static void displayGui(final Observable<File> files,
			final Options options, final CalculationResult calculationResult) {
		final DisplayPanel display = new DisplayPanel(2, new CellsUpdater() {
			@Override
			public void update(DisplayPanel display, Bounds bounds) {
				// decrease cell size by 0.8 beyond the normal proportional
				// decrease
				double cellSizeDegrees = bounds.getWidthDegrees()
						/ options.getBounds().getWidthDegrees()
						* options.getCellSizeDegreesAsDouble() * 0.8;
				Options o2 = Options.builder().cellSizeDegrees(cellSizeDegrees)
						.bounds(bounds).build();
				display.setCalculationResult(DistanceTravelledCalculator
						.calculateTrafficDensity(o2, files), o2);
			}
		});
		display.setCalculationResult(calculationResult, options);
		display.run();
	}

}
