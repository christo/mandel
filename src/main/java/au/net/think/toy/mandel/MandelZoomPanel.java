package au.net.think.toy.mandel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.JPanel;

/**
 * Animate a mandelbrot zoom. TODO: linear zoom requires that the step TODO: export to animated gif! :)
 */
public class MandelZoomPanel extends JPanel implements Runnable {

    private static final long NO_DELAYS_PER_YIELD = 50;
    private static final int MAX_FRAME_SKIPS = 5;
    private static final int WIDTH = 1200;
    private static final int HEIGHT = 1000;
    private static final int THREADS = 2;

    private Thread animator;
    private volatile boolean running = false;
    private Renderer[] renderers;
    private BufferedImage bgImage = null;

    private static final int MAX_ITERATIONS = 90;
    private final Color[] colours;
    private final double initScaleFactor = 2.0;
    //    private final double initXOffset = 1.5;
    private final double initXOffset = 0.48;
    //    private final double initYOffset = 1.0;
    private final double initYOffset = -0.62;
    private final ExecutorService executor;

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
        executor = Executors.newFixedThreadPool(THREADS);
        setFocusable(true);
        requestFocus();
        addKeyListener(new KeyComand());

        if (HEIGHT % THREADS != 0) {
            throw new IllegalStateException("height needs to be an even multiple of the number of threads at the moment");
        }
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
        if (bgImage == null) {
            bgImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        }
        if (renderers == null) {
            renderers = new Renderer[THREADS];
            int per = HEIGHT / THREADS;
            for (int i = 0; i < renderers.length; i++) {
                int subframeOffset = i * per;
                renderers[i] = new Renderer(i, WIDTH, per, subframeOffset, MAX_ITERATIONS, colours);
            }
        }

        final double scaling = (double) WIDTH / scaleFactor; // set scaling factor relative to width
        // farm off to renderers and then wait on them
        List<Future<Renderer>> futures = new ArrayList<Future<Renderer>>(renderers.length);

        for (Renderer renderer : renderers) {
            renderer.setScaling(scaling);
            renderer.setxOffset(xOffset);
            renderer.setyOffset(yOffset);
            futures.add(executor.submit(renderer));
        }
        try {
            for (Future<Renderer> future : futures) {
                renderSubframe(future.get());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * put the renderer into its part in the whole
     *
     * @param renderer the renderer.
     */
    private void renderSubframe(Renderer renderer) {
        final Graphics graphics = bgImage.getGraphics();
        graphics.drawImage(renderer.getImage(), 0, renderer.getSubHeightOffset(), null);
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
            if (g != null && bgImage != null) {
                g.drawImage(bgImage, 0, 0, null);
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
