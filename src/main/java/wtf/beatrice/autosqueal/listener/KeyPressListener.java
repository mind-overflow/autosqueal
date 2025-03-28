package wtf.beatrice.autosqueal.listener;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.Main;

import java.util.ArrayList;
import java.util.List;

public class KeyPressListener implements NativeKeyListener
{
    private static final Logger LOGGER = LogManager.getLogger(KeyPressListener.class);
    private static final List<Integer> pressedKeysIds = new ArrayList<>();

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        String key = NativeKeyEvent.getKeyText(e.getKeyCode());
        LOGGER.info("Key Pressed: {}", key);

        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException nativeHookException) {
                LOGGER.error("Failed to unregister native hook", nativeHookException);
            }
        }

        pressedKeysIds.add(e.getKeyCode());

        handlePressedKeys();
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        String key = NativeKeyEvent.getKeyText(e.getKeyCode());
        LOGGER.info("Key Released: {}", key);

        pressedKeysIds.remove((Integer) e.getKeyCode());

        handlePressedKeys();
    }

    private void handlePressedKeys() {
        if (pressedKeysIds.contains(NativeKeyEvent.VC_ALT) &&
        pressedKeysIds.contains(NativeKeyEvent.VC_CONTROL)) {

            StringBuilder keys = new StringBuilder();
            pressedKeysIds.forEach(keyCode -> keys.append("[").append(NativeKeyEvent.getKeyText(keyCode)).append("]"));

            LOGGER.warn("Received shutdown keystroke: {}", keys);

            Main.getMainWindow().toggleRunning();
        }
    }
}
