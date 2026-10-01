package wtf.beatrice.autosqueal.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigStoreTest
{
    @TempDir
    Path tempDir;

    private ConfigStore store;

    @BeforeEach
    void createStore() {
        store = new ConfigStore(tempDir.resolve("autosqueal.properties"));
    }

    @Test
    void missingFileYieldsTheDefaults() {
        AutoSquealConfig config = store.load();

        assertEquals(30, config.getAwayThresholdSeconds());
        assertEquals(10, config.getMoveIntervalSeconds());
        assertEquals(2, config.getStepDelayMilliseconds());
        assertTrue(config.isClickEnabled());
        assertEquals(5, config.getClickEveryNMoves());
        assertEquals(ScreenArea.PRIMARY, config.getMovementScreen());
        assertTrue(config.isStartAutomatically());
        assertTrue(config.isHotkeyEnabled());
    }

    @Test
    void savedSettingsLoadBack() {
        AutoSquealConfig config = new AutoSquealConfig();
        config.setAwayThresholdSeconds(45);
        config.setMoveIntervalSeconds(20);
        config.setStepDelayMilliseconds(4);
        config.setClickEnabled(false);
        config.setClickEveryNMoves(7);
        config.setMovementScreen(ScreenArea.ALL);
        config.setStartAutomatically(false);
        config.setHotkeyEnabled(false);

        store.save(config);
        AutoSquealConfig loaded = store.load();

        assertEquals(45, loaded.getAwayThresholdSeconds());
        assertEquals(20, loaded.getMoveIntervalSeconds());
        assertEquals(4, loaded.getStepDelayMilliseconds());
        assertFalse(loaded.isClickEnabled());
        assertEquals(7, loaded.getClickEveryNMoves());
        assertEquals(ScreenArea.ALL, loaded.getMovementScreen());
        assertFalse(loaded.isStartAutomatically());
        assertFalse(loaded.isHotkeyEnabled());
    }

    @Test
    void outOfRangeValuesAreClamped() throws IOException {
        writeFile(ConfigStore.KEY_AWAY_THRESHOLD_SECONDS + "=0",
                ConfigStore.KEY_MOVE_INTERVAL_SECONDS + "=99999999",
                ConfigStore.KEY_STEP_DELAY_MILLISECONDS + "=-3",
                ConfigStore.KEY_CLICK_EVERY_N_MOVES + "=1000");

        AutoSquealConfig config = store.load();

        assertEquals(5, config.getAwayThresholdSeconds());
        assertEquals(3600, config.getMoveIntervalSeconds());
        assertEquals(1, config.getStepDelayMilliseconds());
        assertEquals(100, config.getClickEveryNMoves());
    }

    @Test
    void invalidValuesKeepTheDefaults() throws IOException {
        writeFile(ConfigStore.KEY_AWAY_THRESHOLD_SECONDS + "=soon",
                ConfigStore.KEY_CLICK_ENABLED + "=banana",
                ConfigStore.KEY_MOVEMENT_SCREEN + "=THE_MOON",
                ConfigStore.KEY_MOVE_INTERVAL_SECONDS + "=15");

        AutoSquealConfig config = store.load();

        assertEquals(30, config.getAwayThresholdSeconds());
        assertTrue(config.isClickEnabled());
        assertEquals(ScreenArea.PRIMARY, config.getMovementScreen());
        // the one valid value still loads
        assertEquals(15, config.getMoveIntervalSeconds());
    }

    @Test
    void settingsSettersClampToo() {
        AutoSquealConfig config = new AutoSquealConfig();

        config.setAwayThresholdSeconds(-10);
        assertEquals(5, config.getAwayThresholdSeconds());

        config.setAwayThresholdSeconds(10_000);
        assertEquals(600, config.getAwayThresholdSeconds());

        config.setMovementScreen(null);
        assertEquals(ScreenArea.PRIMARY, config.getMovementScreen());
    }

    @Test
    void savingNeverLeavesTempFilesBehind() throws IOException {
        store.save(new AutoSquealConfig());

        assertTrue(Files.isRegularFile(tempDir.resolve("autosqueal.properties")));
        try (var files = Files.list(tempDir)) {
            assertEquals(1, files.count());
        }
    }

    private void writeFile(String... lines) throws IOException {
        Files.write(tempDir.resolve("autosqueal.properties"), List.of(lines));
    }
}