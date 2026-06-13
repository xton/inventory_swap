package com.xton.inventoryswap.loadout;

import com.xton.inventoryswap.InventorySwapPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadoutManagerTest {

    private ServerMock server;
    private InventorySwapPlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(InventorySwapPlugin.class);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void persistsLoadoutsAcrossUnloadAndReload() {
        LoadoutManager manager = plugin.getLoadoutManager();
        PlayerMock player = server.addPlayer();
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));

        PlayerLoadoutData data = manager.getData(player);
        data.setCurrentLoadout("castle");
        data.setLoadout("default", InventorySnapshot.capture(player));
        manager.save(player);
        manager.unload(player);

        PlayerLoadoutData reloaded = manager.getData(player);

        assertEquals("castle", reloaded.getCurrentLoadout());
        InventorySnapshot defaultSnapshot = reloaded.getLoadout("default");
        PlayerMock target = server.addPlayer("target");
        defaultSnapshot.apply(target);
        assertEquals(new ItemStack(Material.DIAMOND, 5), target.getInventory().getItem(0));
    }

    @Test
    void newPlayerStartsOnDefaultLoadoutWithNoSavedLoadouts() {
        LoadoutManager manager = plugin.getLoadoutManager();
        PlayerMock player = server.addPlayer();

        PlayerLoadoutData data = manager.getData(player);

        assertEquals(PlayerLoadoutData.DEFAULT_LOADOUT, data.getCurrentLoadout());
        assertEquals(0, data.getLoadouts().size());
    }

    @Test
    void migratesLegacyProfileFormatOnLoad() throws IOException {
        LoadoutManager manager = plugin.getLoadoutManager();
        PlayerMock player = server.addPlayer();

        File playerDataFolder = new File(plugin.getDataFolder(), "playerdata");
        playerDataFolder.mkdirs();
        File file = new File(playerDataFolder, player.getUniqueId() + ".yml");

        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("current-profile", "castle");
        ConfigurationSection profilesSection = legacy.createSection("profiles");
        InventorySnapshot.empty().writeTo(profilesSection.createSection("castle"));
        legacy.save(file);

        PlayerLoadoutData data = manager.getData(player);

        assertEquals("castle", data.getCurrentLoadout());
        assertTrue(data.getLoadouts().containsKey("castle"));

        YamlConfiguration migrated = YamlConfiguration.loadConfiguration(file);
        assertEquals("castle", migrated.getString("current-loadout"));
        assertTrue(migrated.contains("loadouts.castle"));
        assertFalse(migrated.contains("current-profile"));
        assertFalse(migrated.contains("profiles"));
    }
}
