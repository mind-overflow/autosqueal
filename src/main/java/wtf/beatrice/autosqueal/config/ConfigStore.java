package wtf.beatrice.autosqueal.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wtf.beatrice.autosqueal.util.SystemUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Loads and saves the settings, as a properties file in the user's config
 * directory.
 *
 * The whole store is defensive: a missing file, garbage values or an
 * unwritable directory degrade to the defaults instead of breaking the
 * app, because the automation must be able to start no matter what.
 * Writing goes through a temp file and an atomic move, so that a crash
 * mid-save can never leave a truncated settings file behind.
 */
public class ConfigStore {

    private static final Logger LOGGER = LogManager.getLogger(ConfigStore.class);

    static final String KEY_AWAY_THRESHOLD_SECONDS = "away.threshold.seconds";
    static final String KEY_MOVE_INTERVAL_SECONDS = "move.interval.seconds";
    static final String KEY_STEP_DELAY_MILLISECONDS = "move.step.delay.milliseconds";
    static final String KEY_CLICK_ENABLED = "click.enabled";
    static final String KEY_CLICK_EVERY_N_MOVES = "click.every.n.moves";
    static final String KEY_MOVEMENT_SCREEN = "move.screen";
    static final String KEY_START_AUTOMATICALLY = "automation.start.automatically";
    static final String KEY_HOTKEY_ENABLED = "hotkey.enabled";

    private final Path file;

    /** A store reading the standard settings file of the host platform. */
    public ConfigStore() {
        this(SystemUtil.getAppConfigDir().resolve("autosqueal.properties"));
    }

    /** A store reading the given file; used by the tests. */
    public ConfigStore(Path file) {
        this.file = file;
    }

    /** Reads the settings, falling back to the defaults for anything missing or broken. */
    public AutoSquealConfig load() {
        AutoSquealConfig config = new AutoSquealConfig();

        if (!Files.isRegularFile(file)) {
            LOGGER.info("No settings file yet, using the defaults");
            return config;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        } catch (IOException ex) {
            LOGGER.warn("Could not read the settings file, using the defaults", ex);
            return config;
        }

        readInt(properties, KEY_AWAY_THRESHOLD_SECONDS, config::setAwayThresholdSeconds);
        readInt(properties, KEY_MOVE_INTERVAL_SECONDS, config::setMoveIntervalSeconds);
        readInt(properties, KEY_STEP_DELAY_MILLISECONDS, config::setStepDelayMilliseconds);
        readInt(properties, KEY_CLICK_EVERY_N_MOVES, config::setClickEveryNMoves);
        readBoolean(properties, KEY_CLICK_ENABLED, config::setClickEnabled);
        readBoolean(properties, KEY_START_AUTOMATICALLY, config::setStartAutomatically);
        readBoolean(properties, KEY_HOTKEY_ENABLED, config::setHotkeyEnabled);
        readEnum(properties, KEY_MOVEMENT_SCREEN, ScreenArea.class, config::setMovementScreen);

        return config;
    }

    /** Writes the settings atomically, so that readers never see a half-written file. */
    public void save(AutoSquealConfig config) {
        Map<String, String> entries = new TreeMap<>();
        entries.put(KEY_AWAY_THRESHOLD_SECONDS, String.valueOf(config.getAwayThresholdSeconds()));
        entries.put(KEY_MOVE_INTERVAL_SECONDS, String.valueOf(config.getMoveIntervalSeconds()));
        entries.put(KEY_STEP_DELAY_MILLISECONDS, String.valueOf(config.getStepDelayMilliseconds()));
        entries.put(KEY_CLICK_ENABLED, String.valueOf(config.isClickEnabled()));
        entries.put(KEY_CLICK_EVERY_N_MOVES, String.valueOf(config.getClickEveryNMoves()));
        entries.put(KEY_MOVEMENT_SCREEN, String.valueOf(config.getMovementScreen()));
        entries.put(KEY_START_AUTOMATICALLY, String.valueOf(config.isStartAutomatically()));
        entries.put(KEY_HOTKEY_ENABLED, String.valueOf(config.isHotkeyEnabled()));

        try {
            Path directory = file.toAbsolutePath().getParent();
            Files.createDirectories(directory);
            Path tempFile = Files.createTempFile(directory, "autosqueal-", ".tmp");

            try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
                writer.write("# autosqueal settings, rewritten by the app when the settings change");
                writer.newLine();
                for (Map.Entry<String, String> entry : entries.entrySet()) {
                    writer.write(entry.getKey());
                    writer.write("=");
                    writer.write(entry.getValue());
                    writer.newLine();
                }
            }

            try {
                Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            LOGGER.error("Could not save the settings", ex);
        }
    }

    private static void readInt(Properties properties, String key, IntConsumer setter) {
        String value = properties.getProperty(key);
        if (value == null) {
            return;
        }

        try {
            setter.accept(Integer.parseInt(value.trim()));
        } catch (NumberFormatException ex) {
            LOGGER.warn("Ignoring invalid value for {}: {}", key, value);
        }
    }

    private static void readBoolean(Properties properties, String key, Consumer<Boolean> setter) {
        String value = properties.getProperty(key);
        if (value == null) {
            return;
        }

        String normalized = value.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            setter.accept(true);
        } else if ("false".equalsIgnoreCase(normalized)) {
            setter.accept(false);
        } else {
            LOGGER.warn("Ignoring invalid value for {}: {}", key, value);
        }
    }

    private static <E extends Enum<E>> void readEnum(Properties properties, String key, Class<E> type, Consumer<E> setter) {
        String value = properties.getProperty(key);
        if (value == null) {
            return;
        }

        try {
            setter.accept(Enum.valueOf(type, value.trim()));
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Ignoring unknown value for {}: {}", key, value);
        }
    }
}