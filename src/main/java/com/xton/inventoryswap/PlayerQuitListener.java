package com.xton.inventoryswap;

import com.xton.inventoryswap.loadout.LoadoutManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Flushes a player's loadout data to disk and drops it from the cache when they leave.
 */
public class PlayerQuitListener implements Listener {

    private final LoadoutManager loadoutManager;

    public PlayerQuitListener(LoadoutManager loadoutManager) {
        this.loadoutManager = loadoutManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        loadoutManager.unload(event.getPlayer());
    }
}
