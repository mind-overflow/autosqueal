package wtf.beatrice.autosqueal.listener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.Main;
import wtf.beatrice.autosqueal.util.RunnerUtil;

import java.awt.*;
import java.util.TimerTask;

public class CursorMoveListener extends TimerTask {

    private int oldX;
    private int oldY;

    private int loops;
    private boolean isUserAway;

    private static final Logger LOGGER = LogManager.getLogger(CursorMoveListener.class);


    public CursorMoveListener() {
        oldX = 0;
        oldY = 0;
        loops = 0;
    }

    @Override
    public void run() {
        if(RunnerUtil.isSquealing()) return;

        int newX = MouseInfo.getPointerInfo().getLocation().x;
        int newY = MouseInfo.getPointerInfo().getLocation().y;

        if(newX != oldX || newY != oldY) {
            // cursor has been moved
            loops = 0;
            LOGGER.info("User is no longer away!");
        } else {
            if (loops < 10) {
                loops++;
            } else {
                LOGGER.info("User is away!");
                Main.getMainWindow().toggleRunning();
            }
        }

        isUserAway = loops >= 10;

        oldX = newX;
        oldY = newY;
    }

    public boolean isUserAway() {
        return isUserAway;
    }
}
