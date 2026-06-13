package com.xton.inventoryswap.loadout;

import org.bukkit.entity.Player;

/**
 * Performs the actual inventory swap for an online player, shared by the
 * sign/barrel listener and the admin commands.
 */
public class LoadoutSwapService {

    public enum Result {
        SWITCHED,
        CREATED,
        ALREADY_ACTIVE
    }

    private final LoadoutManager loadoutManager;

    public LoadoutSwapService(LoadoutManager loadoutManager) {
        this.loadoutManager = loadoutManager;
    }

    public Result switchLoadout(Player player, String loadoutName) {
        PlayerLoadoutData data = loadoutManager.getData(player);
        String currentLoadout = data.getCurrentLoadout();

        if (currentLoadout.equalsIgnoreCase(loadoutName)) {
            return Result.ALREADY_ACTIVE;
        }

        // Stash what the player is currently carrying back into their current loadout.
        data.setLoadout(currentLoadout, InventorySnapshot.capture(player));

        InventorySnapshot existing = data.getLoadout(loadoutName);
        boolean created = existing == null;
        InventorySnapshot target = created ? InventorySnapshot.empty() : existing;
        target.apply(player);

        data.setCurrentLoadout(loadoutName);
        loadoutManager.save(player);

        return created ? Result.CREATED : Result.SWITCHED;
    }
}
