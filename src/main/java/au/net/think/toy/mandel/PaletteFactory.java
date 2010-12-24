package au.net.think.toy.mandel;

import java.awt.Color;

/**
 * TODO: Document this class / interface here
 *
 * @since v4.0
 */
public class PaletteFactory {
    /**
     * Creates a rainbow for {@link BufferedImage.TYPE_INT_ARGB}
     * @param number 
     * @return
     */
    public Color[] argbRainbow(int number) {
        Color[] colors = new Color[number];
        int white = 0xffffff; // 24 bit ignoring alpha
        int step = white / number;
        for(int colour = 0, index = 0; index < number; colour += step) {
            colors[index++] = new Color(colour);
        }
        return colors;
    }
}
