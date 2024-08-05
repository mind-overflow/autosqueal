package wtf.beatrice.autosqueal.ui;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.controls.CursorMover;
import wtf.beatrice.autosqueal.listener.CursorMoveListener;
import wtf.beatrice.autosqueal.util.RunnerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Timer;

public class MainWindow
{
    private static final Logger LOGGER = LogManager.getLogger(MainWindow.class);

    private static final int WINDOW_HEIGHT = 600;
    private static final int WINDOW_WIDTH = 800;

    private Timer timerRunner = new Timer();
    private CursorMover cursorMover = new CursorMover();
    private Button toggleButton;


    public MainWindow() {

    }

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
        JLabel imageLabel = new JLabel(getScreenCapture(rescaleWidth, rescaleHeight));
        imageLabel.setBounds(new Rectangle(bordersPx, bordersPx, rescaleWidth, rescaleHeight));
        frame.add(imageLabel);

        frame.setLayout(null);
        frame.setVisible(true);
    }

    private ImageIcon getScreenCapture(int rescaleWidth, int rescaleHeight) {

        try {
            Robot robot = new Robot();
            BufferedImage fullImage = robot.createScreenCapture(new Rectangle(RunnerUtil.SCREEN_WIDTH, RunnerUtil.SCREEN_HEIGHT));
            Image image = fullImage.getScaledInstance(rescaleWidth, rescaleHeight, Image.SCALE_FAST);
            return new ImageIcon(image);
        } catch (AWTException e) {
            LOGGER.error("Robot initialization error", e);
            return null;
        }
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
