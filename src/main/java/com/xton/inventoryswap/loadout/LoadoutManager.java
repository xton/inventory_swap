package com.xton.inventoryswap.loadout;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Loads and saves each player's inventory loadouts to a YAML file in the plugin's data folder.
 */
public class LoadoutManager {

    private final JavaPlugin plugin;
    private final File playerDataFolder;
    private final Map<UUID, PlayerLoadoutData> cache = new HashMap<>();

    public LoadoutManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    public PlayerLoadoutData getData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public PlayerLoadoutData getData(Player player) {
        return getData(player.getUniqueId());
    }

    public void save(UUID uuid) {
        PlayerLoadoutData data = cache.get(uuid);
        if (data != null) {
            save(uuid, data);
        }
    }

    public void save(Player player) {
        save(player.getUniqueId());
    }

    public void unload(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerLoadoutData data = cache.remove(uuid);
        if (data != null) {
            save(uuid, data);
        }
    }

    /**
     * Returns the names of every loadout any player has, for use in tab completion.
     */
    public Set<String> getAllKnownLoadoutNames() {
        Set<UUID> uuids = new HashSet<>(cache.keySet());

        File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName().substring(0, file.getName().length() - ".yml".length());
                try {
                    uuids.add(UUID.fromString(name));
                } catch (IllegalArgumentException ignored) {
                    // Not a player data file; skip it.
                }
            }
        }

        Set<String> names = new TreeSet<>();
        for (UUID uuid : uuids) {
            PlayerLoadoutData data = getData(uuid);
            names.add(data.getCurrentLoadout());
            names.addAll(data.getLoadouts().keySet());
        }
        return names;
    }

    public void saveAll() {
        for (Map.Entry<UUID, PlayerLoadoutData> entry : cache.entrySet()) {
            save(entry.getKey(), entry.getValue());
        }
    }

    private PlayerLoadoutData load(UUID uuid) {
        File file = new File(playerDataFolder, uuid + ".yml");
        if (!file.exists()) {
            return PlayerLoadoutData.createDefault();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        // Files written before the profile -> loadout rename used "current-profile"/"profiles".
        boolean legacyFormat = !yaml.contains("current-loadout") && !yaml.contains("loadouts")
                && (yaml.contains("current-profile") || yaml.contains("profiles"));
        String currentKey = legacyFormat ? "current-profile" : "current-loadout";
        String loadoutsKey = legacyFormat ? "profiles" : "loadouts";

        String currentLoadout = yaml.getString(currentKey, PlayerLoadoutData.DEFAULT_LOADOUT);
        Map<String, InventorySnapshot> loadouts = new HashMap<>();

        ConfigurationSection loadoutsSection = yaml.getConfigurationSection(loadoutsKey);
        if (loadoutsSection != null) {
            for (String key : loadoutsSection.getKeys(false)) {
                ConfigurationSection loadoutSection = loadoutsSection.getConfigurationSection(key);
                if (loadoutSection != null) {
                    loadouts.put(key, InventorySnapshot.readFrom(loadoutSection));
                }
            }
        }

        PlayerLoadoutData data = new PlayerLoadoutData(currentLoadout, loadouts);
        if (legacyFormat) {
            // Rewrite immediately so the file only needs to be migrated once.
            save(uuid, data);
        }
        return data;
    }

    private void save(UUID uuid, PlayerLoadoutData data) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("current-loadout", data.getCurrentLoadout());

        ConfigurationSection loadoutsSection = yaml.createSection("loadouts");
        for (Map.Entry<String, InventorySnapshot> entry : data.getLoadouts().entrySet()) {
            ConfigurationSection loadoutSection = loadoutsSection.createSection(entry.getKey());
            entry.getValue().writeTo(loadoutSection);
        }

        File file = new File(playerDataFolder, uuid + ".yml");
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save inventory loadout data for " + uuid, e);
        }
    }
}
