package com.xton.inventoryswap.command;

import com.xton.inventoryswap.InventorySwapPlugin;
import com.xton.inventoryswap.profile.PlayerProfileData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvSwapCommandTest {

    private ServerMock server;
    private InventorySwapPlugin plugin;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(InventorySwapPlugin.class);
        player = server.addPlayer();
        player.setOp(true);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private String nextMessage() {
        Component component = player.nextComponentMessage();
        return component == null ? null : PlainTextComponentSerializer.plainText().serialize(component);
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    @Test
    void currentShowsDefaultProfileForNewPlayer() {
        server.dispatchCommand(player, "invswap current");

        assertEquals(player.getName() + "'s active profile: default", nextMessage());
    }

    @Test
    void listShowsDefaultProfileAsActive() {
        server.dispatchCommand(player, "invswap list");

        assertEquals("Profiles for " + player.getName() + ":", nextMessage());
        assertEquals(" - default (active)", nextMessage());
    }

    @Test
    void createAddsNewProfile() {
        server.dispatchCommand(player, "invswap create castle");

        assertEquals("Created empty profile 'castle' for " + player.getName() + ".", nextMessage());
        assertTrue(plugin.getProfileManager().getData(player).getProfiles().containsKey("castle"));
    }

    @Test
    void creatingDuplicateProfileFails() {
        server.dispatchCommand(player, "invswap create castle");
        nextMessage();

        server.dispatchCommand(player, "invswap create castle");

        assertEquals("'castle' already exists for " + player.getName() + ".", nextMessage());
    }

    @Test
    void switchSwapsActiveProfileAndCreatesItIfMissing() {
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));

        server.dispatchCommand(player, "invswap switch castle");

        assertEquals("Switched " + player.getName() + " to 'castle'.", nextMessage());
        assertEquals("castle", plugin.getProfileManager().getData(player).getCurrentProfile());
        assertTrue(isEmpty(player.getInventory().getItem(0)));
    }

    @Test
    void deletingActiveProfileFails() {
        server.dispatchCommand(player, "invswap delete default");

        assertEquals("Can't delete " + player.getName() + "'s active profile. Switch away from it first.", nextMessage());
    }

    @Test
    void deletingExistingProfileRemovesIt() {
        server.dispatchCommand(player, "invswap create castle");
        nextMessage();

        server.dispatchCommand(player, "invswap delete castle");

        assertEquals("Deleted profile 'castle' for " + player.getName() + ".", nextMessage());
        assertFalse(plugin.getProfileManager().getData(player).getProfiles().containsKey("castle"));
    }

    @Test
    void renamesStoredProfile() {
        server.dispatchCommand(player, "invswap create castle");
        nextMessage();

        server.dispatchCommand(player, "invswap rename castle fortress");

        assertEquals("Renamed profile 'castle' to 'fortress' for " + player.getName() + ".", nextMessage());
        PlayerProfileData data = plugin.getProfileManager().getData(player);
        assertFalse(data.getProfiles().containsKey("castle"));
        assertTrue(data.getProfiles().containsKey("fortress"));
    }

    @Test
    void renamingActiveProfileUpdatesCurrentProfile() {
        server.dispatchCommand(player, "invswap rename default home");

        assertEquals("Renamed profile 'default' to 'home' for " + player.getName() + ".", nextMessage());
        assertEquals("home", plugin.getProfileManager().getData(player).getCurrentProfile());
    }
}
