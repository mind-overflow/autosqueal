package wtf.beatrice.autosqueal;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.listener.CursorMoveListener;
import wtf.beatrice.autosqueal.listener.KeyPressListener;
import wtf.beatrice.autosqueal.ui.MainWindow;

import java.util.Timer;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private static final MainWindow mainWindow = new MainWindow();

    public static void main(String[] args) {
        LOGGER.info("Hello world!");

        registerJNativeHook();
        mainWindow.init();

        Timer timerRunner = new Timer();
        CursorMoveListener cursorMoveListener = new CursorMoveListener();
        timerRunner.schedule(cursorMoveListener, 0L, 1000L);
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

    public static MainWindow getMainWindow() {
        return mainWindow;
    }

}
