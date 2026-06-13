package com.xton.inventoryswap.command;

import com.xton.inventoryswap.loadout.InventorySnapshot;
import com.xton.inventoryswap.loadout.LoadoutManager;
import com.xton.inventoryswap.loadout.LoadoutSwapService;
import com.xton.inventoryswap.loadout.PlayerLoadoutData;
import com.xton.inventoryswap.loadout.SwapFeedback;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Admin command for inspecting and managing players' inventory loadouts.
 */
public class InvSwapCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "list", "current", "swap", "create", "delete", "rename");

    private final LoadoutManager loadoutManager;
    private final LoadoutSwapService swapService;

    public InvSwapCommand(LoadoutManager loadoutManager, LoadoutSwapService swapService) {
        this.loadoutManager = loadoutManager;
        this.swapService = swapService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> handleList(sender, args);
            case "current" -> handleCurrent(sender, args);
            case "swap" -> handleSwap(sender, args);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "rename" -> handleRename(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleList(CommandSender sender, String[] args) {
        OfflinePlayer target = resolveTarget(sender, args, 1);
        if (target == null) {
            return;
        }

        PlayerLoadoutData data = loadoutManager.getData(target.getUniqueId());
        String current = data.getCurrentLoadout();

        sender.sendMessage(Component.text("Loadouts for " + target.getName() + ":", NamedTextColor.GOLD));
        sender.sendMessage(formatLoadoutLine(current, current));
        data.getLoadouts().keySet().stream()
                .filter(name -> !name.equalsIgnoreCase(current))
                .sorted()
                .forEach(name -> sender.sendMessage(formatLoadoutLine(name, current)));
    }

    private Component formatLoadoutLine(String name, String current) {
        boolean active = name.equalsIgnoreCase(current);
        Component text = Component.text(" - " + name, active ? NamedTextColor.GREEN : NamedTextColor.GRAY);
        if (active) {
            text = text.append(Component.text(" (active)", NamedTextColor.GREEN));
        }
        return text;
    }

    private void handleCurrent(CommandSender sender, String[] args) {
        OfflinePlayer target = resolveTarget(sender, args, 1);
        if (target == null) {
            return;
        }

        PlayerLoadoutData data = loadoutManager.getData(target.getUniqueId());
        sender.sendMessage(Component.text(
                target.getName() + "'s active loadout: " + data.getCurrentLoadout(), NamedTextColor.GOLD));
    }

    private void handleSwap(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /inv swap <loadout> [player]", NamedTextColor.RED));
            return;
        }

        String loadoutName = args[1].toLowerCase(Locale.ROOT);
        OfflinePlayer target = resolveTarget(sender, args, 2);
        if (target == null) {
            return;
        }

        Player online = target.getPlayer();
        if (online == null) {
            sender.sendMessage(Component.text(
                    target.getName() + " must be online to swap their active inventory.", NamedTextColor.RED));
            return;
        }

        LoadoutSwapService.Result result = swapService.switchLoadout(online, loadoutName);
        switch (result) {
            case SWITCHED -> SwapFeedback.showSwitched(online, loadoutName, false);
            case CREATED -> SwapFeedback.showSwitched(online, loadoutName, true);
            case ALREADY_ACTIVE -> SwapFeedback.showAlreadyActive(online, loadoutName);
        }
        sender.sendMessage(Component.text("Swapped " + online.getName() + " to '" + loadoutName + "'.", NamedTextColor.GREEN));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /inv create <loadout> [player]", NamedTextColor.RED));
            return;
        }

        String loadoutName = args[1].toLowerCase(Locale.ROOT);
        OfflinePlayer target = resolveTarget(sender, args, 2);
        if (target == null) {
            return;
        }

        PlayerLoadoutData data = loadoutManager.getData(target.getUniqueId());
        if (loadoutExists(data, loadoutName)) {
            sender.sendMessage(Component.text("'" + loadoutName + "' already exists for " + target.getName() + ".", NamedTextColor.RED));
            return;
        }

        data.setLoadout(loadoutName, InventorySnapshot.empty());
        loadoutManager.save(target.getUniqueId());
        sender.sendMessage(Component.text("Created empty loadout '" + loadoutName + "' for " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /inv delete <loadout> [player]", NamedTextColor.RED));
            return;
        }

        String loadoutName = args[1].toLowerCase(Locale.ROOT);
        OfflinePlayer target = resolveTarget(sender, args, 2);
        if (target == null) {
            return;
        }

        PlayerLoadoutData data = loadoutManager.getData(target.getUniqueId());
        if (loadoutName.equalsIgnoreCase(data.getCurrentLoadout())) {
            sender.sendMessage(Component.text(
                    "Can't delete " + target.getName() + "'s active loadout. Switch away from it first.", NamedTextColor.RED));
            return;
        }
        if (data.getLoadouts().remove(loadoutName) == null) {
            sender.sendMessage(Component.text("'" + loadoutName + "' doesn't exist for " + target.getName() + ".", NamedTextColor.RED));
            return;
        }

        loadoutManager.save(target.getUniqueId());
        sender.sendMessage(Component.text("Deleted loadout '" + loadoutName + "' for " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /inv rename <old> <new> [player]", NamedTextColor.RED));
            return;
        }

        String oldName = args[1].toLowerCase(Locale.ROOT);
        String newName = args[2].toLowerCase(Locale.ROOT);
        OfflinePlayer target = resolveTarget(sender, args, 3);
        if (target == null) {
            return;
        }

        PlayerLoadoutData data = loadoutManager.getData(target.getUniqueId());
        if (loadoutExists(data, newName)) {
            sender.sendMessage(Component.text("'" + newName + "' already exists for " + target.getName() + ".", NamedTextColor.RED));
            return;
        }

        boolean renamedCurrent = oldName.equalsIgnoreCase(data.getCurrentLoadout());
        InventorySnapshot snapshot = data.getLoadouts().remove(oldName);
        if (!renamedCurrent && snapshot == null) {
            sender.sendMessage(Component.text("'" + oldName + "' doesn't exist for " + target.getName() + ".", NamedTextColor.RED));
            return;
        }

        if (snapshot != null) {
            data.setLoadout(newName, snapshot);
        }
        if (renamedCurrent) {
            data.setCurrentLoadout(newName);
        }

        loadoutManager.save(target.getUniqueId());
        sender.sendMessage(Component.text("Renamed loadout '" + oldName + "' to '" + newName + "' for " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private boolean loadoutExists(PlayerLoadoutData data, String name) {
        return name.equalsIgnoreCase(data.getCurrentLoadout()) || data.getLoadouts().containsKey(name);
    }

    private OfflinePlayer resolveTarget(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            String name = args[index];
            OfflinePlayer player = Bukkit.getOfflinePlayer(name);
            if (!player.hasPlayedBefore() && !player.isOnline()) {
                sender.sendMessage(Component.text("Unknown player '" + name + "'.", NamedTextColor.RED));
                return null;
            }
            return player;
        }

        if (sender instanceof Player player) {
            return player;
        }

        sender.sendMessage(Component.text("Console must specify a player name.", NamedTextColor.RED));
        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("InventorySwap commands:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/inv list [player] - list a player's loadouts", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/inv current [player] - show a player's active loadout", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/inv swap <loadout> [player] - swap a player's active inventory", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/inv create <loadout> [player] - create an empty loadout", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/inv delete <loadout> [player] - delete a loadout", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/inv rename <old> <new> [player] - rename a loadout", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list", "current" -> args.length == 2 ? filter(onlinePlayerNames(), args[1]) : List.of();
            case "swap", "create", "delete" -> switch (args.length) {
                case 2 -> filter(allLoadoutNames(sender), args[1]);
                case 3 -> filter(onlinePlayerNames(), args[2]);
                default -> List.of();
            };
            case "rename" -> switch (args.length) {
                case 2 -> filter(allLoadoutNames(sender), args[1]);
                case 4 -> filter(onlinePlayerNames(), args[3]);
                default -> List.of();
            };
            default -> List.of();
        };
    }

    private List<String> allLoadoutNames(CommandSender sender) {
        Set<String> names = new TreeSet<>(loadoutManager.getAllKnownLoadoutNames());
        if (sender instanceof Player player) {
            PlayerLoadoutData data = loadoutManager.getData(player);
            names.add(data.getCurrentLoadout());
            names.addAll(data.getLoadouts().keySet());
        }
        return new ArrayList<>(names);
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(lower))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
