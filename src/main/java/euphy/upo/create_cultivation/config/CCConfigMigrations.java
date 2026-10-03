package euphy.upo.create_cultivation.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

import net.neoforged.fml.loading.FMLPaths;

/**
 * Config file migration: when a mod update moves, renames or retires config
 * keys, old files are upgraded IN PLACE while the user's modified values are
 * preserved.
 *
 * <p>Mechanism: the config file carries a raw {@code configVersion} stamp
 * (written by the last migration; absent = version 1, the first released
 * layout). Older files get every entry of {@link #RENAMES} applied - a value
 * is only MOVED when the destination key is absent from the file, so a value
 * the user already re-set under the new key always wins - obsolete keys
 * (retired with no equivalent) are dropped, and the stamp is written.
 *
 * <p><b>Timing:</b> the migration runs from the mod constructor, BEFORE the
 * spec is registered with FML. NeoForge's config loader deletes every key the
 * spec does not define (including old keys and a bare {@code configVersion}
 * stamp) and refills missing keys with defaults as soon as it loads the file,
 * so a migration hooked on {@code ModConfigEvent.Loading} would always run
 * after the user values are already gone. {@code configVersion} is defined in
 * the spec itself ({@code CCConfig.CONFIG_VERSION}), which lets the stamp
 * survive that correction pass and keeps the migration one-shot.
 *
 * <p>Adding new keys does NOT need a version bump or a migration: NeoForge
 * fills missing keys with their defaults on load. Only key moves, renames and
 * removals go through here - bump {@link #CURRENT_VERSION} and extend
 * {@link #RENAMES} / {@link #OBSOLETE_KEYS} when the config layout changes.
 */
public final class CCConfigMigrations {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Current config layout version; bump when keys move/rename/retire. */
    public static final int CURRENT_VERSION = 2;

    /** Raw key the file's layout version is stamped under (mirrored in CCConfig.CONFIG_VERSION). */
    private static final String VERSION_KEY = "configVersion";

    /**
     * Moved/renamed keys, old full path -&gt; new full path. A move only happens
     * when the destination is absent from the file, so user edits under the
     * new key always win over the migrated old value.
     */
    private static final Map<String, String> RENAMES = Map.ofEntries(
            // 0.1.5 restructure: the flat tank keys moved under cultivation_tank
            Map.entry("general.growthRateMultiplier", "cultivation_tank.general.growthRateMultiplier"),
            Map.entry("general.cropYieldMultiplier", "cultivation_tank.general.cropYieldMultiplier"),
            Map.entry("watering.wateringYieldBonus", "cultivation_tank.watering.wateringYieldBonus"),
            Map.entry("watering.wateringGrowthBonus", "cultivation_tank.watering.wateringGrowthBonus"),
            Map.entry("watering.wateredDuration", "cultivation_tank.watering.wateredDuration"),
            Map.entry("catalyst.catalysts", "cultivation_tank.catalyst.catalysts"),
            Map.entry("synergy.waterCatalystSynergyBonus", "cultivation_tank.synergy.waterCatalystSynergyBonus"));

    /** Keys retired without an equivalent (dropped on migration). */
    private static final List<String> OBSOLETE_KEYS = List.of(
            "catalyst.catalystGrowthBonus");

    private CCConfigMigrations() {}

    /**
     * Runs the in-place migration from the mod constructor, BEFORE
     * {@code registerConfig} hands the file to FML. Safe to call multiple
     * times; already-migrated files are left untouched.
     */
    public static void migrateEarly() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("create_cultivation-common.toml");
        if (!Files.exists(file)) {
            return;
        }
        try (CommentedFileConfig raw = CommentedFileConfig.builder(file).build()) {
            raw.load();
            int version = raw.contains(VERSION_KEY) && raw.get(VERSION_KEY) instanceof Number n
                    ? n.intValue()
                    : 1;
            if (version >= CURRENT_VERSION) {
                return;
            }
            LOGGER.info("Migrating create_cultivation config from version {} to {}", version, CURRENT_VERSION);
            for (Map.Entry<String, String> rename : RENAMES.entrySet()) {
                if (raw.contains(rename.getKey()) && !raw.contains(rename.getValue())) {
                    raw.set(rename.getValue(), raw.get(rename.getKey()));
                    raw.remove(rename.getKey());
                }
            }
            for (String obsolete : OBSOLETE_KEYS) {
                raw.remove(obsolete);
            }
            raw.set(VERSION_KEY, CURRENT_VERSION);
            raw.save();
        } catch (Exception e) {
            LOGGER.warn("Failed to migrate create_cultivation config at {}: {}", file, e.toString());
        }
    }
}
