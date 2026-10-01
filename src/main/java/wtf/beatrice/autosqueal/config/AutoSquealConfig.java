package wtf.beatrice.autosqueal.config;

/**
 * The user-editable settings of the app.
 *
 * A single instance is shared between the components, which read the
 * current values while running: a change is picked up by the next poll or
 * movement without a restart. Values are clamped to sane ranges when set,
 * so that no matter what ends up in the settings file, the automation
 * always gets workable numbers.
 */
public class AutoSquealConfig {

    public static final int AWAY_THRESHOLD_MIN_SECONDS = 5;
    public static final int AWAY_THRESHOLD_MAX_SECONDS = 600;

    public static final int MOVE_INTERVAL_MIN_SECONDS = 1;
    public static final int MOVE_INTERVAL_MAX_SECONDS = 3600;

    public static final int STEP_DELAY_MIN_MILLISECONDS = 1;
    public static final int STEP_DELAY_MAX_MILLISECONDS = 50;

    public static final int CLICK_EVERY_MIN_MOVES = 1;
    public static final int CLICK_EVERY_MAX_MOVES = 100;

    private volatile int awayThresholdSeconds = 30;
    private volatile int moveIntervalSeconds = 10;
    private volatile int stepDelayMilliseconds = 2;
    private volatile boolean clickEnabled = true;
    private volatile int clickEveryNMoves = 5;
    private volatile ScreenArea movementScreen = ScreenArea.PRIMARY;
    private volatile boolean startAutomatically = true;
    private volatile boolean hotkeyEnabled = true;

    /** Seconds without user activity after which the user is considered away. */
    public int getAwayThresholdSeconds() {
        return awayThresholdSeconds;
    }

    public void setAwayThresholdSeconds(int seconds) {
        awayThresholdSeconds = clamp(seconds, AWAY_THRESHOLD_MIN_SECONDS, AWAY_THRESHOLD_MAX_SECONDS);
    }

    /** Seconds between two movements. */
    public int getMoveIntervalSeconds() {
        return moveIntervalSeconds;
    }

    public void setMoveIntervalSeconds(int seconds) {
        moveIntervalSeconds = clamp(seconds, MOVE_INTERVAL_MIN_SECONDS, MOVE_INTERVAL_MAX_SECONDS);
    }

    /** Pause between two cursor steps, in milliseconds: the lower, the faster. */
    public int getStepDelayMilliseconds() {
        return stepDelayMilliseconds;
    }

    public void setStepDelayMilliseconds(int milliseconds) {
        stepDelayMilliseconds = clamp(milliseconds, STEP_DELAY_MIN_MILLISECONDS, STEP_DELAY_MAX_MILLISECONDS);
    }

    /** Whether the corner double click is performed at all. */
    public boolean isClickEnabled() {
        return clickEnabled;
    }

    public void setClickEnabled(boolean enabled) {
        clickEnabled = enabled;
    }

    /**
     * One movement every N is a corner double click, so that the
     * notification area gets exercised too.
     */
    public int getClickEveryNMoves() {
        return clickEveryNMoves;
    }

    public void setClickEveryNMoves(int moves) {
        clickEveryNMoves = clamp(moves, CLICK_EVERY_MIN_MOVES, CLICK_EVERY_MAX_MOVES);
    }

    /** Which screens the cursor may travel to. */
    public ScreenArea getMovementScreen() {
        return movementScreen;
    }

    public void setMovementScreen(ScreenArea screenArea) {
        movementScreen = screenArea == null ? ScreenArea.PRIMARY : screenArea;
    }

    /** Whether the automation starts as soon as the window opens. */
    public boolean isStartAutomatically() {
        return startAutomatically;
    }

    public void setStartAutomatically(boolean automatically) {
        startAutomatically = automatically;
    }

    /** Whether the global toggle hotkey is active. */
    public boolean isHotkeyEnabled() {
        return hotkeyEnabled;
    }

    public void setHotkeyEnabled(boolean enabled) {
        hotkeyEnabled = enabled;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}