package wtf.beatrice.autosqueal.config;

/**
 * Which screens the automation is allowed to move the cursor to.
 */
public enum ScreenArea {

    /** The main screen only, as the app has always used. */
    PRIMARY,

    /** Every connected screen, treated as one big virtual screen. */
    ALL
}