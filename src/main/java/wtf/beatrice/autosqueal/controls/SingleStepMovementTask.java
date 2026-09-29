package wtf.beatrice.autosqueal.controls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.InputEvent;

/**
 * Moves the mouse cursor towards a destination one small step at a time.
 *
 * Runs on its own virtual thread: it mostly sleeps between steps, so blocking
 * it is free. When it gets to its destination, it can optionally perform a
 * double click, with the pacing the app has always used.
 *
 * All the movements it performs are reported to the {@link RobotMouseTracker},
 * so that the away detection can tell them apart from the user's.
 */
public class SingleStepMovementTask implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(SingleStepMovementTask.class);

    /** Delay between two consecutive cursor steps, in milliseconds. */
    private static final long STEP_DELAY_MILLISECONDS = 2L;

    private final RobotMouseTracker robotTracker;
    private final Robot robot;
    private final int destX;
    private final int destY;
    private final boolean click;

    private float currentX;
    private float currentY;
    private float stepX;
    private float stepY;

    public SingleStepMovementTask(RobotMouseTracker robotTracker, int destinationX, int destinationY, boolean click) throws AWTException {

        this.robotTracker = robotTracker;
        this.destX = destinationX;
        this.destY = destinationY;
        this.click = click;

        Point location = MouseInfo.getPointerInfo().getLocation();
        this.currentX = location.x;
        this.currentY = location.y;

        int lengthX = Math.round(Math.abs(currentX - destX));
        int lengthY = Math.round(Math.abs(currentY - destY));

        this.stepX = computeStepX(lengthX, lengthY);
        this.stepY = computeStepY(lengthX, lengthY);

        LOGGER.info("Dest: [{}, {}], Curr: [{}, {}]", destX, destY, currentX, currentY);
        LOGGER.info("Step: [{}, {}]", stepX, stepY);

        this.robot = new Robot();
    }

    @Override
    public void run() {

        // any position change from here on is the app's doing
        robotTracker.moveStarted();

        try {
            while (!hasReachedDestination()) {
                // when less than a full step is left, move exactly what is
                // left, so that both axes always land on their destination
                stepX = adjustedStep(currentX, destX, stepX);
                stepY = adjustedStep(currentY, destY, stepY);

                currentX = advance(currentX, destX, stepX);
                currentY = advance(currentY, destY, stepY);

                robot.mouseMove(Math.round(currentX), Math.round(currentY));

                Thread.sleep(STEP_DELAY_MILLISECONDS);
            }

            onDestinationReached();
        } catch (InterruptedException ex) {
            // the movement was interrupted: the automation was stopped, so
            // just end the movement here
            Thread.currentThread().interrupt();
            LOGGER.debug("Movement interrupted");
            robotTracker.moveEnded(Math.round(currentX), Math.round(currentY));
        } catch (RuntimeException ex) {
            LOGGER.error("Unexpected error while moving the cursor", ex);
            robotTracker.moveEnded(Math.round(currentX), Math.round(currentY));
        }
    }

    private boolean hasReachedDestination() {
        return Math.round(currentX) == destX && Math.round(currentY) == destY;
    }

    private void onDestinationReached() throws InterruptedException {
        LOGGER.info("Reached destination [{}, {}], stopping mover", destX, destY);

        robotTracker.moveEnded(destX, destY);

        if (click) {
            performClickSequence();
        }
    }

    /**
     * Performs a double click with the same pacing the app has always used:
     * press at +500ms, release at +700ms, press at +1200ms, release at +1400ms.
     */
    private void performClickSequence() throws InterruptedException {
        Thread.sleep(500L);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(200L);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(500L);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        Thread.sleep(200L);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    /**
     * Step to take on the X axis so that both axes reach their destination
     * after the same number of steps. Returns 0 when there is no movement to make.
     */
    static float computeStepX(int lengthX, int lengthY) {
        if (lengthX == 0) {
            return 0.0f;
        }
        if (lengthX >= lengthY) {
            return 1.0f;
        }
        return lengthX / (float) lengthY;
    }

    /**
     * Step to take on the Y axis so that both axes reach their destination
     * after the same number of steps. Returns 0 when there is no movement to make.
     */
    static float computeStepY(int lengthX, int lengthY) {
        if (lengthY == 0) {
            return 0.0f;
        }
        if (lengthY > lengthX) {
            return 1.0f;
        }
        return lengthY / (float) lengthX;
    }

    /**
     * When less than a whole step is left to travel, move exactly what is left
     * instead of a whole step, so that the cursor doesn't overshoot the destination.
     */
    static float adjustedStep(float current, float destination, float step) {
        float remaining = Math.abs(current - destination);
        return remaining < 1.0f ? remaining : step;
    }

    /** Moves current towards destination by step, in the right direction. */
    static float advance(float current, float destination, float step) {
        if (current > destination) {
            return current - step;
        }
        if (current < destination) {
            return current + step;
        }
        return current;
    }
}