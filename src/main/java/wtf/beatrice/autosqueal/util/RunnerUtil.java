package wtf.beatrice.autosqueal.util;

import wtf.beatrice.autosqueal.config.ScreenArea;

import java.awt.*;

public class RunnerUtil {

    private RunnerUtil() {
        throw new AssertionError("The RunnerUtil class is not intended to be instantiated.");
    }

    public static final int SCREEN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
    public static final int SCREEN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;

    /**
     * The bounds of the screens the cursor may travel to, according to the
     * given setting. Fetched on every call, so that a change of setting, or
     * a screen plugged in while the app runs, is picked up by the next
     * movement.
     *
     * The result is never empty: when no screen is found, the main screen
     * is returned, because it always exists.
     */
    public static Rectangle screenBounds(ScreenArea area) {
        Rectangle bounds = new Rectangle();

        if (area == ScreenArea.ALL) {
            for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                bounds.add(device.getDefaultConfiguration().getBounds());
            }
        }

        if (bounds.isEmpty()) {
            // the main screen was requested, or no other screen was found
            bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        }

        // defensive copy: the callers treat the rectangle as theirs
        return new Rectangle(bounds);
    }

}
