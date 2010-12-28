package au.net.think.toy.mandel;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Small repeatable paramatisable command that renders a subframe.
 */
public class Renderer implements Callable<Renderer> {
    private final int subframeIndex;
    private final int maxIterations;
    private final Color[] colours;
    private final int width;
    private final int height;
    private final int subHeightOffset;
    private final BufferedImage image;
    private double scaling;
    private double xOffset;
    private double yOffset;

    public Renderer(final int subframeIndex, int width, int height, int subHeightOffset, int maxIterations, Color[] colours) {
        this.subframeIndex = subframeIndex;
        this.width = width;
        this.height = height;
        this.subHeightOffset = subHeightOffset;
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.maxIterations = maxIterations;
        this.colours = colours;
        System.out.println(this.toString());
    }

    public void render() {
        for (int i = 0; i < width; i++) {
            for (int j = subHeightOffset; j < height + subHeightOffset; j++) {
                double u = i / scaling - xOffset;
                double v = j / scaling - yOffset;
                double x = u;
                double y = v;
                int n = 0;
                // inner iteration
                double r = 0;
                double q = 0;
                while (r + q < 4 && n < maxIterations) {
                    r = x * x;
                    q = y * y;

                    y = 2 * x * y + v;
                    x = r - q + u;
                    n++;
                }

                Color plotColour;
                if (n == maxIterations) {
                    plotColour = Color.BLACK;
                } else {
                    plotColour = colours[n % colours.length];
                }

                image.setRGB(i, j - subHeightOffset, plotColour.getRGB());
            }
        }
    }

    public Renderer call() throws Exception {
        render();
        return this;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setScaling(final double scaling) {
        this.scaling = scaling;
    }

    public void setxOffset(final double xOffset) {
        this.xOffset = xOffset;
    }

    public void setyOffset(final double yOffset) {
        this.yOffset = yOffset;
    }

    public int getSubHeightOffset() {
        return subHeightOffset;
    }

    @Override
    public String toString() {
        return "Renderer{" +
                "subframeIndex=" + subframeIndex +
                ", maxIterations=" + maxIterations +
                ", width=" + width +
                ", height=" + height +
                ", subHeightOffset=" + subHeightOffset +
                ", scaling=" + scaling +
                ", xOffset=" + xOffset +
                ", yOffset=" + yOffset +
                '}';
    }
}
