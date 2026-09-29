package wtf.beatrice.autosqueal;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.listener.KeyPressListener;
import wtf.beatrice.autosqueal.ui.MainWindow;

import javax.swing.*;
import javax.swing.UnsupportedLookAndFeelException;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private static final MainWindow mainWindow = new MainWindow();

    public static void main(String[] args) {
        LOGGER.info("Hello world!");

        useSystemLookAndFeel();
        boolean hookRegistered = registerJNativeHook();

        // all Swing components must be created and updated on the EDT
        SwingUtilities.invokeLater(() -> {
            mainWindow.init();
            if (!hookRegistered) {
                mainWindow.showPermissionsHelp();
            }
        });
    }

    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ex) {
            LOGGER.warn("Could not set the system look and feel, using the default one", ex);
        }
    }

    public static boolean registerJNativeHook() {
        LOGGER.info("Registering jnativehook library...");
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new KeyPressListener());
            LOGGER.info("Successfully registered jnativehook library!");
            return true;
        }
        catch (NativeHookException ex) {
            LOGGER.error("There was a problem registering the native hook.", ex);

            // without the accessibility permission the hotkey cannot be
            // registered and the mouse cannot be moved either: the app would
            // look alive but do nothing. don't exit: keep the window and the
            // tray icon usable, and tell the user what is missing.
            return false;
        }
    }

    public static void unregisterJNativeHook() {
        try {
            GlobalScreen.unregisterNativeHook();
            LOGGER.info("Successfully unregistered jnativehook library!");
        }
        catch (NativeHookException ex) {
            LOGGER.error("There was a problem unregistering the native hook.");
            LOGGER.error(ex.getMessage());
        }
    }

    public static MainWindow getMainWindow() {
        return mainWindow;
    }

}