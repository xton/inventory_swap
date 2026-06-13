package com.xton.inventoryswap;

import com.xton.inventoryswap.command.InvSwapCommand;
import com.xton.inventoryswap.loadout.LoadoutManager;
import com.xton.inventoryswap.loadout.LoadoutSwapService;
import org.bukkit.plugin.java.JavaPlugin;

public class InventorySwapPlugin extends JavaPlugin {

    private LoadoutManager loadoutManager;

    @Override
    public void onEnable() {
        loadoutManager = new LoadoutManager(this);
        LoadoutSwapService swapService = new LoadoutSwapService(loadoutManager);

        getServer().getPluginManager().registerEvents(new SwapTriggerListener(swapService), this);
        getServer().getPluginManager().registerEvents(new SignStyleListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(loadoutManager), this);

        InvSwapCommand invSwapCommand = new InvSwapCommand(loadoutManager, swapService);
        var inv = getCommand("inv");
        if (inv != null) {
            inv.setExecutor(invSwapCommand);
            inv.setTabCompleter(invSwapCommand);
        }
    }

    @Override
    public void onDisable() {
        if (loadoutManager != null) {
            loadoutManager.saveAll();
        }
    }

    public LoadoutManager getLoadoutManager() {
        return loadoutManager;
    }
}
