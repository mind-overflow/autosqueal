package wtf.beatrice.autosqueal.controls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.config.AutoSquealConfig;

import java.awt.*;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Periodically starts a new movement of the mouse cursor to a random position,
 * or to the top-right corner of the main screen for a double click.
 *
 * The movements only happen when the user is away: while the user is actively
 * using the machine, they are skipped, so the app never fights for the mouse.
 * The same goes for a movement already in progress: the task checks whether
 * the user is still away at every step, and gives the mouse back when they
 * are not.
 *
 * Everything that varies — how often to move, how fast, whether and how
 * often to click, which screens to travel — comes from the settings, which
 * are read live at every movement. The movement itself is executed by a
 * {@link SingleStepMovementTask}, which runs on its own virtual thread
 * until it reaches its destination.
 */
public class CursorMover implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(CursorMover.class);

    /**
     * Notified of the automation's own timing, for the live status view.
     * Called on the scheduler thread: implementations marshal to their
     * own thread.
     */
    public interface Listener {

        /** Called at every scheduled tick of the movement cadence. */
        default void onTick() { }

        /** Called when a movement was actually queued for execution. */
        default void onMovementQueued(int destX, int destY, boolean click) { }
    }

    /** Distance from the screen edge, so the corner click lands inside the screen. */
    private static final int CORNER_OFFSET_PIXELS = 5;

    private final Random random;
    private final ExecutorService movementExecutor;
    private final RobotMouseTracker robotTracker;
    private final BooleanSupplier isUserAway;
    private final AutoSquealConfig config;
    private final Supplier<Rectangle> movementBounds;
    private final Supplier<Rectangle> primaryScreenBounds;
    private final Supplier<Point> currentPosition;
    private volatile Listener listener;

    /** Movements started since the last corner click. */
    private int iteration = 0;

    /** Sets the listener notified of ticks and queued movements. */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public CursorMover(ExecutorService movementExecutor, RobotMouseTracker robotTracker,
                       BooleanSupplier isUserAway, AutoSquealConfig config,
                       Supplier<Rectangle> movementBounds, Supplier<Rectangle> primaryScreenBounds) {
        this(movementExecutor, robotTracker, isUserAway, config, movementBounds, primaryScreenBounds,
                new SecureRandom(), () -> MouseInfo.getPointerInfo().getLocation());
    }

    CursorMover(ExecutorService movementExecutor, RobotMouseTracker robotTracker,
                BooleanSupplier isUserAway, AutoSquealConfig config,
                Supplier<Rectangle> movementBounds, Supplier<Rectangle> primaryScreenBounds,
                Random random, Supplier<Point> currentPosition) {
        this.movementExecutor = movementExecutor;
        this.robotTracker = robotTracker;
        this.isUserAway = isUserAway;
        this.config = config;
        this.movementBounds = movementBounds;
        this.primaryScreenBounds = primaryScreenBounds;
        this.random = random;
        this.currentPosition = currentPosition;
    }

    @Override
    public void run() {

        if (listener != null) {
            listener.onTick();
        }

        if (!isUserAway.getAsBoolean()) {
            LOGGER.debug("User is present, skipping movement");
            return;
        }

        Point location = currentPosition.get();
        LOGGER.info("Starting coordinates: {}, {}", location.x, location.y);

        int destX;
        int destY;
        boolean click;

        // one movement every N is a corner double click, so that the
        // notification area gets exercised too. the corner is always the
        // main screen's top-right one, whatever screens the cursor may
        // otherwise travel to.
        if (config.isClickEnabled() && iteration == config.getClickEveryNMoves() - 1) {
            Rectangle primary = primaryScreenBounds.get();
            destX = primary.x + primary.width - CORNER_OFFSET_PIXELS;
            destY = primary.y + CORNER_OFFSET_PIXELS;
            click = true;
            iteration = 0;
        } else {
            Rectangle bounds = movementBounds.get();
            destX = bounds.x + random.nextInt(Math.max(1, bounds.width));
            destY = bounds.y + random.nextInt(Math.max(1, bounds.height));
            click = false;
            iteration++;
        }

        LOGGER.info("Destination coordinates: {}, {}", destX, destY);

        try {
            SingleStepMovementTask movement = new SingleStepMovementTask(
                    robotTracker, destX, destY, click, isUserAway,
                    config.getStepDelayMilliseconds(), location);
            movementExecutor.execute(movement);
            if (listener != null) {
                listener.onMovementQueued(destX, destY, click);
            }
        } catch (RejectedExecutionException ex) {
            LOGGER.debug("Movement not started: automation is shutting down");
        }
    }
}