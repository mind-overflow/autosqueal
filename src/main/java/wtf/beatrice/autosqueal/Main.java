package wtf.beatrice.autosqueal;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.Random;
import java.util.Timer;

public class Main {

    private static final int LOOPS_BEFORE_CLICK = 5;
    private static final int TIME_BETWEEN_MOVES = 5;

    private static boolean running = true;

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException, AWTException {
        LOGGER.info("Hello world!");

        registerJNativeHook();

        Timer timer = new Timer();
        CursorMoveListener cursorMoveListener = new CursorMoveListener();
        timer.schedule(cursorMoveListener, 0L, 1000L);

        int height = Toolkit.getDefaultToolkit().getScreenSize().height;
        int width = Toolkit.getDefaultToolkit().getScreenSize().width;

        Random random = new Random();

        while (running) {
            //if(!cursorMoveListener.isUserAway()) continue;

            for(int i = 0; i < LOOPS_BEFORE_CLICK; i++) {

                int currentX = MouseInfo.getPointerInfo().getLocation().x;
                int currentY = MouseInfo.getPointerInfo().getLocation().y;

                LOGGER.info("Starting coordinates: {}, {}", currentX, currentY);

                int randomX;
                int randomY;

                randomX = random.nextInt(width);
                randomY = random.nextInt(height);

                LOGGER.info("Destination coordinates: {}, {}", randomX, randomY);

                startMover(randomX, randomY);
            }

            startMover(width - 5, 5);
            Robot robot = new Robot();
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(200);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(500);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(200);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(200);

        }
    }

    private static void registerJNativeHook() {
        LOGGER.info("Registering jnativehook library...");
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new KeyPressListener());
            LOGGER.info("Successfully registered jnativehook library!");
        }
        catch (NativeHookException ex) {
            LOGGER.error("There was a problem registering the native hook.");
            LOGGER.error(ex.getMessage());

            System.exit(1);
        }
    }

    private static void startMover(int destX, int destY) throws InterruptedException {
        Timer timer = new Timer();
        CursorMover cursorMover = new CursorMover(destX, destY);
        timer.schedule(cursorMover, 0L, 2L);
        while (cursorMover.isRunning()) {
            Thread.sleep(200);
        }

        timer.cancel();

        Thread.sleep(TIME_BETWEEN_MOVES * 1000L);
    }
}
