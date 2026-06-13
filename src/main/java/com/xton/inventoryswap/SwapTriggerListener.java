package com.xton.inventoryswap;

import com.xton.inventoryswap.profile.ProfileSwapService;
import com.xton.inventoryswap.profile.SwapFeedback;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

/**
 * Listens for right-clicks on signs whose first line is "[INV]", or on a container
 * (barrel, chest, etc.) that has such a sign mounted on top of or beside it, and
 * swaps the clicking player's inventory to the profile named on the second line.
 */
public class SwapTriggerListener implements Listener {

    private static final String TAG_LINE = "[inv]";
    private static final BlockFace[] HORIZONTAL_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private final ProfileSwapService swapService;

    public SwapTriggerListener(ProfileSwapService swapService) {
        this.swapService = swapService;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        BlockState state = clickedBlock.getState();
        Sign sign;
        if (state instanceof Sign clickedSign) {
            sign = clickedSign;
        } else if (state instanceof Container) {
            sign = findAttachedSign(clickedBlock);
        } else {
            return;
        }

        if (sign == null) {
            return;
        }

        String profileName = readProfileName(sign);
        if (profileName == null) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("inventoryswap.use")) {
            return;
        }

        event.setCancelled(true);
        restyleSign(sign);

        ProfileSwapService.Result result = swapService.switchProfile(player, profileName);
        switch (result) {
            case SWITCHED -> SwapFeedback.showSwitched(player, profileName, false);
            case CREATED -> SwapFeedback.showSwitched(player, profileName, true);
            case ALREADY_ACTIVE -> SwapFeedback.showAlreadyActive(player, profileName);
        }
    }

    /**
     * Looks for an "[INV]" sign mounted on top of, or attached to a side of, the given container block.
     */
    private Sign findAttachedSign(Block container) {
        BlockState above = container.getRelative(BlockFace.UP).getState();
        if (above instanceof Sign sign && readProfileName(sign) != null) {
            return sign;
        }

        for (BlockFace face : HORIZONTAL_FACES) {
            BlockState neighbor = container.getRelative(face).getState();
            if (neighbor instanceof Sign sign
                    && sign.getBlockData() instanceof WallSign wallSign
                    && wallSign.getFacing() == face
                    && readProfileName(sign) != null) {
                return sign;
            }
        }

        return null;
    }

    /**
     * Brings an "[INV]" sign's styling up to date with the current look, in case it was
     * written before the styling changed or by a plugin version that didn't style it at all.
     */
    private void restyleSign(Sign sign) {
        SignSide front = sign.getSide(Side.FRONT);
        List<Component> lines = front.lines();
        boolean changed = false;

        Component styledTag = SignStyle.styledTag();
        if (!styledTag.equals(lines.get(0))) {
            front.line(0, styledTag);
            changed = true;
        }

        if (lines.size() > 1) {
            String profileText = PlainTextComponentSerializer.plainText().serialize(lines.get(1)).trim();
            if (!profileText.isEmpty()) {
                Component styledProfile = SignStyle.styledProfileName(profileText);
                if (!styledProfile.equals(lines.get(1))) {
                    front.line(1, styledProfile);
                    changed = true;
                }
            }
        }

        if (changed) {
            sign.update();
        }
    }

    /**
     * Returns the profile name from an "[INV]" sign's front side, or null if the sign isn't tagged.
     */
    private String readProfileName(Sign sign) {
        List<Component> lines = sign.getSide(Side.FRONT).lines();
        if (lines.isEmpty()) {
            return null;
        }

        String tag = PlainTextComponentSerializer.plainText().serialize(lines.get(0)).trim();
        if (!tag.equalsIgnoreCase(TAG_LINE)) {
            return null;
        }

        String profileName = lines.size() > 1
                ? PlainTextComponentSerializer.plainText().serialize(lines.get(1)).trim()
                : "";
        return profileName.isEmpty() ? null : profileName.toLowerCase();
    }
}
