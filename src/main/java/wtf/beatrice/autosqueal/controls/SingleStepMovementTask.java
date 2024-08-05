package wtf.beatrice.autosqueal.controls;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.TimerTask;

public class SingleStepMovementTask extends TimerTask {

    private static final Logger LOGGER = LogManager.getLogger(SingleStepMovementTask.class);

    final int destX;
    final int destY;
    final Robot robot;

    float currentX;
    float currentY;

    float stepX = 1;
    float stepY = 1;
    boolean isRunning = true;


    public SingleStepMovementTask(int destinationX, int destinationY) {

        currentX = MouseInfo.getPointerInfo().getLocation().x;
        currentY = MouseInfo.getPointerInfo().getLocation().y;

        destX = destinationX;
        destY = destinationY;

        int lengthX;
        int lengthY;

        lengthX = Math.round(Math.abs(currentX - destX));
        lengthY = Math.round(Math.abs(currentY - destY));

        if(lengthX > lengthY) {
            stepX = lengthX / (float) lengthY;
        }

        if(lengthY > lengthX) {
            stepY = lengthY / (float) lengthX;
        }

        LOGGER.info("Dest: [{}, {}], Curr: [{}, {}]", destX, destY, currentX, currentY);
        LOGGER.info("Len: [{}, {}]", lengthX, lengthY);
        LOGGER.info("Step: [{}, {}]", stepX, stepY);

        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void run() {
        if(Math.abs(currentX - destX) < 1) {
            stepX = currentX - destX;
        }

        if(Math.abs(currentY - destY) < 1) {
            stepY = currentY - destY;
        }

        if(destX == Math.round(currentX) || destY == Math.round(currentY)) {
            LOGGER.info("Reached destination, stopping mover timer");
            LOGGER.info("Dest: [{}, {}], Curr: [{}, {}]", destX, destY, currentX, currentY);
            isRunning = false;
            this.cancel();
            return;
        }

        if(currentX > destX) {
            currentX -= stepX;
        }

        if(currentY > destY) {
            currentY -= stepY;
        }

        if(currentX < destX) {
            currentX += stepX;
        }

        if(currentY < destY) {
            currentY += stepY;
        }

        robot.mouseMove(Math.round(currentX), Math.round(currentY));
    }

    public boolean isRunning() {
        return isRunning;
    }
}