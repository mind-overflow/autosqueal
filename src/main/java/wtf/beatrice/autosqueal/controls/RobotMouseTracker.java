package wtf.beatrice.autosqueal.controls;

/**
 * Keeps track of the mouse movements the app itself performs, so that other
 * components (like the away detection) can tell them apart from the user's.
 *
 * Written by the movement tasks, read by whoever needs to know.
 */
public class RobotMouseTracker {

    private volatile boolean moving = false;
    private volatile int lastX = Integer.MIN_VALUE;
    private volatile int lastY = Integer.MIN_VALUE;

    /** Marks the start of a movement performed by the app. */
    public void moveStarted() {
        moving = true;
    }

    /**
     * Marks the end of a movement performed by the app, which left the cursor
     * at the given coordinates.
     */
    public void moveEnded(int x, int y) {
        lastX = x;
        lastY = y;
        moving = false;
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean isLastPosition(int x, int y) {
        return x == lastX && y == lastY;
    }
}