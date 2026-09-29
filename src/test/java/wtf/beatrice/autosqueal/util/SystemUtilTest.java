package wtf.beatrice.autosqueal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemUtilTest
{
    @Test
    void detectsWindows() {
        assertEquals(SystemUtil.OperatingSystem.WINDOWS, SystemUtil.fromOsName("Windows 11"));
    }

    @Test
    void detectsLinux() {
        assertEquals(SystemUtil.OperatingSystem.LINUX, SystemUtil.fromOsName("Linux"));
        assertEquals(SystemUtil.OperatingSystem.LINUX, SystemUtil.fromOsName("GNU/Linux"));
    }

    @Test
    void detectsMacOS() {
        assertEquals(SystemUtil.OperatingSystem.MAC_OS, SystemUtil.fromOsName("Mac OS X"));
    }

    @Test
    void unknownForOtherSystems() {
        assertEquals(SystemUtil.OperatingSystem.UNKNOWN, SystemUtil.fromOsName("SunOS"));
    }

    @Test
    void hostSystemMatchesOsName() {
        assertEquals(SystemUtil.fromOsName(System.getProperty("os.name")), SystemUtil.getHostSystem());
    }
}