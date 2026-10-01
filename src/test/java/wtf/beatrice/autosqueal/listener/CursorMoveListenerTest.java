package wtf.beatrice.autosqueal.listener;

import org.junit.jupiter.api.Test;
import wtf.beatrice.autosqueal.controls.RobotMouseTracker;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorMoveListenerTest
{
    private static final int LOOPS_BEFORE_AWAY = 30;

    private final RobotMouseTracker robotTracker = new RobotMouseTracker();

    /** Builds a listener that reads scripted cursor positions, one per poll. */
    private CursorMoveListener scriptedListener(Point... positions) {
        Queue<Point> positionQueue = new ArrayDeque<>(java.util.List.of(positions));
        return new CursorMoveListener(robotTracker, positionQueue::remove);
    }

    private static Point at(int x, int y) {
        return new Point(x, y);
    }

    @Test
    void userIsAwayAfterThirtyStillSeconds() {
        Point[] still = new Point[LOOPS_BEFORE_AWAY + 2];
        for (int i = 0; i < still.length; i++) {
            still[i] = at(100, 100);
        }
        CursorMoveListener listener = scriptedListener(still);

        // the first poll seeds the baseline and does not count
        for (int i = 0; i < LOOPS_BEFORE_AWAY; i++) {
            listener.run();
            assertFalse(listener.isUserAway());
        }

        listener.run();
        assertTrue(listener.isUserAway());
    }

    @Test
    void firstPollDoesNotCountAsMovement() {
        // the very first position read must seed the baseline: starting anywhere
        // else would have counted as a user movement and reset the away timer
        CursorMoveListener listener = scriptedListener(at(500, 300), at(500, 300));

        listener.run();
        listener.run();

        assertFalse(listener.isUserAway());
        // still at (500, 300), so the baseline was seeded correctly
    }

    @Test
    void userMovementResetsTheAwayTimer() {
        List<Point> positions = new ArrayList<>();
        positions.add(at(100, 100));                              // baseline
        for (int i = 0; i < 5; i++) positions.add(at(100, 100)); // 5 still polls
        positions.add(at(140, 100));                              // the user moves the mouse
        for (int i = 0; i < LOOPS_BEFORE_AWAY; i++) positions.add(at(140, 100)); // then it stays put

        CursorMoveListener listener = scriptedListener(positions.toArray(Point[]::new));

        listener.run(); // seed the baseline
        for (int i = 0; i < 5; i++) listener.run(); // loops = 5
        assertFalse(listener.isUserAway());

        listener.run(); // the user moved: the away timer resets
        assertFalse(listener.isUserAway());

        // ...and it still takes 30 more still polls to be away again
        for (int i = 0; i < LOOPS_BEFORE_AWAY - 1; i++) listener.run(); // loops = 29
        assertFalse(listener.isUserAway());

        listener.run(); // loops = 30
        assertTrue(listener.isUserAway());
    }

    @Test
    void appMovementsDoNotKeepTheUserPresent() {
        // this is the regression the away detection was disconnected for: the
        // app moves the mouse every few seconds, and before the fix those
        // movements were indistinguishable from the user's, so the app would
        // always think the user was present

        // every poll finds the cursor where the app last left it
        List<Point> positions = new ArrayList<>();
        positions.add(at(100, 100)); // baseline
        for (int i = 1; i <= 40; i++) positions.add(at(100 + i, 100));

        CursorMoveListener listener = scriptedListener(positions.toArray(Point[]::new));

        listener.run(); // seed the baseline

        for (int i = 1; i <= 40; i++) {
            // the app just finished a movement, leaving the cursor exactly
            // where this poll will find it
            robotTracker.moveEnded(100 + i, 100);
            listener.run();
        }

        // every position change was the app's own doing: the user is away
        assertTrue(listener.isUserAway());
    }

    @Test
    void ongoingAppMovementsDoNotKeepTheUserPresent() {
        // same as above, but the polls happen while the app is mid-movement,
        // with the cursor transiently at intermediate positions
        Point[] positions = new Point[41];
        positions[0] = at(100, 100);
        for (int i = 1; i < positions.length; i++) {
            positions[i] = at(100 + (i / 2), 100);
        }
        CursorMoveListener listener = scriptedListener(positions);

        robotTracker.moveStarted();
        for (int i = 1; i < positions.length; i++) {
            listener.run();
        }

        assertTrue(listener.isUserAway());
    }

    @Test
    void keyboardActivityResetsTheAwayTimer() {
        List<Point> positions = new ArrayList<>();
        positions.add(at(100, 100));                              // baseline
        for (int i = 0; i < 29; i++) positions.add(at(100, 100)); // 29 still polls
        positions.add(at(100, 100));                              // the user presses a key
        for (int i = 0; i < LOOPS_BEFORE_AWAY; i++) positions.add(at(100, 100)); // then the mouse stays put

        CursorMoveListener listener = scriptedListener(positions.toArray(Point[]::new));

        listener.run(); // seed the baseline
        for (int i = 0; i < 29; i++) listener.run(); // loops = 29
        assertFalse(listener.isUserAway());

        // the user is typing without moving the mouse: the timer resets
        listener.reportKeyboardActivity();
        listener.run();
        assertFalse(listener.isUserAway());

        // ...and it still takes 30 still polls to be away again
        for (int i = 0; i < LOOPS_BEFORE_AWAY - 1; i++) listener.run(); // loops = 29
        assertFalse(listener.isUserAway());

        listener.run(); // loops = 30
        assertTrue(listener.isUserAway());
    }

    @Test
    void keyboardActivityBringsTheUserBackWhileAway() {
        Point[] positions = new Point[LOOPS_BEFORE_AWAY + 3];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = at(100, 100);
        }
        CursorMoveListener listener = scriptedListener(positions);

        for (int i = 0; i <= LOOPS_BEFORE_AWAY; i++) { // baseline + 30 still polls
            listener.run();
        }
        assertTrue(listener.isUserAway());

        // the user comes back at the keyboard: the away state must end,
        // even though the mouse never moved
        listener.reportKeyboardActivity();
        listener.run();
        assertFalse(listener.isUserAway());
    }

    @Test
    void userComingBackIsDetectedWhileTheAppIsWiggling() {
        // the app keeps wiggling while the user is away; when the user comes
        // back and moves the mouse, the away state must end

        // polls: baseline, 30 still polls, two app wiggles, then the user's move
        Point[] positions = new Point[LOOPS_BEFORE_AWAY + 4];
        for (int i = 0; i <= LOOPS_BEFORE_AWAY; i++) {
            positions[i] = at(100, 100);
        }
        positions[LOOPS_BEFORE_AWAY + 1] = at(150, 100);
        positions[LOOPS_BEFORE_AWAY + 2] = at(200, 100);
        positions[LOOPS_BEFORE_AWAY + 3] = at(333, 100);
        CursorMoveListener listener = scriptedListener(positions);

        // baseline + 30 still polls: the user goes away
        for (int i = 0; i <= LOOPS_BEFORE_AWAY; i++) {
            listener.run();
        }
        assertTrue(listener.isUserAway());

        // the app wiggles: each movement is discounted, the user stays "away"
        robotTracker.moveEnded(150, 100);
        listener.run();
        assertTrue(listener.isUserAway());

        robotTracker.moveEnded(200, 100);
        listener.run();
        assertTrue(listener.isUserAway());

        // the user comes back and grabs the mouse
        listener.run();
        assertFalse(listener.isUserAway());
    }
}