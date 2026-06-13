package com.xton.inventoryswap.loadout;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Shows the player an on-screen title and plays a sound whenever their inventory loadout changes.
 */
public final class SwapFeedback {

    private static final Title.Times TIMES = Title.Times.times(
            Duration.ofMillis(150), Duration.ofMillis(1250), Duration.ofMillis(500));

    private SwapFeedback() {
    }

    public static void showSwitched(Player player, String loadoutName, boolean created) {
        Component main = Component.text(displayName(loadoutName), NamedTextColor.AQUA, TextDecoration.BOLD);
        Component sub = Component.text(created ? "New inventory created" : "Inventory loaded", NamedTextColor.GRAY);

        player.showTitle(Title.title(main, sub, TIMES));
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1f, created ? 1.4f : 1f);
    }

    public static void showAlreadyActive(Player player, String loadoutName) {
        player.sendActionBar(Component.text("Already using '" + displayName(loadoutName) + "'", NamedTextColor.YELLOW));
    }

    private static String displayName(String loadoutName) {
        if (loadoutName.isEmpty()) {
            return loadoutName;
        }
        return loadoutName.substring(0, 1).toUpperCase() + loadoutName.substring(1);
    }
}
