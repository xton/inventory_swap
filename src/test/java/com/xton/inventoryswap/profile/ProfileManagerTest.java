package com.xton.inventoryswap.profile;

import com.xton.inventoryswap.InventorySwapPlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileManagerTest {

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
    void persistsProfilesAcrossUnloadAndReload() {
        ProfileManager manager = plugin.getProfileManager();
        PlayerMock player = server.addPlayer();
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));

        PlayerProfileData data = manager.getData(player);
        data.setCurrentProfile("castle");
        data.setProfile("default", InventorySnapshot.capture(player));
        manager.save(player);
        manager.unload(player);

        PlayerProfileData reloaded = manager.getData(player);

        assertEquals("castle", reloaded.getCurrentProfile());
        InventorySnapshot defaultSnapshot = reloaded.getProfile("default");
        PlayerMock target = server.addPlayer("target");
        defaultSnapshot.apply(target);
        assertEquals(new ItemStack(Material.DIAMOND, 5), target.getInventory().getItem(0));
    }

    @Test
    void newPlayerStartsOnDefaultProfileWithNoSavedProfiles() {
        ProfileManager manager = plugin.getProfileManager();
        PlayerMock player = server.addPlayer();

        PlayerProfileData data = manager.getData(player);

        assertEquals(PlayerProfileData.DEFAULT_PROFILE, data.getCurrentProfile());
        assertEquals(0, data.getProfiles().size());
    }
}
