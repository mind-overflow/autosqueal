package wtf.beatrice.autosqueal.listener;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.junit.jupiter.api.Test;
import wtf.beatrice.autosqueal.config.AutoSquealConfig;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyPressListenerTest
{
    private final List<String> events = new ArrayList<>();
    private final AutoSquealConfig config = new AutoSquealConfig();
    private final KeyPressListener listener = new KeyPressListener(
            () -> events.add("toggle"), () -> events.add("activity"), config);

    private static NativeKeyEvent press(int keyCode) {
        return new NativeKeyEvent(NativeKeyEvent.NATIVE_KEY_PRESSED, 0, 0, keyCode, ' ');
    }

    private static NativeKeyEvent release(int keyCode) {
        return new NativeKeyEvent(NativeKeyEvent.NATIVE_KEY_RELEASED, 0, 0, keyCode, ' ');
    }

    @Test
    void theComboTogglesOnceOnItsSecondKey() {
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_CONTROL));

        // ctrl alone is not the combo: it only reports presence
        assertEquals(List.of("activity"), events);

        listener.nativeKeyPressed(press(NativeKeyEvent.VC_ALT));

        // the second key completes the combo: presence first, then the toggle
        assertEquals(List.of("activity", "activity", "toggle"), events);
    }

    @Test
    void theComboWorksInTheOtherOrderToo() {
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_ALT));
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_CONTROL));

        assertEquals(List.of("activity", "activity", "toggle"), events);
    }

    @Test
    void repeatedPressesAreIgnored() {
        // the OS repeats press events while a key is held down: they must
        // not toggle repeatedly
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_CONTROL));
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_ALT));

        events.clear();
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_CONTROL));
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_ALT));

        assertEquals(List.of(), events);
    }

    @Test
    void theComboCanFireAgainAfterBothKeysAreReleased() {
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_CONTROL));
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_ALT));
        listener.nativeKeyReleased(release(NativeKeyEvent.VC_CONTROL));
        listener.nativeKeyReleased(release(NativeKeyEvent.VC_ALT));

        events.clear();
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_CONTROL));
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_ALT));

        assertEquals(List.of("activity", "activity", "toggle"), events);
    }

    @Test
    void theHotkeyCanBeDisabledWithoutLosingPresenceReporting() {
        config.setHotkeyEnabled(false);

        listener.nativeKeyPressed(press(NativeKeyEvent.VC_CONTROL));
        listener.nativeKeyPressed(press(NativeKeyEvent.VC_ALT));

        // typing still counts as presence, but the combo does nothing
        assertEquals(List.of("activity", "activity"), events);
    }
}