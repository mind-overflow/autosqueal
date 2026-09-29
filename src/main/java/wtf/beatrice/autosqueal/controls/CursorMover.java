package wtf.beatrice.autosqueal.controls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.util.RunnerUtil;

import java.awt.*;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Periodically starts a new movement of the mouse cursor to a random position,
 * or to the top-right corner of the screen for a double click.
 *
 * The movement itself is executed by a {@link SingleStepMovementTask}, which
 * re-schedules itself on the shared scheduler until it reaches its destination.
 */
public class CursorMover implements Runnable
{
    private static final Logger LOGGER = LogManager.getLogger(CursorMover.class);

    private static final int LOOPS_BEFORE_CLICK = 5;

    private final Random random = new SecureRandom();
    private final ScheduledExecutorService scheduler;

    private int iteration = 0;

    public CursorMover(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void run() {

        Point location = MouseInfo.getPointerInfo().getLocation();
        LOGGER.info("Starting coordinates: {}, {}", location.x, location.y);

        int destX;
        int destY;
        boolean click;

        if (iteration == LOOPS_BEFORE_CLICK) {
            destX = RunnerUtil.SCREEN_WIDTH - 5;
            destY = 5;
            click = true;

            iteration = 0;
        } else {
            destX = random.nextInt(RunnerUtil.SCREEN_WIDTH);
            destY = random.nextInt(RunnerUtil.SCREEN_HEIGHT);
            click = false;

            iteration++;
        }

        LOGGER.info("Destination coordinates: {}, {}", destX, destY);

        try {
            SingleStepMovementTask movement = new SingleStepMovementTask(scheduler, destX, destY, click);
            scheduler.execute(movement);
        } catch (AWTException ex) {
            LOGGER.error("Could not start movement task", ex);
        }
    }
}