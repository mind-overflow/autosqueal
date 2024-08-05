package wtf.beatrice.autosqueal.util;

import java.util.Locale;

public class SystemUtil
{

    public static OperatingSystem getHostSystem() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (osName.contains("nix") ||
                osName.contains("nux") ||
                osName.contains("aix")) {
            return OperatingSystem.LINUX;
        } else if (osName.contains("mac")) {
            return OperatingSystem.MAC_OS;
        } else {
            return OperatingSystem.UNKNOWN;
        }
    }

    public enum OperatingSystem {
        WINDOWS, LINUX, MAC_OS, UNKNOWN;
    }
}
