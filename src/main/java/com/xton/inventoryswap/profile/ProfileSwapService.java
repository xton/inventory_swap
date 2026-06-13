package com.xton.inventoryswap.profile;

import org.bukkit.entity.Player;

/**
 * Performs the actual inventory swap for an online player, shared by the
 * sign/barrel listener and the admin commands.
 */
public class ProfileSwapService {

    public enum Result {
        SWITCHED,
        CREATED,
        ALREADY_ACTIVE
    }

    private final ProfileManager profileManager;

    public ProfileSwapService(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    public Result switchProfile(Player player, String profileName) {
        PlayerProfileData data = profileManager.getData(player);
        String currentProfile = data.getCurrentProfile();

        if (currentProfile.equalsIgnoreCase(profileName)) {
            return Result.ALREADY_ACTIVE;
        }

        // Stash what the player is currently carrying back into their current profile.
        data.setProfile(currentProfile, InventorySnapshot.capture(player));

        InventorySnapshot existing = data.getProfile(profileName);
        boolean created = existing == null;
        InventorySnapshot target = created ? InventorySnapshot.empty() : existing;
        target.apply(player);

        data.setCurrentProfile(profileName);
        profileManager.save(player);

        return created ? Result.CREATED : Result.SWITCHED;
    }
}
