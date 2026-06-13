package com.xton.inventoryswap;

import com.xton.inventoryswap.profile.PlayerProfileData;
import com.xton.inventoryswap.profile.ProfileManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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

class SwapTriggerListenerTest {

    private ServerMock server;
    private InventorySwapPlugin plugin;
    private World world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(InventorySwapPlugin.class);
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Sign placeSign(Block block, String tagLine, String profileLine) {
        block.setType(Material.OAK_SIGN);
        return setSignText(block, tagLine, profileLine);
    }

    private Sign setSignText(Block block, String tagLine, String profileLine) {
        Sign sign = (Sign) block.getState();
        sign.getSide(Side.FRONT).setLine(0, tagLine);
        sign.getSide(Side.FRONT).setLine(1, profileLine);
        sign.update(true);
        return (Sign) block.getState();
    }

    private boolean rightClick(Block block) {
        PlayerInteractEvent event = new PlayerInteractEvent(
                player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.UP, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(event);
        return event.isCancelled();
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    @Test
    void rightClickingTaggedSignSwapsInventoryAndStoresPreviousContents() {
        placeSign(world.getBlockAt(0, 64, 0), "[INV]", "castle");

        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));

        boolean cancelled = rightClick(world.getBlockAt(0, 64, 0));

        assertTrue(cancelled);
        assertTrue(isEmpty(player.getInventory().getItem(0)), "new profile should start empty");

        ProfileManager manager = plugin.getProfileManager();
        PlayerProfileData data = manager.getData(player);
        assertEquals("castle", data.getCurrentProfile());

        // Switching back to default should restore the diamonds.
        placeSign(world.getBlockAt(0, 64, 1), "[INV]", "default");
        rightClick(world.getBlockAt(0, 64, 1));

        assertEquals("default", manager.getData(player).getCurrentProfile());
        assertEquals(new ItemStack(Material.DIAMOND, 5), player.getInventory().getItem(0));
    }

    @Test
    void rightClickingSignForActiveProfileIsANoOp() {
        placeSign(world.getBlockAt(0, 64, 0), "[INV]", "default");
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));

        boolean cancelled = rightClick(world.getBlockAt(0, 64, 0));

        assertTrue(cancelled);
        assertEquals(new ItemStack(Material.DIAMOND, 5), player.getInventory().getItem(0));
        assertEquals("Already using 'Default'", plainText(player.nextActionBar()));
    }

    @Test
    void ignoresSignsWithoutInvTag() {
        placeSign(world.getBlockAt(0, 64, 0), "Welcome!", "to the castle");
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND, 5));

        boolean cancelled = rightClick(world.getBlockAt(0, 64, 0));

        assertFalse(cancelled);
        assertEquals(new ItemStack(Material.DIAMOND, 5), player.getInventory().getItem(0));
    }

    @Test
    void rightClickingBarrelWithSignOnTopSwapsProfile() {
        Block barrel = world.getBlockAt(5, 64, 5);
        barrel.setType(Material.BARREL);
        placeSign(barrel.getRelative(BlockFace.UP), "[INV]", "treehouse");

        player.getInventory().setItem(0, new ItemStack(Material.OAK_LOG, 32));

        boolean cancelled = rightClick(barrel);

        assertTrue(cancelled);
        assertTrue(isEmpty(player.getInventory().getItem(0)));
        assertEquals("treehouse", plugin.getProfileManager().getData(player).getCurrentProfile());
        player.assertSoundHeard(Sound.BLOCK_ENDER_CHEST_CLOSE);
    }

    @Test
    void rightClickingBarrelWithWallSignAttachedSwapsProfile() {
        Block barrel = world.getBlockAt(10, 64, 10);
        barrel.setType(Material.BARREL);

        Block wallSignBlock = barrel.getRelative(BlockFace.NORTH);
        wallSignBlock.setType(Material.OAK_WALL_SIGN);
        WallSign wallSignData = (WallSign) wallSignBlock.getBlockData();
        wallSignData.setFacing(BlockFace.NORTH);
        wallSignBlock.setBlockData(wallSignData);
        setSignText(wallSignBlock, "[INV]", "mine");

        boolean cancelled = rightClick(barrel);

        assertTrue(cancelled);
        assertEquals("mine", plugin.getProfileManager().getData(player).getCurrentProfile());
    }

    @Test
    void rightClickingSignRestylesItToCurrentLook() {
        Block block = world.getBlockAt(0, 64, 0);
        placeSign(block, "[INV]", "castle");

        rightClick(block);

        Sign sign = (Sign) block.getState();
        assertEquals(SignStyle.styledTag(), sign.getSide(Side.FRONT).line(0));
        assertEquals(SignStyle.styledProfileName("castle"), sign.getSide(Side.FRONT).line(1));
    }

    @Test
    void rightClickingPlainBarrelDoesNothing() {
        Block barrel = world.getBlockAt(20, 64, 20);
        barrel.setType(Material.BARREL);

        boolean cancelled = rightClick(barrel);

        assertFalse(cancelled);
        assertEquals("default", plugin.getProfileManager().getData(player).getCurrentProfile());
    }

    private String plainText(net.kyori.adventure.text.Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }
}
