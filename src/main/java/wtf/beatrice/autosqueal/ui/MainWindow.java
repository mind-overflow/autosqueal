package wtf.beatrice.autosqueal.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.controls.CursorMover;
import wtf.beatrice.autosqueal.listener.CursorMoveListener;
import wtf.beatrice.autosqueal.util.RunnerUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Timer;

public class MainWindow
{
    private static final Logger LOGGER = LogManager.getLogger(MainWindow.class);

    private static final int WINDOW_HEIGHT = 600;
    private static final int WINDOW_WIDTH = 800;

    private final Timer TIMER_RUNNER = new Timer();

    private CursorMover cursorMover = null;


    public MainWindow() {
    }

    public void init() {
        JFrame frame = new JFrame();
        frame.setSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));

        Button toggleButton = new Button("Start");
        toggleButton.setBounds(new Rectangle((WINDOW_WIDTH / 2) - 40, WINDOW_HEIGHT - 60, 80, 30));
        toggleButton.addActionListener(e -> toggleRunning(toggleButton));
        frame.add(toggleButton);

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

    private void toggleRunning(Button button) {

        if (cursorMover == null) {
            CursorMoveListener cursorMoveListener = new CursorMoveListener();
            TIMER_RUNNER.schedule(cursorMoveListener, 0L, 1000L);

            cursorMover = new CursorMover(RunnerUtil.SCREEN_WIDTH, RunnerUtil.SCREEN_HEIGHT);
            TIMER_RUNNER.schedule(cursorMover, 1000L, RunnerUtil.SECONDS_BETWEEN_MOVES * 1000L);

            button.setLabel("Stop");
        }
        else {
            TIMER_RUNNER.cancel();

            cursorMover = null;

            button.setLabel("Start");
        }

    }
}
