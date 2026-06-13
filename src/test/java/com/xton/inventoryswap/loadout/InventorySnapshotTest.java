package com.xton.inventoryswap.profile;

import org.bukkit.Material;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventorySnapshotTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void captureAndApplyRoundTrip() {
        PlayerMock source = server.addPlayer();
        source.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        source.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        source.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));

        InventorySnapshot snapshot = InventorySnapshot.capture(source);

        PlayerMock target = server.addPlayer();
        target.getInventory().setItem(0, new ItemStack(Material.COBBLESTONE, 64));
        snapshot.apply(target);

        assertEquals(new ItemStack(Material.DIAMOND, 5), target.getInventory().getItem(0));
        assertEquals(new ItemStack(Material.IRON_HELMET), target.getInventory().getHelmet());
        assertEquals(new ItemStack(Material.SHIELD), target.getInventory().getItemInOffHand());
    }

    @Test
    void emptySnapshotClearsInventory() {
        PlayerMock player = server.addPlayer();
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));

        InventorySnapshot.empty().apply(player);

        assertTrue(isEmpty(player.getInventory().getItem(0)));
        assertTrue(isEmpty(player.getInventory().getHelmet()));
        assertTrue(isEmpty(player.getInventory().getItemInOffHand()));
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    @Test
    void serializesToAndFromConfigurationSection() {
        PlayerMock source = server.addPlayer();
        source.getInventory().setItem(3, new ItemStack(Material.GOLDEN_PICKAXE));
        source.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));

        InventorySnapshot snapshot = InventorySnapshot.capture(source);

        MemoryConfiguration config = new MemoryConfiguration();
        snapshot.writeTo(config.createSection("profile"));

        InventorySnapshot restored = InventorySnapshot.readFrom(config.getConfigurationSection("profile"));

        PlayerMock target = server.addPlayer();
        restored.apply(target);

        assertEquals(new ItemStack(Material.GOLDEN_PICKAXE), target.getInventory().getItem(3));
        assertEquals(new ItemStack(Material.DIAMOND_BOOTS), target.getInventory().getBoots());
    }
}
