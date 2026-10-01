package wtf.beatrice.autosqueal.listener;

import org.junit.jupiter.api.Test;
import wtf.beatrice.autosqueal.config.AutoSquealConfig;
import wtf.beatrice.autosqueal.controls.RobotMouseTracker;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorMoveListenerTest
{
    private static final int AWAY_THRESHOLD_SECONDS = 30;

    private final RobotMouseTracker robotTracker = new RobotMouseTracker();

    /** The fake clock the listener polls with, in milliseconds. */
    private long now = 1_000_000L;

    /** Builds a listener that reads scripted cursor positions, one per poll. */
    private CursorMoveListener scriptedListener(Point... positions) {
        Queue<Point> positionQueue = new ArrayDeque<>(java.util.List.of(positions));
        return new CursorMoveListener(robotTracker, new AutoSquealConfig(),
                positionQueue::remove, () -> now);
    }

    private static Point at(int x, int y) {
        return new Point(x, y);
    }

    /** Polls, pretending that the given seconds have passed since the previous poll. */
    private void poll(CursorMoveListener listener, int secondsSincePreviousPoll) {
        now += secondsSincePreviousPoll * 1000L;
        listener.run();
    }

    @Test
    void userIsAwayAfterThirtyStillSeconds() {
        Point[] still = new Point[AWAY_THRESHOLD_SECONDS + 3];
        for (int i = 0; i < still.length; i++) {
            still[i] = at(100, 100);
        }
        CursorMoveListener listener = scriptedListener(still);

        // the first poll seeds the baseline and does not count
        poll(listener, 1);

        // 29 still seconds: the user is still considered present
        for (int i = 0; i < AWAY_THRESHOLD_SECONDS - 1; i++) {
            poll(listener, 1);
            assertFalse(listener.isUserAway());
        }

        // the 30th still second: the user is away
        poll(listener, 1);
        assertTrue(listener.isUserAway());
    }

    @Test
    void firstPollDoesNotCountAsMovementOrIdleTime() {
        // the very first position read must seed the baseline and the
        // activity clock: starting anywhere else would have counted the
        // first poll as a movement, or as a full idle period
        CursorMoveListener listener = scriptedListener(at(500, 300), at(500, 300));

        poll(listener, 5);
        poll(listener, 10);

        assertFalse(listener.isUserAway());
        // still at (500, 300), and only 10 idle seconds: the seeding poll
        // consumed the first 5 seconds
    }

    @Test
    void userMovementResetsTheAwayTimer() {
        Queue<Point> positions = new ArrayDeque<>();
        positions.add(at(100, 100));                          // baseline
        for (int i = 0; i < 3; i++) positions.add(at(100, 100)); // 3 still seconds
        positions.add(at(140, 100));                          // the user moves the mouse
        for (int i = 0; i < AWAY_THRESHOLD_SECONDS; i++) positions.add(at(140, 100)); // then it stays put
        CursorMoveListener listener = new CursorMoveListener(robotTracker, new AutoSquealConfig(),
                positions::remove, () -> now);

        poll(listener, 1); // seed the baseline
        for (int i = 0; i < 3; i++) {
            poll(listener, 1);
        }
        assertFalse(listener.isUserAway());

        poll(listener, 1); // the user moved: the away timer resets
        assertFalse(listener.isUserAway());

        // ...and it still takes 30 more still seconds to be away again
        for (int i = 0; i < AWAY_THRESHOLD_SECONDS - 1; i++) {
            poll(listener, 1);
            assertFalse(listener.isUserAway());
        }

        poll(listener, 1);
        assertTrue(listener.isUserAway());
    }

    @Test
    void appMovementsDoNotKeepTheUserPresent() {
        // this is the regression the away detection was disconnected for: the
        // app moves the mouse every few seconds, and before the fix those
        // movements were indistinguishable from the user's, so the app would
        // always think the user was present

        // every poll finds the cursor where the app last left it
        Queue<Point> appPositions = new ArrayDeque<>();
        appPositions.add(at(100, 100));
        for (int i = 1; i <= 40; i++) {
            appPositions.add(at(100 + i, 100));
        }
        CursorMoveListener listener = new CursorMoveListener(robotTracker, new AutoSquealConfig(),
                appPositions::remove, () -> now);

        poll(listener, 1); // seed the baseline

        for (int i = 1; i <= 40; i++) {
            // the app just finished a movement, leaving the cursor exactly
            // where this poll will find it
            robotTracker.moveEnded(100 + i, 100);
            poll(listener, 1);
        }

        // every position change was the app's own doing: the user is away
        assertTrue(listener.isUserAway());
    }

    @Test
    void ongoingAppMovementsDoNotKeepTheUserPresent() {
        // same as above, but the polls happen while the app is mid-movement,
        // with the cursor transiently at intermediate positions
        Queue<Point> positions = new ArrayDeque<>();
        positions.add(at(100, 100));
        for (int i = 1; i <= 40; i++) {
            positions.add(at(100 + (i / 2), 100));
        }
        CursorMoveListener listener = new CursorMoveListener(robotTracker, new AutoSquealConfig(),
                positions::remove, () -> now);

        robotTracker.moveStarted();
        for (int i = 0; i <= 40; i++) {
            poll(listener, 1);
        }

        assertTrue(listener.isUserAway());
    }

    @Test
    void keyboardActivityResetsTheAwayTimer() {
        Queue<Point> positions = new ArrayDeque<>();
        positions.add(at(100, 100)); // baseline
        for (int i = 0; i < 60; i++) {
            positions.add(at(100, 100)); // the mouse never moves
        }
        CursorMoveListener listener = new CursorMoveListener(robotTracker, new AutoSquealConfig(),
                positions::remove, () -> now);

        poll(listener, 1); // seed the baseline
        for (int i = 0; i < AWAY_THRESHOLD_SECONDS - 1; i++) {
            poll(listener, 1);
        }
        assertFalse(listener.isUserAway());

        // the user is typing without moving the mouse: the timer resets
        listener.reportKeyboardActivity();
        poll(listener, 1);
        assertFalse(listener.isUserAway());

        // ...and it still takes 30 still seconds to be away again
        for (int i = 0; i < AWAY_THRESHOLD_SECONDS - 1; i++) {
            poll(listener, 1);
            assertFalse(listener.isUserAway());
        }

        poll(listener, 1);
        assertTrue(listener.isUserAway());
    }

    @Test
    void keyboardActivityBringsTheUserBackWhileAway() {
        Queue<Point> positions = new ArrayDeque<>();
        positions.add(at(100, 100));
        for (int i = 0; i < AWAY_THRESHOLD_SECONDS + 3; i++) {
            positions.add(at(100, 100));
        }
        CursorMoveListener listener = new CursorMoveListener(robotTracker, new AutoSquealConfig(),
                positions::remove, () -> now);

        poll(listener, 1); // seed the baseline
        for (int i = 0; i < AWAY_THRESHOLD_SECONDS; i++) {
            poll(listener, 1);
        }
        assertTrue(listener.isUserAway());

        // the user comes back at the keyboard: the away state must end,
        // even though the mouse never moved
        listener.reportKeyboardActivity();
        poll(listener, 1);
        assertFalse(listener.isUserAway());
    }

    @Test
    void userComingBackIsDetectedWhileTheAppIsWiggling() {
        // the app keeps wiggling while the user is away; when the user comes
        // back and moves the mouse, the away state must end
        Queue<Point> positions = new ArrayDeque<>();
        positions.add(at(100, 100));
        for (int i = 0; i < AWAY_THRESHOLD_SECONDS; i++) {
            positions.add(at(100, 100));
        }
        positions.add(at(150, 100)); // app wiggle
        positions.add(at(200, 100)); // app wiggle
        positions.add(at(333, 100)); // the user's move
        CursorMoveListener listener = new CursorMoveListener(robotTracker, new AutoSquealConfig(),
                positions::remove, () -> now);

        poll(listener, 1); // seed the baseline
        for (int i = 0; i < AWAY_THRESHOLD_SECONDS; i++) {
            poll(listener, 1);
        }
        assertTrue(listener.isUserAway());

        // the app wiggles: each movement is discounted, the user stays "away"
        robotTracker.moveEnded(150, 100);
        poll(listener, 1);
        assertTrue(listener.isUserAway());

        robotTracker.moveEnded(200, 100);
        poll(listener, 1);
        assertTrue(listener.isUserAway());

        // the user comes back and grabs the mouse
        poll(listener, 1);
        assertFalse(listener.isUserAway());
    }

    @Test
    void thresholdChangesApplyFromTheNextPoll() {
        // the threshold is read live: a change applies without a restart
        AutoSquealConfig config = new AutoSquealConfig();
        config.setAwayThresholdSeconds(10);
        Queue<Point> positions = new ArrayDeque<>();
        for (int i = 0; i < 20; i++) {
            positions.add(at(100, 100));
        }
        CursorMoveListener listener = new CursorMoveListener(robotTracker, config,
                positions::remove, () -> now);

        poll(listener, 1); // seed the baseline

        // 9 still seconds: not away with a 10 seconds threshold
        for (int i = 0; i < 9; i++) {
            poll(listener, 1);
            assertFalse(listener.isUserAway());
        }

        // 10th second: away
        poll(listener, 1);
        assertTrue(listener.isUserAway());

        // the user tightens the threshold to 5: the next poll re-evaluates
        // and the user is still away (already idle for more than 5 seconds)
        config.setAwayThresholdSeconds(5);
        poll(listener, 1);
        assertTrue(listener.isUserAway());

        // the user loosens the threshold to 600: the same idle time is no
        // longer enough, so the user is present again — and back to away
        // only after 600 seconds of stillness
        config.setAwayThresholdSeconds(600);
        poll(listener, 1);
        assertFalse(listener.isUserAway());
    }
}