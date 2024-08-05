package wtf.beatrice.autosqueal.controls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class CursorMover extends TimerTask
{

    private static final Logger LOGGER = LogManager.getLogger(CursorMover.class);
    private final Random RANDOM;

    private final int SCREEN_HEIGHT;
    private final int SCREEN_WIDTH;

    public CursorMover(int screenWidth, int screenHeight) {
        RANDOM = new SecureRandom();

        this.SCREEN_WIDTH = screenWidth;
        this.SCREEN_HEIGHT = screenHeight;
    }

    @Override
    public void run() {

        int currentX = MouseInfo.getPointerInfo().getLocation().x;
        int currentY = MouseInfo.getPointerInfo().getLocation().y;

        LOGGER.info("Starting coordinates: {}, {}", currentX, currentY);

        int destX = RANDOM.nextInt(SCREEN_WIDTH);
        int destY = RANDOM.nextInt(SCREEN_HEIGHT);

        LOGGER.info("Destination coordinates: {}, {}", destX, destY);

        Timer timer = new Timer();
        SingleStepMovementTask singleStepMovementTask = new SingleStepMovementTask(destX, destY);
        timer.schedule(singleStepMovementTask, 0L, 2L);
    }
}
