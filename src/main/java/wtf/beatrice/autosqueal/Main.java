package wtf.beatrice.autosqueal;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.config.AutoSquealConfig;
import wtf.beatrice.autosqueal.config.ConfigStore;
import wtf.beatrice.autosqueal.listener.KeyPressListener;
import wtf.beatrice.autosqueal.ui.MainWindow;
import wtf.beatrice.autosqueal.ui.ThemeManager;
import wtf.beatrice.autosqueal.util.SystemUtil;

import javax.swing.*;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private static MainWindow mainWindow;
    private static AutoSquealConfig config;

    /** Whether the hook and its listener are registered, so retries don't add duplicates. */
    private static boolean hookRegistered = false;

    public static void main(String[] args) {
        LOGGER.info("Hello world!");

        // macos window chrome: these must be set before AWT initializes,
        // or they are ignored
        if (SystemUtil.getHostSystem() == SystemUtil.OperatingSystem.MAC_OS) {
            // follow the system appearance for the window title bars
            System.setProperty("apple.awt.application.appearance", "system");
            // name the app in the menu bar
            System.setProperty("apple.awt.application.name", "autosqueal");
        }

        ConfigStore configStore = new ConfigStore();
        config = configStore.load();
        mainWindow = new MainWindow(config, configStore);

        // ⌘Q on macOS, the IDE stop button and signals all bypass
        // windowClosing: make cleanup run on every exit path instead
        Runtime.getRuntime().addShutdownHook(new Thread(mainWindow::cleanup, "autosqueal-shutdown"));

        ThemeManager.applySystemTheme();
        boolean hookRegistered = registerJNativeHook();

        // all Swing components must be created and updated on the EDT
        SwingUtilities.invokeLater(() -> {
            mainWindow.init();
            if (!hookRegistered) {
                mainWindow.showPermissionsHelp();
            }
        });
    }

    public static boolean registerJNativeHook() {
        if (hookRegistered) {
            return true;
        }

        LOGGER.info("Registering jnativehook library...");
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(
                    new KeyPressListener(mainWindow::toggleRunning, mainWindow::notifyKeyboardActivity, config));
            hookRegistered = true;
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
        if (!hookRegistered) {
            return;
        }

        try {
            GlobalScreen.unregisterNativeHook();
            LOGGER.info("Successfully unregistered jnativehook library!");
        }
        catch (NativeHookException ex) {
            LOGGER.error("There was a problem unregistering the native hook.");
            LOGGER.error(ex.getMessage());
        }
    }

}