package com.xton.inventoryswap.command;

import com.xton.inventoryswap.InventorySwapPlugin;
import com.xton.inventoryswap.profile.InventorySnapshot;
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

import java.util.List;

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
        server.dispatchCommand(player, "inv current");

        assertEquals(player.getName() + "'s active profile: default", nextMessage());
    }

    @Test
    void listShowsDefaultProfileAsActive() {
        server.dispatchCommand(player, "inv list");

        assertEquals("Profiles for " + player.getName() + ":", nextMessage());
        assertEquals(" - default (active)", nextMessage());
    }

    @Test
    void createAddsNewProfile() {
        server.dispatchCommand(player, "inv create castle");

        assertEquals("Created empty profile 'castle' for " + player.getName() + ".", nextMessage());
        assertTrue(plugin.getProfileManager().getData(player).getProfiles().containsKey("castle"));
    }

    @Test
    void creatingDuplicateProfileFails() {
        server.dispatchCommand(player, "inv create castle");
        nextMessage();

        server.dispatchCommand(player, "inv create castle");

        assertEquals("'castle' already exists for " + player.getName() + ".", nextMessage());
    }

    @Test
    void swapSwapsActiveProfileAndCreatesItIfMissing() {
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));

        server.dispatchCommand(player, "inv swap castle");

        assertEquals("Swapped " + player.getName() + " to 'castle'.", nextMessage());
        assertEquals("castle", plugin.getProfileManager().getData(player).getCurrentProfile());
        assertTrue(isEmpty(player.getInventory().getItem(0)));
    }

    @Test
    void deletingActiveProfileFails() {
        server.dispatchCommand(player, "inv delete default");

        assertEquals("Can't delete " + player.getName() + "'s active profile. Switch away from it first.", nextMessage());
    }

    @Test
    void deletingExistingProfileRemovesIt() {
        server.dispatchCommand(player, "inv create castle");
        nextMessage();

        server.dispatchCommand(player, "inv delete castle");

        assertEquals("Deleted profile 'castle' for " + player.getName() + ".", nextMessage());
        assertFalse(plugin.getProfileManager().getData(player).getProfiles().containsKey("castle"));
    }

    @Test
    void renamesStoredProfile() {
        server.dispatchCommand(player, "inv create castle");
        nextMessage();

        server.dispatchCommand(player, "inv rename castle fortress");

        assertEquals("Renamed profile 'castle' to 'fortress' for " + player.getName() + ".", nextMessage());
        PlayerProfileData data = plugin.getProfileManager().getData(player);
        assertFalse(data.getProfiles().containsKey("castle"));
        assertTrue(data.getProfiles().containsKey("fortress"));
    }

    @Test
    void renamingActiveProfileUpdatesCurrentProfile() {
        server.dispatchCommand(player, "inv rename default home");

        assertEquals("Renamed profile 'default' to 'home' for " + player.getName() + ".", nextMessage());
        assertEquals("home", plugin.getProfileManager().getData(player).getCurrentProfile());
    }

    private List<String> tabComplete(String... args) {
        InvSwapCommand command = (InvSwapCommand) plugin.getCommand("inv").getTabCompleter();
        return command.onTabComplete(player, plugin.getCommand("inv"), "inv", args);
    }

    @Test
    void tabCompletesSubcommandNames() {
        assertEquals(List.of("swap"), tabComplete("s"));
    }

    @Test
    void tabCompletesOwnProfileNames() {
        server.dispatchCommand(player, "inv create castle");
        nextMessage();

        assertEquals(List.of("castle", "default"), tabComplete("swap", ""));
        assertEquals(List.of("castle"), tabComplete("swap", "c"));
    }

    @Test
    void tabCompletesProfileNamesCreatedByOtherPlayers() {
        PlayerMock other = server.addPlayer("Other");
        PlayerProfileData otherData = plugin.getProfileManager().getData(other);
        otherData.setProfile("treehouse", InventorySnapshot.empty());
        plugin.getProfileManager().save(other);

        assertEquals(List.of("default", "treehouse"), tabComplete("swap", ""));
    }
}
