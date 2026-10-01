package wtf.beatrice.autosqueal.listener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.config.AutoSquealConfig;
import wtf.beatrice.autosqueal.controls.RobotMouseTracker;

import java.awt.*;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Watches the cursor position and tracks whether the user is away: if the
 * user has not touched mouse or keyboard for long enough — ignoring the
 * movements the app performs on its own — the user is considered away.
 * Keyboard activity counts as presence, too: a user who is typing is using
 * the machine, even if the mouse never moves.
 *
 * The away state is time-based: how long the user has been idle is compared
 * against the threshold at every poll, so a threshold changed on the fly
 * applies from the very next poll.
 */
public class CursorMoveListener implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(CursorMoveListener.class);

    private final RobotMouseTracker robotTracker;
    private final Supplier<Point> currentPosition;
    private final AutoSquealConfig config;
    private final LongSupplier clock;

    private int lastSeenX;
    private int lastSeenY;
    private boolean firstPoll = true;
    private long lastUserActivityTime;
    private volatile boolean userAway = false;
    private volatile boolean keyboardActivity = false;

    public CursorMoveListener(RobotMouseTracker robotTracker, AutoSquealConfig config) {
        this(robotTracker, config, () -> MouseInfo.getPointerInfo().getLocation(), System::currentTimeMillis);
    }

    CursorMoveListener(RobotMouseTracker robotTracker, AutoSquealConfig config,
                       Supplier<Point> currentPosition, LongSupplier clock) {
        this.robotTracker = robotTracker;
        this.config = config;
        this.currentPosition = currentPosition;
        this.clock = clock;
    }

    /**
     * Reports that the user pressed a key: keyboard activity counts as
     * presence, so it resets the away timer at the next poll. Safe to call
     * from any thread.
     */
    public void reportKeyboardActivity() {
        keyboardActivity = true;
    }

    @Override
    public void run() {

        // consume the keyboard activity reported since the previous poll
        boolean keyboardActivity = this.keyboardActivity;
        this.keyboardActivity = false;

        Point location = currentPosition.get();
        long now = clock.getAsLong();

        if (firstPoll) {
            // seed the comparison baseline and the activity clock, so that
            // the first poll is neither a movement nor a full idle period
            firstPoll = false;
            lastSeenX = location.x;
            lastSeenY = location.y;
            lastUserActivityTime = now;
            return;
        }

        boolean positionChanged = location.x != lastSeenX || location.y != lastSeenY;
        boolean userMoved = keyboardActivity || (positionChanged && !isAppMovement(location));

        if (userMoved) {
            lastUserActivityTime = now;
        }

        boolean away = now - lastUserActivityTime >= config.getAwayThresholdSeconds() * 1000L;
        if (away && !userAway) {
            LOGGER.info("User is away!");
        } else if (!away && userAway) {
            LOGGER.info("User is no longer away!");
        }
        userAway = away;

        lastSeenX = location.x;
        lastSeenY = location.y;
    }

    /**
     * Whether a position change was (most likely) caused by the app itself:
     * either a movement is currently in progress, or the cursor is exactly
     * where the app last left it.
     */
    private boolean isAppMovement(Point location) {
        return robotTracker.isMoving() || robotTracker.isLastPosition(location.x, location.y);
    }

    public boolean isUserAway() {
        return userAway;
    }
}