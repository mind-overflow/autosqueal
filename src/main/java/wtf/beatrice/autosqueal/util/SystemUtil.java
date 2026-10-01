package wtf.beatrice.autosqueal.util;

import java.nio.file.Path;
import java.util.Locale;

public class SystemUtil
{

    public static OperatingSystem getHostSystem() {
        return fromOsName(System.getProperty("os.name"));
    }

    /**
     * The per-user directory where the app keeps its settings, following
     * the conventions of the host platform.
     */
    public static Path getAppConfigDir() {
        String home = System.getProperty("user.home", ".");
        String appData = System.getenv("APPDATA");
        String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");

        return switch (getHostSystem()) {
            case MAC_OS -> Path.of(home, "Library", "Application Support", "autosqueal");
            case WINDOWS -> appData != null && !appData.isBlank()
                    ? Path.of(appData, "autosqueal")
                    : Path.of(home, ".autosqueal");
            case LINUX -> xdgConfigHome != null && !xdgConfigHome.isBlank()
                    ? Path.of(xdgConfigHome, "autosqueal")
                    : Path.of(home, ".config", "autosqueal");
            default -> Path.of(home, ".autosqueal");
        };
    }

    public static OperatingSystem fromOsName(String osName) {
        String normalizedOsName = osName.toLowerCase(Locale.ENGLISH);
        if (normalizedOsName.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (normalizedOsName.contains("nix") ||
                normalizedOsName.contains("nux") ||
                normalizedOsName.contains("aix")) {
            return OperatingSystem.LINUX;
        } else if (normalizedOsName.contains("mac")) {
            return OperatingSystem.MAC_OS;
        } else {
            return OperatingSystem.UNKNOWN;
        }
    }

    public enum OperatingSystem {
        WINDOWS, LINUX, MAC_OS, UNKNOWN;
    }
}
