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
import java.io.IOException;
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

    // the away detection tracks the user, not the automation: it lives from
    // the window's creation and is polled only while the automation runs
    private final RobotMouseTracker robotTracker = new RobotMouseTracker();
    private final CursorMoveListener awayDetector = new CursorMoveListener(robotTracker);

    private ScheduledExecutorService periodicScheduler;
    private ExecutorService movementExecutor;
    private CursorMover cursorMover;

    private TrayIcon trayIcon;
    private MenuItem trayToggleItem;

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
                cleanup();
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

        if (SystemTray.isSupported()) {
            addTrayIcon();
        }

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
     * Adds a menu bar icon with a toggle entry and a quit entry, so the
     * automation can be controlled even with the window closed.
     */
    private void addTrayIcon() {
        PopupMenu menu = new PopupMenu();

        trayToggleItem = new MenuItem();
        trayToggleItem.addActionListener(e -> toggleRunning());
        menu.add(trayToggleItem);

        MenuItem quitItem = new MenuItem("Quit");
        quitItem.addActionListener(e -> {
            cleanup();
            System.exit(0);
        });
        menu.add(quitItem);

        trayIcon = new TrayIcon(createTrayIconImage(), "autosqueal", menu);
        trayIcon.setImageAutoSize(true);

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException ex) {
            LOGGER.error("Could not add the tray icon", ex);
            trayIcon = null;
            trayToggleItem = null;
        }
    }

    /** Draws the little mouse pointer used as tray icon. */
    private Image createTrayIconImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        int[] xPoints = {3, 3, 6, 8, 10, 8, 12};
        int[] yPoints = {1, 12, 8, 12, 11, 7, 7};

        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(Color.BLACK);
        graphics.fillPolygon(xPoints, yPoints, xPoints.length);
        graphics.setColor(Color.WHITE);
        graphics.drawPolygon(xPoints, yPoints, xPoints.length);
        graphics.dispose();

        return image;
    }

    /** Stops the automation and releases everything the app is holding. */
    private void cleanup() {
        LOGGER.info("Shutting down...");
        stopAutomation();

        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }

        Main.unregisterJNativeHook();
    }

    /**
     * Explains which permissions the app needs and offers to open each pane.
     * the hook is retried on demand, so the app can start working without a
     * restart. must be called on the EDT, after the window is visible.
     */
    public void showPermissionsHelp() {
        Object[] options = {"Open Device Control Settings", "Open Screen Recording Settings", "Try Again", "Close"};

        while (true) {
            int choice = JOptionPane.showOptionDialog(frame,
                    "autosqueal needs three separate permissions to work:\n\n"
                            + "1. device control and data access — called \"accessibility\" on older macos\n"
                            + "   versions — lets the app listen for the ctrl+alt hotkey.\n"
                            + "   if the pane shows a separate \"events\" switch for autosqueal, turn\n"
                            + "   that on too: it is what lets the app move the mouse and click.\n\n"
                            + "2. screen recording — lets the app take the screenshot shown\n"
                            + "   in the main window.\n\n"
                            + "open each pane, unlock it, and make sure autosqueal is listed and switched on.\n"
                            + "if it is already listed, remove it with the minus button and re-add it:\n"
                            + "every rebuild looks like a different app to the system.\n"
                            + "when you are done, press \"try again\".",
                    "permissions needed",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == 0) {
                openPrivacyPane("Privacy_Accessibility");
            } else if (choice == 1) {
                openPrivacyPane("Privacy_ScreenCapture");
            } else if (choice == 2) {
                if (Main.registerJNativeHook()) {
                    LOGGER.info("The native hook was registered on a later attempt!");
                    return;
                }
            } else {
                // closed or gave up: leave the app running, the window and the
                // tray icon still work
                return;
            }
        }
    }

    /** Opens the privacy & security pane for the given anchor. */
    private void openPrivacyPane(String privacyAnchor) {
        if (SystemUtil.getHostSystem() != SystemUtil.OperatingSystem.MAC_OS) {
            return;
        }

        try {
            new ProcessBuilder("open",
                    "x-apple.systempreferences:com.apple.preference.security?" + privacyAnchor)
                    .start();
        } catch (IOException ex) {
            LOGGER.error("Could not open the system settings", ex);
        }
    }

    /**
     * Forwards keyboard activity to the away detection, so that typing
     * counts as presence even when the mouse never moves.
     */
    public void notifyKeyboardActivity() {
        awayDetector.reportKeyboardActivity();
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
        periodicScheduler.scheduleWithFixedDelay(awayDetector, 0L, AWAY_POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);

        cursorMover = new CursorMover(movementExecutor, robotTracker, awayDetector::isUserAway);
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

        String label = (cursorMover == null ? "Start " : "Stop ") + hotkey;

        toggleButton.setText(label);
        if (trayToggleItem != null) {
            trayToggleItem.setLabel(label);
        }
    }

}