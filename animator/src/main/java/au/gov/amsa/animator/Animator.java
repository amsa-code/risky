package au.gov.amsa.animator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;
import org.geotools.map.MapViewport;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StreamingRenderer;

import au.gov.amsa.util.swing.FramePreferences;
import rx.Scheduler.Worker;
import rx.internal.util.SubscriptionList;
import rx.schedulers.Schedulers;
import rx.schedulers.SwingScheduler;

public class Animator {

    private final Model model;
    private final View view;
    private volatile AtomicReference<BufferedImage> backgroundImage = new AtomicReference<>();
    private volatile ReferencedEnvelope bounds;
    private final JPanel panel = createMapPanel();
    private final MapContent map;
    private final SubscriptionList subscriptions;
    private final Worker worker;
    private volatile AffineTransform worldToScreen;

    public Animator(MapContent map, Model model, View view) {
        this.map = map;
        this.model = model;
        this.view = view;
        // default to Australia centred region
        bounds = new ReferencedEnvelope(90, 175, -50, 0, DefaultGeographicCRS.WGS84);
        subscriptions = new SubscriptionList();
        worker = Schedulers.newThread().createWorker();
        subscriptions.add(worker);
    }

    ReferencedEnvelope getBounds() {
        return bounds;
    }

    private JPanel createMapPanel() {
        final JPanel panel = new JPanel() {
            private static final long serialVersionUID = 3824694997015022298L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintMap(g);
            }
        };
        MouseAdapter listener = createMouseListener();
        panel.addMouseListener(listener);
        panel.addMouseWheelListener(listener);
        return panel;
    }
    
    
    private static final RenderingHints HINTS = createRenderingHints();
    
    private static RenderingHints createRenderingHints() {
        java.util.Map<RenderingHints.Key, Object> hints = new HashMap<RenderingHints.Key, Object>();
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return new RenderingHints(hints);
    }

    private void paintMap(Graphics g) {
        BufferedImage bi;
        while (true) {
            int width = panel.getParent().getWidth();
            double ratio = bounds.getHeight() / bounds.getWidth();
            int proportionalHeight = (int) Math.round(width * ratio);
            Rectangle imageBounds = new Rectangle(0, 0, width, proportionalHeight);
            bi = backgroundImage.get();
            if (bi == null) {
                // get the frame width and height
                bi = createImage(imageBounds);
                Graphics2D gr = bi.createGraphics();
                gr.setPaint(Color.WHITE);
                gr.fill(imageBounds);
                StreamingRenderer renderer = new StreamingRenderer();
                MapViewport viewport = new MapViewport();
                viewport.setScreenArea(imageBounds);
                viewport.setCoordinateReferenceSystem(DefaultGeographicCRS.WGS84);
                viewport.setBounds(bounds);
                renderer.setMapContent(map);
                renderer.setJava2DHints(HINTS);
                map.setViewport(viewport);
                renderer.paint(gr, imageBounds, bounds);
                this.worldToScreen = RendererUtilities.worldToScreenTransform(bounds,
                        imageBounds);
                if (this.backgroundImage.compareAndSet(null, bi)) {
                    break;
                }
            } else {
                break;
            }
        }
        g.drawImage(bi, 0, 0, null);
        view.draw(model, (Graphics2D) g, worldToScreen);
    }

    private MouseAdapter createMouseListener() {
        return new MouseAdapter() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                Point2D.Float p = toWorld(e);
                boolean zoomIn = notches < 0;
                for (int i = 0; i < Math.min(Math.abs(notches), 8); i++) {
                    if (zoomIn)
                        zoom(p, 0.9);
                    else
                        zoom(p, 1.1);
                }
                worker.schedule(() -> {
                    redrawAll();
                }, 50, TimeUnit.MILLISECONDS);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                boolean shiftDown = (e.getModifiersEx()
                        & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK;
                Point2D.Float p = toWorld(e);
                if (e.getClickCount() == 2) {
                    if (shiftDown) {
                        // zoom out centred on p
                        zoom(p, 2.5);
                    } else {
                        // zoom in centred on p
                        zoom(p, 0.4);
                    }
                    redrawAll();
                } else if (e.getClickCount() == 1 && e.getButton() == MouseEvent.BUTTON1) {
                    System.out.println("click world=" + p.getX() + " " + p.getY());
                }
            }

            private void zoom(Point2D.Float p, double factor) {
                double minX = p.getX() - (p.getX() - bounds.getMinX()) * factor;
                double maxX = p.getX() + (bounds.getMaxX() - p.getX()) * factor;
                double minY = p.getY() - (p.getY() - bounds.getMinY()) * factor;
                double maxY = p.getY() + (bounds.getMaxY() - p.getY()) * factor;
                if ((maxX - minX) >= map.getMaxBounds().getWidth()
                        || (maxY - minY) >= map.getMaxBounds().getHeight())
                    bounds = map.getMaxBounds();
                bounds = new ReferencedEnvelope(minX, maxX, minY, maxY,
                        bounds.getCoordinateReferenceSystem());
            }

            private Point2D.Float toWorld(MouseEvent e) {
                Point2D.Float a = new Point2D.Float(e.getX(), e.getY());
                Point2D.Float b = new Point2D.Float();
                try {
                    worldToScreen.inverseTransform(a, b);
                } catch (NoninvertibleTransformException e1) {
                    throw new RuntimeException(e1);
                }
                return b;
            }

        };
    }

    public void start() {
        SwingScheduler.getInstance().createWorker().schedule(() -> {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            synchronized (panel) {
                frame.setContentPane(panel);
            }
            FramePreferences.restoreLocationAndSize(frame, 100, 100, 800, 600, Animator.class);
            bounds = AnimatorPreferences.restoreBounds(90, 175, -50, 0, frame, Animator.this);
            frame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    redrawAll();
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    super.componentShown(e);
                    redrawAll();
                }
            });
            frame.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    Animator.this.close();
                }
            });
            frame.setVisible(true);
        });
        final AtomicInteger timeStep = new AtomicInteger();
        worker.schedulePeriodically(() -> {
            model.updateModel(timeStep.getAndIncrement());
            panel.repaint();
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    private void redrawAll() {
        backgroundImage.set(null);
        panel.repaint();
    }

    private static BufferedImage createImage(Rectangle imageBounds) {
        BufferedImage img = new BufferedImage(imageBounds.width, imageBounds.height,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setBackground(Color.white);
        return img;
    }

    public void close() {
        System.out.println("unsubscribing");
        subscriptions.unsubscribe();
        System.out.println("unsubscribed");
    }

}