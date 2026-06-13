package com.xton.inventoryswap.profile;

import java.util.HashMap;
import java.util.Map;

/**
 * All of a single player's inventory profiles, plus which one is currently active.
 */
public class PlayerProfileData {

    public static final String DEFAULT_PROFILE = "default";

    private String currentProfile;
    private final Map<String, InventorySnapshot> profiles;

    public PlayerProfileData(String currentProfile, Map<String, InventorySnapshot> profiles) {
        this.currentProfile = currentProfile;
        this.profiles = profiles;
    }

    public static PlayerProfileData createDefault() {
        return new PlayerProfileData(DEFAULT_PROFILE, new HashMap<>());
    }

    public String getCurrentProfile() {
        return currentProfile;
    }

    public void setCurrentProfile(String profile) {
        this.currentProfile = profile;
    }

    public InventorySnapshot getProfile(String name) {
        return profiles.get(name);
    }

    public void setProfile(String name, InventorySnapshot snapshot) {
        profiles.put(name, snapshot);
    }

    public Map<String, InventorySnapshot> getProfiles() {
        return profiles;
    }
}
