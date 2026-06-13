package com.xton.inventoryswap.loadout;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * A point-in-time copy of a player's main inventory, armor and offhand item.
 */
public class InventorySnapshot {

    private final List<ItemStack> storageContents;
    private final List<ItemStack> armorContents;
    private final ItemStack offHand;

    private InventorySnapshot(List<ItemStack> storageContents, List<ItemStack> armorContents, ItemStack offHand) {
        this.storageContents = storageContents;
        this.armorContents = armorContents;
        this.offHand = offHand;
    }

    public static InventorySnapshot empty() {
        return new InventorySnapshot(emptyItems(36), emptyItems(4), new ItemStack(Material.AIR));
    }

    public static InventorySnapshot capture(Player player) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> storage = normalize(inventory.getStorageContents());
        List<ItemStack> armor = normalize(inventory.getArmorContents());
        ItemStack offHand = inventory.getItemInOffHand().clone();
        return new InventorySnapshot(storage, armor, offHand);
    }

    public void apply(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.setStorageContents(storageContents.toArray(new ItemStack[0]));
        inventory.setArmorContents(armorContents.toArray(new ItemStack[0]));
        inventory.setItemInOffHand(offHand);
    }

    public void writeTo(ConfigurationSection section) {
        section.set("inventory", storageContents);
        section.set("armor", armorContents);
        section.set("offhand", offHand);
    }

    @SuppressWarnings("unchecked")
    public static InventorySnapshot readFrom(ConfigurationSection section) {
        List<ItemStack> storage = (List<ItemStack>) (List<?>) section.getList("inventory", emptyItems(36));
        List<ItemStack> armor = (List<ItemStack>) (List<?>) section.getList("armor", emptyItems(4));
        ItemStack offHand = section.getItemStack("offhand", new ItemStack(Material.AIR));
        return new InventorySnapshot(normalize(storage), normalize(armor), offHand);
    }

    private static List<ItemStack> normalize(ItemStack[] items) {
        List<ItemStack> list = new ArrayList<>(items.length);
        for (ItemStack item : items) {
            list.add(item == null ? new ItemStack(Material.AIR) : item.clone());
        }
        return list;
    }

    private static List<ItemStack> normalize(List<ItemStack> items) {
        List<ItemStack> list = new ArrayList<>(items.size());
        for (ItemStack item : items) {
            list.add(item == null ? new ItemStack(Material.AIR) : item);
        }
        return list;
    }

    private static List<ItemStack> emptyItems(int size) {
        List<ItemStack> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new ItemStack(Material.AIR));
        }
        return list;
    }
}
