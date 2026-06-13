package com.xton.inventoryswap.profile;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Loads and saves each player's inventory profiles to a YAML file in the plugin's data folder.
 */
public class ProfileManager {

    private final JavaPlugin plugin;
    private final File playerDataFolder;
    private final Map<UUID, PlayerProfileData> cache = new HashMap<>();

    public ProfileManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }
    }

    public PlayerProfileData getData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public PlayerProfileData getData(Player player) {
        return getData(player.getUniqueId());
    }

    public void save(UUID uuid) {
        PlayerProfileData data = cache.get(uuid);
        if (data != null) {
            save(uuid, data);
        }
    }

    public void save(Player player) {
        save(player.getUniqueId());
    }

    public void unload(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerProfileData data = cache.remove(uuid);
        if (data != null) {
            save(uuid, data);
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, PlayerProfileData> entry : cache.entrySet()) {
            save(entry.getKey(), entry.getValue());
        }
    }

    private PlayerProfileData load(UUID uuid) {
        File file = new File(playerDataFolder, uuid + ".yml");
        if (!file.exists()) {
            return PlayerProfileData.createDefault();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String currentProfile = yaml.getString("current-profile", PlayerProfileData.DEFAULT_PROFILE);
        Map<String, InventorySnapshot> profiles = new HashMap<>();

        ConfigurationSection profilesSection = yaml.getConfigurationSection("profiles");
        if (profilesSection != null) {
            for (String key : profilesSection.getKeys(false)) {
                ConfigurationSection profileSection = profilesSection.getConfigurationSection(key);
                if (profileSection != null) {
                    profiles.put(key, InventorySnapshot.readFrom(profileSection));
                }
            }
        }

        return new PlayerProfileData(currentProfile, profiles);
    }

    private void save(UUID uuid, PlayerProfileData data) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("current-profile", data.getCurrentProfile());

        ConfigurationSection profilesSection = yaml.createSection("profiles");
        for (Map.Entry<String, InventorySnapshot> entry : data.getProfiles().entrySet()) {
            ConfigurationSection profileSection = profilesSection.createSection(entry.getKey());
            entry.getValue().writeTo(profileSection);
        }

        File file = new File(playerDataFolder, uuid + ".yml");
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save inventory profile data for " + uuid, e);
        }
    }
}
