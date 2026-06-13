package com.xton.inventoryswap.command;

import com.xton.inventoryswap.InventorySwapPlugin;
import com.xton.inventoryswap.loadout.InventorySnapshot;
import com.xton.inventoryswap.loadout.PlayerLoadoutData;
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
    void currentShowsDefaultLoadoutForNewPlayer() {
        server.dispatchCommand(player, "inv current");

        assertEquals(player.getName() + "'s active loadout: default", nextMessage());
    }

    @Test
    void listShowsDefaultLoadoutAsActive() {
        server.dispatchCommand(player, "inv list");

        assertEquals("Loadouts for " + player.getName() + ":", nextMessage());
        assertEquals(" - default (active)", nextMessage());
    }

    @Test
    void createAddsNewLoadout() {
        server.dispatchCommand(player, "inv create castle");

        assertEquals("Created empty loadout 'castle' for " + player.getName() + ".", nextMessage());
        assertTrue(plugin.getLoadoutManager().getData(player).getLoadouts().containsKey("castle"));
    }

    @Test
    void creatingDuplicateLoadoutFails() {
        server.dispatchCommand(player, "inv create castle");
        nextMessage();

        server.dispatchCommand(player, "inv create castle");

        assertEquals("'castle' already exists for " + player.getName() + ".", nextMessage());
    }

    @Test
    void swapSwapsActiveLoadoutAndCreatesItIfMissing() {
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));

        server.dispatchCommand(player, "inv swap castle");

        assertEquals("Swapped " + player.getName() + " to 'castle'.", nextMessage());
        assertEquals("castle", plugin.getLoadoutManager().getData(player).getCurrentLoadout());
        assertTrue(isEmpty(player.getInventory().getItem(0)));
    }

    @Test
    void deletingActiveLoadoutFails() {
        server.dispatchCommand(player, "inv delete default");

        assertEquals("Can't delete " + player.getName() + "'s active loadout. Switch away from it first.", nextMessage());
    }

    @Test
    void deletingExistingLoadoutRemovesIt() {
        server.dispatchCommand(player, "inv create castle");
        nextMessage();

        server.dispatchCommand(player, "inv delete castle");

        assertEquals("Deleted loadout 'castle' for " + player.getName() + ".", nextMessage());
        assertFalse(plugin.getLoadoutManager().getData(player).getLoadouts().containsKey("castle"));
    }

    @Test
    void renamesStoredLoadout() {
        server.dispatchCommand(player, "inv create castle");
        nextMessage();

        server.dispatchCommand(player, "inv rename castle fortress");

        assertEquals("Renamed loadout 'castle' to 'fortress' for " + player.getName() + ".", nextMessage());
        PlayerLoadoutData data = plugin.getLoadoutManager().getData(player);
        assertFalse(data.getLoadouts().containsKey("castle"));
        assertTrue(data.getLoadouts().containsKey("fortress"));
    }

    @Test
    void renamingActiveLoadoutUpdatesCurrentLoadout() {
        server.dispatchCommand(player, "inv rename default home");

        assertEquals("Renamed loadout 'default' to 'home' for " + player.getName() + ".", nextMessage());
        assertEquals("home", plugin.getLoadoutManager().getData(player).getCurrentLoadout());
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
    void tabCompletesOwnLoadoutNames() {
        server.dispatchCommand(player, "inv create castle");
        nextMessage();

        assertEquals(List.of("castle", "default"), tabComplete("swap", ""));
        assertEquals(List.of("castle"), tabComplete("swap", "c"));
    }

    @Test
    void tabCompletesLoadoutNamesCreatedByOtherPlayers() {
        PlayerMock other = server.addPlayer("Other");
        PlayerLoadoutData otherData = plugin.getLoadoutManager().getData(other);
        otherData.setLoadout("treehouse", InventorySnapshot.empty());
        plugin.getLoadoutManager().save(other);

        assertEquals(List.of("default", "treehouse"), tabComplete("swap", ""));
    }
}
