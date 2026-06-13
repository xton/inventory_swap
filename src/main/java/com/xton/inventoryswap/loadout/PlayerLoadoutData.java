package com.xton.inventoryswap.loadout;

import java.util.HashMap;
import java.util.Map;

/**
 * All of a single player's inventory loadouts, plus which one is currently active.
 */
public class PlayerLoadoutData {

    public static final String DEFAULT_LOADOUT = "default";

    private String currentLoadout;
    private final Map<String, InventorySnapshot> loadouts;

    public PlayerLoadoutData(String currentLoadout, Map<String, InventorySnapshot> loadouts) {
        this.currentLoadout = currentLoadout;
        this.loadouts = loadouts;
    }

    public static PlayerLoadoutData createDefault() {
        return new PlayerLoadoutData(DEFAULT_LOADOUT, new HashMap<>());
    }

    public String getCurrentLoadout() {
        return currentLoadout;
    }

    public void setCurrentLoadout(String loadout) {
        this.currentLoadout = loadout;
    }

    public InventorySnapshot getLoadout(String name) {
        return loadouts.get(name);
    }

    public void setLoadout(String name, InventorySnapshot snapshot) {
        loadouts.put(name, snapshot);
    }

    public Map<String, InventorySnapshot> getLoadouts() {
        return loadouts;
    }
}
