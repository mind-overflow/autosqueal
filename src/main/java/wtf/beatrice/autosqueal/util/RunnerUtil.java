package wtf.beatrice.autosqueal.util;

import java.awt.*;

public class RunnerUtil {

    private RunnerUtil() {
        throw new AssertionError("The RunnerUtil class is not intended to be instantiated.");
    }

    public static final int SECONDS_BETWEEN_MOVES = 10;

    public static final int SCREEN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
    public static final int SCREEN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;

}
