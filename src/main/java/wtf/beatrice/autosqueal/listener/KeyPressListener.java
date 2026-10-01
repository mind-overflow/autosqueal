package wtf.beatrice.autosqueal.listener;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.config.AutoSquealConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Listens for the global toggle hotkey, and reports every key press to the
 * away detection: a user who is typing is present, hotkey or not.
 *
 * The listener is fully decoupled: it receives the actions to run instead
 * of reaching into the main window, so it can be wired — and tested — on
 * its own.
 */
public class KeyPressListener implements NativeKeyListener {

    private static final Logger LOGGER = LogManager.getLogger(KeyPressListener.class);

    private final Runnable toggleAction;
    private final Runnable keyboardActivityReporter;
    private final AutoSquealConfig config;
    private final List<Integer> pressedKeys = new ArrayList<>();

    public KeyPressListener(Runnable toggleAction, Runnable keyboardActivityReporter, AutoSquealConfig config) {
        this.toggleAction = toggleAction;
        this.keyboardActivityReporter = keyboardActivityReporter;
        this.config = config;
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // the OS fires repeated "press" events while a key is being held down; ignore them,
        // otherwise holding the toggle combo down would rapidly toggle the app on and off
        if (pressedKeys.contains(e.getKeyCode())) {
            return;
        }

        pressedKeys.add(e.getKeyCode());
        LOGGER.debug("Key Pressed: {}", NativeKeyEvent.getKeyText(e.getKeyCode()));

        // any key the user presses means they are present: let the away
        // detection know, so the app doesn't grab the mouse while they type
        keyboardActivityReporter.run();

        if (!config.isHotkeyEnabled()) {
            return;
        }

        // toggle only when the second key of the combo is pressed down: not only does this
        // fire exactly once per combo, but it also avoids toggling when any other key is
        // pressed or released while ctrl+alt happen to be held down
        if ((e.getKeyCode() == NativeKeyEvent.VC_CONTROL && pressedKeys.contains(NativeKeyEvent.VC_ALT))
                || (e.getKeyCode() == NativeKeyEvent.VC_ALT && pressedKeys.contains(NativeKeyEvent.VC_CONTROL))) {
            LOGGER.info("Received toggle hotkey: [{}][{}]",
                    NativeKeyEvent.getKeyText(NativeKeyEvent.VC_CONTROL),
                    NativeKeyEvent.getKeyText(NativeKeyEvent.VC_ALT));

            toggleAction.run();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        LOGGER.debug("Key Released: {}", NativeKeyEvent.getKeyText(e.getKeyCode()));

        pressedKeys.remove((Integer) e.getKeyCode());
    }
}