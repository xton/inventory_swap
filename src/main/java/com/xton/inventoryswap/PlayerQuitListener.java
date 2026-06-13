package com.xton.inventoryswap;

import com.xton.inventoryswap.profile.ProfileManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Flushes a player's profile data to disk and drops it from the cache when they leave.
 */
public class PlayerQuitListener implements Listener {

    private final ProfileManager profileManager;

    public PlayerQuitListener(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        profileManager.unload(event.getPlayer());
    }
}
