package au.net.think.toy.mandel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * Zoomer
 */
public class Zoomer {
    public static void main(String[] args) {
        JFrame f = new JFrame("zoomer");
        f.setLayout(new BorderLayout());
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        int fps = 60;
        long period = (long) 1000.0 / fps;
        final MandelZoomPanel zoomPanel = new MandelZoomPanel(period * 1000000);
        f.add(zoomPanel, BorderLayout.CENTER); // milli to nano
        f.add(makeControls(zoomPanel), BorderLayout.SOUTH);
        f.pack();
        f.setVisible(true);
    }

    private static Component makeControls(final MandelZoomPanel zoomPanel) {
        JPanel controls = new JPanel();
        final JTextField scale = new JTextField(Double.toString(zoomPanel.targetScaleFactor), 5);
        controls.add(scale);
        final JTextField xOffset = new JTextField(Double.toString(zoomPanel.targetXOffset), 5);
        controls.add(xOffset);
        final JTextField yOffset = new JTextField(Double.toString(zoomPanel.targetYOffset), 5);
        controls.add(yOffset);
        controls.add(new JButton(new AbstractAction("Update") {
            public void actionPerformed(final ActionEvent actionEvent) {
                try {
                    double s = Double.parseDouble(scale.getText());
                    double x = Double.parseDouble(xOffset.getText());
                    double y = Double.parseDouble(yOffset.getText());
                    zoomPanel.retarget(s, x, y);
                } catch (NumberFormatException e) {
                    System.out.println("problem parsing number field");
                }
            }
        }));
        controls.add(new JButton(new AbstractAction("Stop") {
            volatile boolean started = true;
            public void actionPerformed(final ActionEvent e) {
                if (started) {
                    putValue(Action.NAME, "Start");
                    zoomPanel.pause();
                } else {
                    putValue(Action.NAME, "Stop");
                    zoomPanel.unPause();
                }
                started = !started;
            }
        }));
        return controls;
    }
}
