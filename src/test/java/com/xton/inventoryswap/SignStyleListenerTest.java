package com.xton.inventoryswap;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.sign.Side;
import org.bukkit.event.block.SignChangeEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignStyleListenerTest {

    private ServerMock server;
    private World world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(InventorySwapPlugin.class);
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private SignChangeEvent change(String... lines) {
        Block block = world.getBlockAt(0, 64, 0);
        block.setType(Material.OAK_SIGN);

        List<Component> components = Arrays.asList(
                Component.text(lines[0]),
                Component.text(lines[1]),
                Component.text(lines[2]),
                Component.text(lines[3]));

        SignChangeEvent event = new SignChangeEvent(block, player, components, Side.FRONT);
        server.getPluginManager().callEvent(event);
        return event;
    }

    private String plainText(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void recolorsTaggedSignAndLoadoutName() {
        SignChangeEvent event = change("[inv]", "castle", "", "");

        Component tagLine = event.line(0);
        assertEquals("[inv]", plainText(tagLine));
        assertEquals(NamedTextColor.GRAY, tagLine.color());
        assertTrue(tagLine.hasDecoration(TextDecoration.ITALIC));

        Component loadoutLine = event.line(1);
        assertEquals("castle", plainText(loadoutLine));
        assertEquals(NamedTextColor.DARK_GREEN, loadoutLine.color());
        assertTrue(loadoutLine.hasDecoration(TextDecoration.BOLD));
    }

    @Test
    void leavesLoadoutLineAloneWhenEmpty() {
        SignChangeEvent event = change("[INV]", "", "", "");

        assertEquals("[inv]", plainText(event.line(0)));
        assertEquals(NamedTextColor.GRAY, event.line(0).color());
        assertEquals("", plainText(event.line(1)));
    }

    @Test
    void ignoresSignsWithoutInvTag() {
        SignChangeEvent event = change("Welcome!", "to the castle", "", "");

        assertEquals("Welcome!", plainText(event.line(0)));
        assertFalse(NamedTextColor.GRAY.equals(event.line(0).color()));
        assertEquals("to the castle", plainText(event.line(1)));
        assertFalse(NamedTextColor.DARK_GREEN.equals(event.line(1).color()));
    }
}
