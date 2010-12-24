package au.net.think.toy.mandel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * Animate a mandelbrot zoom. TODO: linear zoom requires that the step TODO: export to animated gif! :)
 *
 */
public class MandelZoomPanel extends JPanel implements Runnable {

    private static final long NO_DELAYS_PER_YIELD = 50;
    private static final int MAX_FRAME_SKIPS = 5;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;

    private Thread animator;
    private volatile boolean running = false;
    private BufferedImage dbImage = null;
    private static final int MAX_ITERATIONS = 90;
    private final Color[] colours;
    private final double initScaleFactor = 2.0;
//    private final double initXOffset = 1.5;
    private final double initXOffset = 0.48;
//    private final double initYOffset = 1.0;
    private final double initYOffset = -0.62;
    volatile double targetScaleFactor = 0.0001;
    volatile double targetXOffset = 0.48;
    volatile double targetYOffset = -0.62;

    private volatile double scaleFactor, xOffset, yOffset;
    private volatile boolean zoomIn = true;
    private long period;
    private volatile double scaleFactorStep;
    private volatile double xOffsetStep;
    private volatile double yOffsetStep;
    private volatile boolean step = false;
    private volatile boolean paused = false;

    public MandelZoomPanel(long period) {
        this.period = period;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        colours = new PaletteFactory().argbRainbow(31);
        scaleFactor = initScaleFactor;
        xOffset = initXOffset;
        yOffset = initYOffset;

        setFocusable(true);
        requestFocus();
        addKeyListener(new KeyComand());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        start();
    }

    private void start() {
        init();
        if (animator == null || !running) {
            animator = new Thread(this);
            animator.start();
        }
    }

    void retarget(double sf, double xo, double yo) {
        if (sf < initScaleFactor) {
            targetScaleFactor = sf;
        }
        if (xo < initXOffset) {
            targetXOffset = xo;
        }
        if (yo < initYOffset) {
            targetYOffset = yo;
        }
        init();
        zoomIn = false; // in case target moved above current
    }

    void init() {
        int animSteps = 200;
        scaleFactorStep = (targetScaleFactor - initScaleFactor) / animSteps;
        xOffsetStep = (targetXOffset - initXOffset) / animSteps;
        yOffsetStep = (targetYOffset - initYOffset) / animSteps;
    }

    public void run() {
        long beforeTime, afterTime, timeDiff, sleepTime;
        long overSleepTime = 0L;
        int noDelays = 0;
        long excess = 0L;

        beforeTime = System.nanoTime();
        running = true;
        while (running) {
            update();
            render();
            paintScreen();
            afterTime = System.nanoTime();
            timeDiff = afterTime - beforeTime;
            sleepTime = (period - timeDiff) - overSleepTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime / 1000000L);
                } catch (InterruptedException e) {
                }
                overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
            } else {
                excess -= sleepTime;
                overSleepTime = 0L;
                if (++noDelays >= NO_DELAYS_PER_YIELD) {
                    Thread.yield();
                    noDelays = 0;
                }
            }
            beforeTime = System.nanoTime();
            int skips = 0;
            while ((excess > period) && (skips < MAX_FRAME_SKIPS)) {
                excess -= period;
                update(); // update without a render
                skips++;
            }
        }
        System.exit(0);
    }

    private void render() {
        if (dbImage == null) {
            dbImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        }
        final double scaling = (double) WIDTH / scaleFactor; // set scaling factor relative to width
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                double u = i / scaling - xOffset;
                double v = j / scaling - yOffset;
                double x = u;
                double y = v;
                int n = 0;
                // inner iteration
                double r = 0;
                double q = 0;
                while (r + q < 4 && n < MAX_ITERATIONS) {
                    r = x * x;
                    q = y * y;

                    y = 2 * x * y + v;
                    x = r - q + u;
                    n++;
                }

                Color plotColour;
                if (n == MAX_ITERATIONS) {
                    plotColour = Color.BLACK;
                } else {
                    plotColour = colours[n % colours.length];
                }

                dbImage.setRGB(i, j, plotColour.getRGB());
            }
        }
    }

    private void update() {
        if (step || !paused) {
            if (zoomIn) {
                if (scaleFactor > targetScaleFactor) {
                    scaleFactor += scaleFactorStep * scaleFactor;
                }
                if (xOffset > targetXOffset) {
                    xOffset += xOffsetStep;
                }
                if (yOffset > targetYOffset) {
                    yOffset += yOffsetStep;
                }
                if (scaleFactor <= targetScaleFactor && xOffset <= targetXOffset && yOffset <= targetYOffset) {
                    zoomIn = false; // switch to zoom out
                }
            } else {
                if (scaleFactor < initScaleFactor) {
                    scaleFactor -= scaleFactorStep * scaleFactor;
                }
                if (xOffset < initXOffset) {
                    xOffset -= xOffsetStep;
                }
                if (yOffset < initYOffset) {
                    yOffset -= yOffsetStep;
                }
                if (scaleFactor >= initScaleFactor && xOffset >= initXOffset && yOffset >= initYOffset) {
                    zoomIn = true;
                }
            }
            step = false;
        }
    }

    private void paintScreen() {
        Graphics g;
        try {
            g = this.getGraphics();
            if (g != null && dbImage != null) {
                g.drawImage(dbImage, 0, 0, null);
                Toolkit.getDefaultToolkit().sync();
                g.dispose();
            } else {
                System.out.println("errrr...");
            }
        } catch (Exception e) {
            System.out.println("e = " + e);
        }
    }

    private class KeyComand extends KeyAdapter implements KeyListener {
        @Override
        public void keyReleased(final KeyEvent keyEvent) {
            if (keyEvent.getKeyChar() == 'p') {
                paused = !paused;
            } else if (keyEvent.getKeyChar() == 's' && paused) {
                step = true;
            }
        }
    }
}
