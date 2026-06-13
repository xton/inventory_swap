package com.xton.inventoryswap;

import com.xton.inventoryswap.command.InvSwapCommand;
import com.xton.inventoryswap.profile.ProfileManager;
import com.xton.inventoryswap.profile.ProfileSwapService;
import org.bukkit.plugin.java.JavaPlugin;

public class InventorySwapPlugin extends JavaPlugin {

    private ProfileManager profileManager;

    @Override
    public void onEnable() {
        profileManager = new ProfileManager(this);
        ProfileSwapService swapService = new ProfileSwapService(profileManager);

        getServer().getPluginManager().registerEvents(new SwapTriggerListener(swapService), this);
        getServer().getPluginManager().registerEvents(new SignStyleListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(profileManager), this);

        InvSwapCommand invSwapCommand = new InvSwapCommand(profileManager, swapService);
        var inv = getCommand("inv");
        if (inv != null) {
            inv.setExecutor(invSwapCommand);
            inv.setTabCompleter(invSwapCommand);
        }
    }

    @Override
    public void onDisable() {
        if (profileManager != null) {
            profileManager.saveAll();
        }
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }
}
