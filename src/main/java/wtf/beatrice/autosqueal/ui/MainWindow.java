package wtf.beatrice.autosqueal.ui;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.Main;
import wtf.beatrice.autosqueal.controls.CursorMover;
import wtf.beatrice.autosqueal.controls.RobotMouseTracker;
import wtf.beatrice.autosqueal.listener.CursorMoveListener;
import wtf.beatrice.autosqueal.util.RunnerUtil;
import wtf.beatrice.autosqueal.util.SystemUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainWindow
{
    private static final Logger LOGGER = LogManager.getLogger(MainWindow.class);

    private static final int WINDOW_HEIGHT = 700;
    private static final int WINDOW_WIDTH = 800;

    private static final long AUTOMATION_START_DELAY_SECONDS = 1L;
    private static final long AWAY_POLL_INTERVAL_SECONDS = 1L;

    private final JFrame frame = new JFrame();
    private JButton toggleButton;

    private ScheduledExecutorService periodicScheduler;
    private ExecutorService movementExecutor;
    private CursorMover cursorMover;

    /**
     * Builds and shows the main window, and starts the automation.
     * Must be called on the EDT.
     */
    public void init() {

        frame.setSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        frame.setTitle("autosqueal");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                LOGGER.info("Shutting down...");
                stopAutomation();
                Main.unregisterJNativeHook();
            }
        });

        toggleButton = new JButton();
        toggleButton.setBounds(new Rectangle((WINDOW_WIDTH / 2) - 60, WINDOW_HEIGHT - 60, 120, 30));
        toggleButton.addActionListener(e -> toggleRunning());
        frame.add(toggleButton);

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

        startAutomation();
        updateToggleLabel();
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

    /**
     * Toggles the automation on or off. Safe to call from any thread: the actual
     * work is always marshalled to the EDT, since it touches Swing components.
     */
    public void toggleRunning() {
        SwingUtilities.invokeLater(() -> {
            if (cursorMover == null) {
                startAutomation();
            } else {
                stopAutomation();
            }
            updateToggleLabel();
        });
    }

    private void startAutomation() {
        LOGGER.info("Starting automation");

        // periodic work (polling and movement cadence) on a classic scheduler,
        // so that it can be scheduled with fixed delays
        periodicScheduler = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "autosqueal-scheduler");
            thread.setDaemon(true);
            return thread;
        });

        // movements and clicks run on virtual threads: they mostly sleep,
        // and blocking them costs nothing
        movementExecutor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("autosqueal-movement-").factory());

        // away-detection: polls the cursor position and ignores the movements
        // the app performs itself, as reported by the robot mouse tracker.
        // the automation only moves the mouse when the user is away.
        RobotMouseTracker robotTracker = new RobotMouseTracker();
        CursorMoveListener cursorMoveListener = new CursorMoveListener(robotTracker);
        periodicScheduler.scheduleWithFixedDelay(cursorMoveListener, 0L, AWAY_POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);

        cursorMover = new CursorMover(movementExecutor, robotTracker, cursorMoveListener::isUserAway);
        periodicScheduler.scheduleWithFixedDelay(cursorMover,
                AUTOMATION_START_DELAY_SECONDS,
                RunnerUtil.SECONDS_BETWEEN_MOVES,
                TimeUnit.SECONDS);
    }

    private void stopAutomation() {
        LOGGER.info("Stopping automation");

        if (periodicScheduler != null) {
            periodicScheduler.shutdownNow();
            periodicScheduler = null;
        }

        if (movementExecutor != null) {
            // interrupts the in-flight movement threads, too
            movementExecutor.shutdownNow();
            movementExecutor = null;
        }

        cursorMover = null;
    }

    private void updateToggleLabel() {
        if (toggleButton == null) {
            // the window is not initialized yet
            return;
        }

        String hotkey = "[" + NativeKeyEvent.getKeyText(NativeKeyEvent.VC_CONTROL) + "]"
                + "[" + NativeKeyEvent.getKeyText(NativeKeyEvent.VC_ALT) + "]";

        toggleButton.setText((cursorMover == null ? "Start " : "Stop ") + hotkey);
    }

}