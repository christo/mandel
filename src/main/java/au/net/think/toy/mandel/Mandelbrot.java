package au.net.think.toy.mandel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Mandelbrot with crappy controls and a non-working zoomer.
 *
 * @author christo
 * @since 17/05/2008
 */
public class Mandelbrot {

    //sFactor: 0.017037102559001212 xOffset: 0.13 yOffset: 0.9000000000000004
    private static double targetSFactor = 0.017037102559001212;
    private static double targetXOffset = 0.013;
    private static double targetYOffset = 0.9;


    private final BufferedImage image;

    private static final Color[] COLORS;

    static {
        COLORS = generateColors(70);
    }

    private static Color[] generateColors(final int howmany) {
        Color[] colors = new Color[howmany];
        int white = 0xffffff; // 24 bit ignoring alpha
        int step = white / howmany;
        for(int colour = 0, index = 0; index < howmany; colour += step) {
            colors[index++] = new Color(colour);
        }
        return colors;
    }

    private MandelbrotPanel panel;
    private final int width;
    private final int height;

    public double getSFactor() {
        return sFactor;
    }

    public void setSFactor(final double sFactor) {
        this.sFactor = sFactor;
    }

    private volatile double sFactor = 2.0;
    private volatile double xOffset = 1.5;
    private volatile double yOffset = 1.0;

    private static final int MAX_ITERATIONS = 200;

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        final Mandelbrot m = new Mandelbrot(300, 300);
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                m.createGui();
            }
        });
        m.render();
    }

    public Mandelbrot(int width, int height) {
        this.width = width;
        this.height = height;
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    private void createGui() {
        JFrame frame = new JFrame("mandelbrot");
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent keyEvent) {
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        yOffset += 0.01;
                        render();
                        break;
                    case KeyEvent.VK_DOWN:
                        yOffset -= 0.01;
                        render();
                        break;
                    case KeyEvent.VK_LEFT:
                        xOffset += 0.01;
                        render();
                        break;
                    case KeyEvent.VK_RIGHT:
                        xOffset -= 0.01;
                        render();
                        break;
                    case KeyEvent.VK_Z:
                        zoom();
                        break;
                    default:
                        System.out.println("not listening to that key " + keyEvent.getKeyCode());
                }
            }
        });
        JPanel content = new JPanel();
        panel = new MandelbrotPanel();
        panel.setDoubleBuffered(true);
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        panel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(final MouseEvent mouseEvent) {
                final Point point = mouseEvent.getPoint();
//                System.out.println("point = " + point);
                if (point.getY() < height / 3) {
                   yOffset += 0.1;
                }
                if (point.getY() > height * 2 / 3) { 
                   yOffset -= 0.1;
                }
                if (point.getX() < width / 3) {
                   xOffset = 0.1;
                }
                if (point.getX() > width * 2 / 3) { 
                   xOffset -= 0.1;
                }
                int dir = mouseEvent.isShiftDown() ? -1 : 1; // shift reverses
                sFactor /= 1.1;
                render();
            }
        });
        content.add(panel, BorderLayout.CENTER);
        //content.add(createControlPanel(), BorderLayout.EAST);
        frame.setContentPane(content);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void zoom() {
        int steps = 30;
        double sFactorStep = (targetSFactor - sFactor) / steps;
        double xOffsetStep = (targetXOffset - xOffset) / steps;
        double yOffsetStep = (targetYOffset - yOffset) / steps;
        while(steps-- > 0) {
            sFactor += sFactorStep;
            xOffset += xOffsetStep;
            yOffset += yOffsetStep;
            render();

            panel.repaint();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        
    }

    private void render() {
        //dump();
        final double scaling = (double) width / sFactor; // set scaling factor relative to width
        long time0 = System.currentTimeMillis();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
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

                Color plotColour = Color.BLACK;
                plotColour = COLORS[n % COLORS.length];
                if (n > COLORS.length) {
                    //long colourNum = Math.round(((n - COLORS.length) * COLORS.length) / (MAX_ITERATIONS - COLORS.length));
                    plotColour = COLORS[n % COLORS.length];
                }

                image.setRGB(i, j, plotColour.getRGB());
            }
            panel.repaint();
        }
        panel.repaint();
        //System.out.println("render time: " + (System.currentTimeMillis() - time0));
    }

    private void dump() {
        System.out.println("sFactor: " + sFactor + " xOffset: " + xOffset + " yOffset: " + yOffset);
    }

    public double getXOffset() {
        return xOffset;
    }

    public void setXOffset(final double xOffset) {
        this.xOffset = xOffset;
    }

    public double getYOffset() {
        return yOffset;
    }

    public void setYOffset(final double yOffset) {
        this.yOffset = yOffset;
    }

    private class MandelbrotPanel extends JPanel {

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, null);
        }

        public Dimension getPreferredSize() {
            return new Dimension(image.getWidth(), image.getHeight());
        }
    }

    interface Updater<T> {
        void update(T value);
    }
}
