package wtf.beatrice.autosqueal.util;

import java.util.Locale;

public class SystemUtil
{

    public static OperatingSystem getHostSystem() {
        return fromOsName(System.getProperty("os.name"));
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
