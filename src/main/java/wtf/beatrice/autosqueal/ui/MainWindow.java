package wtf.beatrice.autosqueal.ui;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.Main;
import wtf.beatrice.autosqueal.config.AutoSquealConfig;
import wtf.beatrice.autosqueal.config.ConfigStore;
import wtf.beatrice.autosqueal.config.ScreenArea;
import wtf.beatrice.autosqueal.controls.CursorMover;
import wtf.beatrice.autosqueal.controls.RobotMouseTracker;
import wtf.beatrice.autosqueal.controls.SingleStepMovementTask;
import wtf.beatrice.autosqueal.listener.CursorMoveListener;
import wtf.beatrice.autosqueal.util.RunnerUtil;
import wtf.beatrice.autosqueal.util.SystemUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.desktop.QuitStrategy;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class MainWindow {

    private static final Logger LOGGER = LogManager.getLogger(MainWindow.class);

    private static final long AUTOMATION_START_DELAY_SECONDS = 1L;
    private static final long AWAY_POLL_INTERVAL_SECONDS = 1L;

    private static final Color RUNNING_COLOR = new Color(0x3F, 0xB9, 0x50);

    private final AutoSquealConfig config;
    private final ConfigStore configStore;

    // the away detection tracks the user, not the automation: it lives from
    // the window's creation and is polled only while the automation runs
    private final RobotMouseTracker robotTracker;
    private final CursorMoveListener awayDetector;

    private final JFrame frame = new JFrame();

    // status
    private JLabel statusPill;
    private JLabel userStateLabel;
    private JLabel nextMoveLabel;
    private JLabel movesLabel;
    private JButton toggleButton;
    private JButton moveNowButton;

    // settings
    private JSpinner awaySpinner;
    private JSpinner intervalSpinner;
    private JSlider stepPauseSlider;
    private JCheckBox clickCheckBox;
    private JSpinner clickEverySpinner;
    private JComboBox<ScreenArea> screenCombo;
    private JCheckBox startAutoCheckBox;
    private JCheckBox hotkeyCheckBox;

    private TrayIcon trayIcon;
    private MenuItem trayToggleItem;

    private ScheduledExecutorService periodicScheduler;
    private ExecutorService movementExecutor;
    private CursorMover cursorMover;
    private ScheduledFuture<?> moverFuture;

    /** When the movement cadence last ticked; drives the countdown. */
    private volatile long lastMovementTickAt = 0L;

    // status state: only touched on the EDT
    private long sessionMoves = 0;
    private String lastMoveAt = null;

    /** Updates the status section once per second. */
    private final javax.swing.Timer statusTimer = new javax.swing.Timer(1000, e -> refreshStatus());

    /** Counts the automation's own movements, wherever they come from. */
    private final CursorMover.Listener movementListener = new CursorMover.Listener() {
        @Override
        public void onTick() {
            lastMovementTickAt = System.currentTimeMillis();
        }

        @Override
        public void onMovementQueued(int destX, int destY, boolean click) {
            SwingUtilities.invokeLater(() -> {
                sessionMoves++;
                lastMoveAt = String.format("%tT", new Date());
            });
        }
    };

    /**
     * Creates the window around the given settings: the components read
     * them live, so a change applies without a restart. The store is used
     * to persist the settings as soon as the user changes them.
     */
    public MainWindow(AutoSquealConfig config, ConfigStore configStore) {
        this.config = config;
        this.configStore = configStore;
        this.robotTracker = new RobotMouseTracker();
        this.awayDetector = new CursorMoveListener(robotTracker, config);
    }

    /**
     * Builds and shows the main window, and starts the automation if the
     * settings say so. Must be called on the EDT.
     */
    public void init() {

        frame.setTitle("autosqueal");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                statusTimer.stop();
                cleanup();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                // catch a system appearance change while the app was inactive
                ThemeManager.reapplyIfSystemThemeChanged();
            }
        });

        // ⌘Q on macOS defaults to calling System.exit(0) directly, which
        // bypasses windowClosing and skips cleanup: route quits through
        // the window instead, so the hook is always released
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS);
            } catch (UnsupportedOperationException ex) {
                LOGGER.debug("Quit strategy not supported here", ex);
            }
        }

        buildContent();

        if (SystemTray.isSupported()) {
            addTrayIcon();
        }

        if (config.isStartAutomatically()) {
            startAutomation();
        }
        updateToggleLabel();
        refreshStatus();
        statusTimer.start();

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ── layout ──────────────────────────────────────────────────────────

    private void buildContent() {

        MigLayout layout = new MigLayout("wrap 1, insets 16, gap 6", "[grow]");
        JPanel root = new JPanel(layout);

        root.add(buildHeader(), "growx");
        root.add(buildStatusSection(), "growx");
        root.add(buildSettingsSection(), "growx");
        root.add(buildFooter(), "growx");

        frame.setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new MigLayout("insets 0, gap 0", "[][grow, right]"));

        JLabel titleLabel = new JLabel("autosqueal");
        titleLabel.setFont(titleLabel.getFont().deriveFont(20f));

        JLabel subtitleLabel = new JLabel("moves the mouse only when you're away");
        subtitleLabel.setForeground(UIManager.getColor("Label.disabledForeground"));

        statusPill = new JLabel("○ paused");
        statusPill.setForeground(UIManager.getColor("Label.disabledForeground"));

        header.add(titleLabel, "cell 0 0, aligny top");
        header.add(statusPill, "cell 1 0, aligny top");
        header.add(subtitleLabel, "cell 0 1");
        return header;
    }

    private JPanel buildStatusSection() {
        JPanel status = new JPanel(new MigLayout("wrap 1, insets 10 14 14 14, gap 4"));
        status.setBorder(BorderFactory.createTitledBorder("status"));

        userStateLabel = new JLabel("you are here");
        nextMoveLabel = new JLabel("—");
        movesLabel = new JLabel("no moves yet");

        toggleButton = new JButton("Start");
        toggleButton.addActionListener(e -> toggleRunning());

        moveNowButton = new JButton("Move now");
        moveNowButton.setToolTipText("queues a single small movement, ignoring the away detection: to test permissions");
        moveNowButton.addActionListener(e -> moveNow());

        JPanel buttons = new JPanel(new MigLayout("insets 0, gap 8"));
        buttons.add(toggleButton, "width 120:120:120, height 30:30:30");
        buttons.add(moveNowButton);

        status.add(userStateLabel);
        status.add(nextMoveLabel);
        status.add(movesLabel);
        status.add(buttons, "gaptop 8");
        return status;
    }

    private JPanel buildSettingsSection() {
        JPanel settings = new JPanel(new MigLayout("wrap 2, insets 10 14 14 14, gap 6 12", "[][right]", ""));
        settings.setBorder(BorderFactory.createTitledBorder("settings"));

        // away detection
        settings.add(new JLabel("Consider the user away after (seconds)"));
        awaySpinner = new JSpinner(new SpinnerNumberModel(config.getAwayThresholdSeconds(),
                AutoSquealConfig.AWAY_THRESHOLD_MIN_SECONDS, AutoSquealConfig.AWAY_THRESHOLD_MAX_SECONDS, 1));
        awaySpinner.addChangeListener(e -> applySetting(() ->
                config.setAwayThresholdSeconds(spinnerValue(awaySpinner))));
        settings.add(awaySpinner, "width 90:90:90, wrap");

        // movement cadence
        settings.add(new JLabel("Move every (seconds)"));
        intervalSpinner = new JSpinner(new SpinnerNumberModel(config.getMoveIntervalSeconds(),
                AutoSquealConfig.MOVE_INTERVAL_MIN_SECONDS, AutoSquealConfig.MOVE_INTERVAL_MAX_SECONDS, 1));
        intervalSpinner.addChangeListener(e -> {
            applySetting(() -> config.setMoveIntervalSeconds(spinnerValue(intervalSpinner)));
            rescheduleMoverInterval();
        });
        settings.add(intervalSpinner, "width 90:90:90, wrap");

        // movement speed: the slider shows the speed, from slow (left) to
        // fast (right) — internally it is the step pause, read upside down
        int fastest = AutoSquealConfig.STEP_DELAY_MIN_MILLISECONDS;
        int slowest = AutoSquealConfig.STEP_DELAY_MAX_MILLISECONDS;
        settings.add(new JLabel("Movement speed"));
        stepPauseSlider = new JSlider(fastest, slowest, slowest + fastest - config.getStepDelayMilliseconds());
        stepPauseSlider.addChangeListener(e -> applySetting(() ->
                config.setStepDelayMilliseconds(slowest + fastest - stepPauseSlider.getValue())));
        settings.add(stepPauseSlider, "width 140:160:180, wrap");

        // corner clicks
        settings.add(new JLabel("Click the notification corner"));
        clickCheckBox = new JCheckBox("", config.isClickEnabled());
        clickCheckBox.addChangeListener(e -> applySetting(() -> config.setClickEnabled(clickCheckBox.isSelected())));
        settings.add(clickCheckBox, "wrap");

        settings.add(new JLabel("One corner click every (moves)"));
        clickEverySpinner = new JSpinner(new SpinnerNumberModel(config.getClickEveryNMoves(),
                AutoSquealConfig.CLICK_EVERY_MIN_MOVES, AutoSquealConfig.CLICK_EVERY_MAX_MOVES, 1));
        clickEverySpinner.addChangeListener(e -> applySetting(() ->
                config.setClickEveryNMoves(spinnerValue(clickEverySpinner))));
        settings.add(clickEverySpinner, "width 90:90:90, wrap");
        clickCheckBox.addChangeListener(e -> clickEverySpinner.setEnabled(clickCheckBox.isSelected()));
        clickEverySpinner.setEnabled(clickCheckBox.isSelected());

        // screen
        settings.add(new JLabel("Travel to"));
        screenCombo = new JComboBox<>(ScreenArea.values());
        screenCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                         boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, screenName((ScreenArea) value),
                        index, isSelected, cellHasFocus);
            }
        });
        screenCombo.setSelectedItem(config.getMovementScreen());
        screenCombo.addActionListener(e -> applySetting(() ->
                config.setMovementScreen((ScreenArea) screenCombo.getSelectedItem())));
        settings.add(screenCombo, "width 140:140:140, wrap");

        // startup
        settings.add(new JLabel("Start the automation at launch"));
        startAutoCheckBox = new JCheckBox("", config.isStartAutomatically());
        startAutoCheckBox.addChangeListener(e -> applySetting(() ->
                config.setStartAutomatically(startAutoCheckBox.isSelected())));
        settings.add(startAutoCheckBox, "wrap");

        // hotkey
        settings.add(new JLabel("Global toggle hotkey"));
        JPanel hotkeyPanel = new JPanel(new MigLayout("insets 0, gap 8", "[]push[]", ""));
        hotkeyCheckBox = new JCheckBox("enabled", config.isHotkeyEnabled());
        hotkeyCheckBox.addChangeListener(e -> applySetting(() ->
                config.setHotkeyEnabled(hotkeyCheckBox.isSelected())));
        String hotkey = "[" + NativeKeyEvent.getKeyText(NativeKeyEvent.VC_CONTROL) + "]"
                + "[" + NativeKeyEvent.getKeyText(NativeKeyEvent.VC_ALT) + "]";
        hotkeyPanel.add(hotkeyCheckBox);
        hotkeyPanel.add(new JLabel(hotkey));
        settings.add(hotkeyPanel, "wrap");

        return settings;
    }

    private String screenName(ScreenArea area) {
        return area == ScreenArea.ALL ? "all screens" : "the main screen";
    }

    private JComponent buildFooter() {
        JLabel footer = new JLabel("settings are saved as they change · ⌘Q or the tray icon quits");
        footer.setFont(footer.getFont().deriveFont(11f));
        footer.setForeground(UIManager.getColor("Label.disabledForeground"));
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        return footer;
    }

    // ── status ──────────────────────────────────────────────────────────

    private void refreshStatus() {
        boolean away = awayDetector.isUserAway();

        userStateLabel.setText(away ? "● you are away" : "● you are here");
        userStateLabel.setForeground(away ? RUNNING_COLOR : UIManager.getColor("Label.foreground"));

        boolean running = cursorMover != null;
        if (!running) {
            nextMoveLabel.setText("automation is paused");
        } else if (robotTracker.isMoving()) {
            nextMoveLabel.setText("moving…");
        } else {
            long nextAt = lastMovementTickAt + config.getMoveIntervalSeconds() * 1000L;
            long seconds = Math.max(0L, (nextAt - System.currentTimeMillis()) / 1000L);
            nextMoveLabel.setText((away ? "next move in " : "next attempt in ") + seconds + "s");
        }

        movesLabel.setText(sessionMoves + " moves this session"
                + (lastMoveAt == null ? "" : " · last at " + lastMoveAt));

        updateToggleLabel();
    }

    private void updateToggleLabel() {
        if (toggleButton == null) {
            // the window is not initialized yet
            return;
        }

        boolean running = cursorMover != null;
        toggleButton.setText(running ? "Pause" : "Start");
        moveNowButton.setEnabled(running);
        statusPill.setText(running ? "● running" : "○ paused");
        statusPill.setForeground(running ? RUNNING_COLOR : UIManager.getColor("Label.disabledForeground"));

        if (trayToggleItem != null) {
            trayToggleItem.setLabel(running ? "Pause" : "Start");
        }
    }

    // ── settings plumbing ───────────────────────────────────────────────

    /** Applies a setting change and persists it right away. */
    private void applySetting(Runnable setter) {
        setter.run();
        configStore.save(config);
    }

    private static int spinnerValue(JSpinner spinner) {
        return (Integer) spinner.getValue();
    }

    /**
     * Re-schedules the movement cadence with the current interval, without
     * stopping the automation: the away detection keeps running untouched.
     */
    private void rescheduleMoverInterval() {
        if (cursorMover == null || periodicScheduler == null) {
            return;
        }

        if (moverFuture != null) {
            moverFuture.cancel(false);
        }
        long interval = config.getMoveIntervalSeconds();
        moverFuture = periodicScheduler.scheduleWithFixedDelay(cursorMover, interval, interval, TimeUnit.SECONDS);
    }

    // ── automation ──────────────────────────────────────────────────────

    /** Queues a single small movement, ignoring the away detection: a test. */
    private void moveNow() {
        if (movementExecutor == null) {
            return;
        }

        Rectangle bounds = RunnerUtil.screenBounds(ScreenArea.PRIMARY);
        Point start = MouseInfo.getPointerInfo().getLocation();

        int destX = Math.min(bounds.x + bounds.width - 20, start.x + 80);
        int destY = Math.max(bounds.y + 20, start.y - 60);
        if (destX == start.x && destY == start.y) {
            // no visible movement otherwise
            destX += 40;
        }

        // the test move ignores the away detection, or it would be
        // abandoned at the first poll with the user present
        SingleStepMovementTask movement = new SingleStepMovementTask(
                robotTracker, destX, destY, false, () -> true,
                config.getStepDelayMilliseconds(), start);
        movementExecutor.execute(movement);

        sessionMoves++;
        lastMoveAt = String.format("%tT", new Date());
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

        // the movement bounds are read again at every movement, so a change
        // of setting applies from the next movement without a restart. the
        // corner click always aims at the main screen, whatever screens the
        // cursor may otherwise travel to.
        Supplier<Rectangle> movementBounds = () -> RunnerUtil.screenBounds(config.getMovementScreen());
        Supplier<Rectangle> primaryScreenBounds = () -> RunnerUtil.screenBounds(ScreenArea.PRIMARY);

        cursorMover = new CursorMover(movementExecutor, robotTracker, awayDetector::isUserAway,
                config, movementBounds, primaryScreenBounds);
        cursorMover.setListener(movementListener);
        lastMovementTickAt = System.currentTimeMillis();

        moverFuture = periodicScheduler.scheduleWithFixedDelay(cursorMover,
                AUTOMATION_START_DELAY_SECONDS,
                config.getMoveIntervalSeconds(),
                TimeUnit.SECONDS);
    }

    private void stopAutomation() {
        LOGGER.info("Stopping automation");

        if (moverFuture != null) {
            moverFuture.cancel(false);
            moverFuture = null;
        }

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
            refreshStatus();
        });
    }

    // ── tray ─────────────────────────────────────────────────────────────

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
            statusTimer.stop();
            cleanup();
            System.exit(0);
        });
        menu.add(quitItem);

        // multi-resolution, so the icon stays crisp on HiDPI screens
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

    /** Draws the mouse pointer used as tray icon, at the given pixel size. */
    private Image createTrayIconImage() {
        return new BaseMultiResolutionImage(drawPointer(16), drawPointer(32), drawPointer(64));
    }

    private BufferedImage drawPointer(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        double scale = size / 16.0;

        int[] xPoints = {3, 3, 6, 8, 10, 8, 12};
        int[] yPoints = {1, 12, 8, 12, 11, 7, 7};
        int[] scaledX = scale(xPoints, scale);
        int[] scaledY = scale(yPoints, scale);

        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.BLACK);
            graphics.fillPolygon(scaledX, scaledY, scaledX.length);
            graphics.setColor(Color.WHITE);
            graphics.setStroke(new BasicStroke((float) Math.max(1.0, scale)));
            graphics.drawPolygon(scaledX, scaledY, scaledX.length);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static int[] scale(int[] points, double factor) {
        int[] scaled = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            scaled[i] = (int) Math.round(points[i] * factor);
        }
        return scaled;
    }

    // ── shutdown ─────────────────────────────────────────────────────────

    /** Whether cleanup already ran, so it can safely be called twice. */
    private boolean cleanedUp = false;

    /**
     * Stops the automation and releases everything the app is holding.
     * Safe to call more than once, from any thread: window closing and
     * JVM shutdown hooks can race each other on the way out.
     */
    public synchronized void cleanup() {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;

        LOGGER.info("Shutting down...");
        statusTimer.stop();
        stopAutomation();

        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
            trayIcon = null;
        }

        Main.unregisterJNativeHook();
    }

    // ── permissions help ──────────────────────────────────────────────────

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
                            + "2. screen recording — lets the app read the cursor position.\n\n"
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
}