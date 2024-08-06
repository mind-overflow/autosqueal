package wtf.beatrice.autosqueal.ui;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.controls.CursorMover;
import wtf.beatrice.autosqueal.listener.CursorMoveListener;
import wtf.beatrice.autosqueal.util.RunnerUtil;
import wtf.beatrice.autosqueal.util.SystemUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Timer;

public class MainWindow
{
    private static final Logger LOGGER = LogManager.getLogger(MainWindow.class);

    private static final int WINDOW_HEIGHT = 700;
    private static final int WINDOW_WIDTH = 800;

    private Timer timerRunner = new Timer();
    private CursorMover cursorMover = new CursorMover();
    private Button toggleButton;

    public void init() {

        JFrame frame = new JFrame();
        frame.setSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        frame.setTitle("autosqueal");
        frame.setResizable(false);

        toggleButton = new Button();
        toggleButton.setBounds(new Rectangle((WINDOW_WIDTH / 2) - 60, WINDOW_HEIGHT - 60, 120, 30));
        toggleButton.addActionListener(e -> toggleRunning());
        frame.add(toggleButton);
        toggleRunning();

        int bordersPx = 10;
        int rescaleRateo = ((WINDOW_WIDTH - (2 * bordersPx)) * 100) / RunnerUtil.SCREEN_WIDTH;
        int rescaleWidth = RunnerUtil.SCREEN_WIDTH * rescaleRateo / 100;
        int rescaleHeight = RunnerUtil.SCREEN_HEIGHT * rescaleRateo / 100;
        JLabel imageLabel = new JLabel(new ImageIcon(getScreenCapture(rescaleWidth, rescaleHeight)));
        imageLabel.setBounds(new Rectangle(bordersPx, bordersPx, rescaleWidth, rescaleHeight));
        frame.add(imageLabel);

        Image preciseScreenshot = getPreciseScreenshot();
        JLabel timestampLabel = new JLabel(new ImageIcon(preciseScreenshot));
        timestampLabel.setBounds(new Rectangle(bordersPx, bordersPx + rescaleHeight + bordersPx, 100, 30));
        frame.add(timestampLabel);


        frame.setLayout(null);
        frame.setVisible(true);
    }

    private Image getScreenCapture(int rescaleWidth, int rescaleHeight) {

            Image fullImage = getScreenCapture();
            return fullImage.getScaledInstance(rescaleWidth, rescaleHeight, Image.SCALE_FAST);
    }

    public Image getPreciseScreenshot() {

        if(SystemUtil.getHostSystem().equals(SystemUtil.OperatingSystem.MAC_OS)) {
            BufferedImage screenshot = getScreenCapture();
            return screenshot.getSubimage(RunnerUtil.SCREEN_WIDTH - 100,0, 100, 30);
        }

        return null;
    }

    public BufferedImage getScreenCapture() {
        try {
            Robot robot = new Robot();
            return robot.createScreenCapture(new Rectangle(RunnerUtil.SCREEN_WIDTH, RunnerUtil.SCREEN_HEIGHT));
        } catch (AWTException e) {
            LOGGER.error("Robot initialization error", e);
        }

        return null;
    }

    public BufferedImage getBufferedImage(Image img) {
        if (img instanceof BufferedImage image)
        {
            return image;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    public void toggleRunning() {

        String label;

        if (cursorMover == null) {
            timerRunner = new Timer();
            CursorMoveListener cursorMoveListener = new CursorMoveListener();
            timerRunner.schedule(cursorMoveListener, 0L, 1000L);

            cursorMover = new CursorMover();
            timerRunner.schedule(cursorMover, 1000L, RunnerUtil.SECONDS_BETWEEN_MOVES * 1000L);

            label = "Stop [" +
                    NativeKeyEvent.getKeyText(NativeKeyEvent.VC_CONTROL) +
                    "][" +
                    NativeKeyEvent.getKeyText(NativeKeyEvent.VC_ALT) +
                    "]";
        }
        else {
            timerRunner.cancel();

            cursorMover = null;

            label = "Start [" +
                    NativeKeyEvent.getKeyText(NativeKeyEvent.VC_CONTROL) +
                    "][" +
                    NativeKeyEvent.getKeyText(NativeKeyEvent.VC_ALT) +
                    "]";
        }

        toggleButton.setLabel(label);

    }

}
