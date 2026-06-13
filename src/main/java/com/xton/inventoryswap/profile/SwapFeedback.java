package com.xton.inventoryswap.profile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Shows the player an on-screen title and plays a sound whenever their inventory profile changes.
 */
public final class SwapFeedback {

    private static final Title.Times TIMES = Title.Times.times(
            Duration.ofMillis(150), Duration.ofMillis(1250), Duration.ofMillis(500));

    private SwapFeedback() {
    }

    public static void showSwitched(Player player, String profileName, boolean created) {
        Component main = Component.text(displayName(profileName), NamedTextColor.AQUA, TextDecoration.BOLD);
        Component sub = Component.text(created ? "New inventory created" : "Inventory loaded", NamedTextColor.GRAY);

        player.showTitle(Title.title(main, sub, TIMES));
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1f, created ? 1.4f : 1f);
    }

    public static void showAlreadyActive(Player player, String profileName) {
        player.sendActionBar(Component.text("Already using '" + displayName(profileName) + "'", NamedTextColor.YELLOW));
    }

    private static String displayName(String profileName) {
        if (profileName.isEmpty()) {
            return profileName;
        }
        return profileName.substring(0, 1).toUpperCase() + profileName.substring(1);
    }
}
