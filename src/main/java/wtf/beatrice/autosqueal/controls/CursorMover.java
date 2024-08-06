package wtf.beatrice.autosqueal.controls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.util.RunnerUtil;

import java.awt.*;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class CursorMover extends TimerTask
{

    private static final Logger LOGGER = LogManager.getLogger(CursorMover.class);
    private final Random random;
    private SingleStepMovementTask singleStepMovementTask;

    private static final int LOOPS_BEFORE_CLICK = 5;

    private int iteration = 0;

    public CursorMover() {
        random = new SecureRandom();
        singleStepMovementTask = null;
    }

    @Override
    public void run() {

        int currentX = MouseInfo.getPointerInfo().getLocation().x;
        int currentY = MouseInfo.getPointerInfo().getLocation().y;

        LOGGER.info("Starting coordinates: {}, {}", currentX, currentY);

        int destX;
        int destY;

        if (iteration == LOOPS_BEFORE_CLICK) {
            destX = RunnerUtil.SCREEN_WIDTH - 5;
            destY = 5;

            try {
                singleStepMovementTask = new SingleStepMovementTask(destX, destY, true);
            } catch (AWTException e) {
                LOGGER.error(e);
                return;
            }

            iteration = 0;
        } else {
            destX = random.nextInt(RunnerUtil.SCREEN_WIDTH);
            destY = random.nextInt(RunnerUtil.SCREEN_HEIGHT);

            try {
                singleStepMovementTask = new SingleStepMovementTask(destX, destY, false);
            } catch (AWTException e) {
                LOGGER.error(e);
                return;
            }

            iteration++;
        }

        LOGGER.info("Destination coordinates: {}, {}", destX, destY);

        Timer timer = new Timer();
        timer.schedule(singleStepMovementTask, 0L, 2L);
    }

    public boolean isRunning() {
        if (singleStepMovementTask == null) return false;

        return singleStepMovementTask.isRunning();
    }
}
