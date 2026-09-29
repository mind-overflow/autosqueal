package wtf.beatrice.autosqueal.listener;

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
    private final List<Integer> pressedKeys = new ArrayList<>();

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // the OS fires repeated "press" events while a key is being held down; ignore them,
        // otherwise holding the toggle combo down would rapidly toggle the app on and off
        if (pressedKeys.contains(e.getKeyCode())) {
            return;
        }

        pressedKeys.add(e.getKeyCode());
        LOGGER.info("Key Pressed: {}", NativeKeyEvent.getKeyText(e.getKeyCode()));

        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            Main.unregisterJNativeHook();
        }

        // toggle only when the second key of the combo is pressed down: not only does this
        // fire exactly once per combo, but it also avoids toggling when any other key is
        // pressed or released while ctrl+alt happen to be held down
        if ((e.getKeyCode() == NativeKeyEvent.VC_CONTROL && pressedKeys.contains(NativeKeyEvent.VC_ALT))
                || (e.getKeyCode() == NativeKeyEvent.VC_ALT && pressedKeys.contains(NativeKeyEvent.VC_CONTROL))) {
            LOGGER.warn("Received shutdown keystroke: [{}][{}]",
                    NativeKeyEvent.getKeyText(NativeKeyEvent.VC_CONTROL),
                    NativeKeyEvent.getKeyText(NativeKeyEvent.VC_ALT));

            Main.getMainWindow().toggleRunning();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        LOGGER.info("Key Released: {}", NativeKeyEvent.getKeyText(e.getKeyCode()));

        pressedKeys.remove((Integer) e.getKeyCode());
    }
}