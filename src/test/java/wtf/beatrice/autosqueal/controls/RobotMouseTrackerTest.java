package wtf.beatrice.autosqueal.controls;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotMouseTrackerTest
{
    @Test
    void startsIdle() {
        RobotMouseTracker tracker = new RobotMouseTracker();

        assertFalse(tracker.isMoving());
        assertFalse(tracker.isLastPosition(10, 20));
    }

    @Test
    void tracksOngoingMovement() {
        RobotMouseTracker tracker = new RobotMouseTracker();

        tracker.moveStarted();
        assertTrue(tracker.isMoving());

        tracker.moveEnded(15, 25);
        assertFalse(tracker.isMoving());
        assertTrue(tracker.isLastPosition(15, 25));
        assertFalse(tracker.isLastPosition(15, 26));
        assertFalse(tracker.isLastPosition(25, 15));
    }
}