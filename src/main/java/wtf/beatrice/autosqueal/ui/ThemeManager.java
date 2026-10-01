package wtf.beatrice.autosqueal.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.util.SystemUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Keeps the look and feel in sync with the system appearance.
 *
 * On macOS the dark or light mode is read from the system, so the app
 * follows the user's taste without any setting of its own; everywhere
 * else the light theme is used. The check runs again every time the
 * window is activated, so switching the system appearance while the app
 * runs is picked up without a restart.
 */
public final class ThemeManager {

    private static final Logger LOGGER = LogManager.getLogger(ThemeManager.class);

    /** The theme currently applied, so a change of system appearance is noticed. */
    private static boolean darkThemeApplied = false;

    private ThemeManager() {
        throw new AssertionError("The ThemeManager class is not intended to be instantiated.");
    }

    /** Applies the theme matching the current system appearance. */
    public static void applySystemTheme() {
        boolean systemDark = isSystemDarkMode();
        applyTheme(systemDark);
    }

    /** Re-checks the system appearance, and switches theme if it changed. */
    public static void reapplyIfSystemThemeChanged() {
        boolean systemDark = isSystemDarkMode();
        if (systemDark != darkThemeApplied) {
            applyTheme(systemDark);
        }
    }

    private static void applyTheme(boolean dark) {
        try {
            FlatLaf.setup(dark ? new FlatMacDarkLaf() : new FlatMacLightLaf());
            darkThemeApplied = dark;
        } catch (RuntimeException ex) {
            // a broken theme must never keep the app from starting
            LOGGER.error("Could not apply the flat theme, using the default look and feel", ex);
        }
    }

    /**
     * Whether the system is in dark mode. On macOS, the appearance lives
     * in AppleInterfaceStyle, which simply does not exist while the system
     * is in light mode.
     */
    private static boolean isSystemDarkMode() {
        if (SystemUtil.getHostSystem() != SystemUtil.OperatingSystem.MAC_OS) {
            return false;
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "defaults", "read", "NSGlobalDomain", "AppleInterfaceStyle");
            builder.redirectErrorStream(true);
            Process process = builder.start();

            String style;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                style = reader.readLine();
            }

            boolean exited = process.waitFor() == 0;
            return exited && "Dark".equalsIgnoreCase(style == null ? "" : style.trim());
        } catch (IOException | InterruptedException ex) {
            // if the check fails, prefer the light theme
            LOGGER.debug("Could not read the system appearance, assuming light", ex);
            return false;
        }
    }
}