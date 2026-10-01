package wtf.beatrice.autosqueal.listener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.controls.RobotMouseTracker;

import java.awt.*;
import java.util.function.Supplier;

/**
 * Watches the cursor position and tracks whether the user is away: if the cursor
 * stays still for long enough — ignoring the movements the app performs on its
 * own — the user is considered away. Keyboard activity counts as presence,
 * too: a user who is typing is using the machine, even if the mouse never moves.
 */
public class CursorMoveListener implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(CursorMoveListener.class);

    /** Number of consecutive still polls after which the user is considered away. */
    private static final int LOOPS_BEFORE_AWAY = 30;

    private final RobotMouseTracker robotTracker;
    private final Supplier<Point> currentPosition;

    private int lastSeenX;
    private int lastSeenY;
    private boolean firstPoll = true;
    private int loops = 0;
    private volatile boolean userAway = false;
    private volatile boolean keyboardActivity = false;

    public CursorMoveListener(RobotMouseTracker robotTracker) {
        this(robotTracker, () -> MouseInfo.getPointerInfo().getLocation());
    }

    CursorMoveListener(RobotMouseTracker robotTracker, Supplier<Point> currentPosition) {
        this.robotTracker = robotTracker;
        this.currentPosition = currentPosition;
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

        if (firstPoll) {
            // seed the comparison baseline, so that the first poll is not
            // mistaken for a movement
            firstPoll = false;
            lastSeenX = location.x;
            lastSeenY = location.y;
            return;
        }

        boolean positionChanged = location.x != lastSeenX || location.y != lastSeenY;
        boolean userMoved = keyboardActivity || (positionChanged && !isAppMovement(location));

        if (userMoved) {
            if (userAway) {
                LOGGER.info("User is no longer away!");
            }
            loops = 0;
        } else if (loops < LOOPS_BEFORE_AWAY) {
            loops++;
            if (loops == LOOPS_BEFORE_AWAY) {
                LOGGER.info("User is away!");
            }
        }

        userAway = loops >= LOOPS_BEFORE_AWAY;

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