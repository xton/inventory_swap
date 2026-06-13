package com.xton.inventoryswap.command;

import com.xton.inventoryswap.profile.InventorySnapshot;
import com.xton.inventoryswap.profile.PlayerProfileData;
import com.xton.inventoryswap.profile.ProfileManager;
import com.xton.inventoryswap.profile.ProfileSwapService;
import com.xton.inventoryswap.profile.SwapFeedback;
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
import java.util.stream.Collectors;

/**
 * Admin command for inspecting and managing players' inventory profiles.
 */
public class InvSwapCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "list", "current", "switch", "create", "delete", "rename");

    private final ProfileManager profileManager;
    private final ProfileSwapService swapService;

    public InvSwapCommand(ProfileManager profileManager, ProfileSwapService swapService) {
        this.profileManager = profileManager;
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
            case "switch" -> handleSwitch(sender, args);
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

        PlayerProfileData data = profileManager.getData(target.getUniqueId());
        String current = data.getCurrentProfile();

        sender.sendMessage(Component.text("Profiles for " + target.getName() + ":", NamedTextColor.GOLD));
        sender.sendMessage(formatProfileLine(current, current));
        data.getProfiles().keySet().stream()
                .filter(name -> !name.equalsIgnoreCase(current))
                .sorted()
                .forEach(name -> sender.sendMessage(formatProfileLine(name, current)));
    }

    private Component formatProfileLine(String name, String current) {
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

        PlayerProfileData data = profileManager.getData(target.getUniqueId());
        sender.sendMessage(Component.text(
                target.getName() + "'s active profile: " + data.getCurrentProfile(), NamedTextColor.GOLD));
    }

    private void handleSwitch(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /invswap switch <profile> [player]", NamedTextColor.RED));
            return;
        }

        String profileName = args[1].toLowerCase(Locale.ROOT);
        OfflinePlayer target = resolveTarget(sender, args, 2);
        if (target == null) {
            return;
        }

        Player online = target.getPlayer();
        if (online == null) {
            sender.sendMessage(Component.text(
                    target.getName() + " must be online to switch their active inventory.", NamedTextColor.RED));
            return;
        }

        ProfileSwapService.Result result = swapService.switchProfile(online, profileName);
        switch (result) {
            case SWITCHED -> SwapFeedback.showSwitched(online, profileName, false);
            case CREATED -> SwapFeedback.showSwitched(online, profileName, true);
            case ALREADY_ACTIVE -> SwapFeedback.showAlreadyActive(online, profileName);
        }
        sender.sendMessage(Component.text("Switched " + online.getName() + " to '" + profileName + "'.", NamedTextColor.GREEN));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /invswap create <profile> [player]", NamedTextColor.RED));
            return;
        }

        String profileName = args[1].toLowerCase(Locale.ROOT);
        OfflinePlayer target = resolveTarget(sender, args, 2);
        if (target == null) {
            return;
        }

        PlayerProfileData data = profileManager.getData(target.getUniqueId());
        if (profileExists(data, profileName)) {
            sender.sendMessage(Component.text("'" + profileName + "' already exists for " + target.getName() + ".", NamedTextColor.RED));
            return;
        }

        data.setProfile(profileName, InventorySnapshot.empty());
        profileManager.save(target.getUniqueId());
        sender.sendMessage(Component.text("Created empty profile '" + profileName + "' for " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /invswap delete <profile> [player]", NamedTextColor.RED));
            return;
        }

        String profileName = args[1].toLowerCase(Locale.ROOT);
        OfflinePlayer target = resolveTarget(sender, args, 2);
        if (target == null) {
            return;
        }

        PlayerProfileData data = profileManager.getData(target.getUniqueId());
        if (profileName.equalsIgnoreCase(data.getCurrentProfile())) {
            sender.sendMessage(Component.text(
                    "Can't delete " + target.getName() + "'s active profile. Switch away from it first.", NamedTextColor.RED));
            return;
        }
        if (data.getProfiles().remove(profileName) == null) {
            sender.sendMessage(Component.text("'" + profileName + "' doesn't exist for " + target.getName() + ".", NamedTextColor.RED));
            return;
        }

        profileManager.save(target.getUniqueId());
        sender.sendMessage(Component.text("Deleted profile '" + profileName + "' for " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void handleRename(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /invswap rename <old> <new> [player]", NamedTextColor.RED));
            return;
        }

        String oldName = args[1].toLowerCase(Locale.ROOT);
        String newName = args[2].toLowerCase(Locale.ROOT);
        OfflinePlayer target = resolveTarget(sender, args, 3);
        if (target == null) {
            return;
        }

        PlayerProfileData data = profileManager.getData(target.getUniqueId());
        if (profileExists(data, newName)) {
            sender.sendMessage(Component.text("'" + newName + "' already exists for " + target.getName() + ".", NamedTextColor.RED));
            return;
        }

        boolean renamedCurrent = oldName.equalsIgnoreCase(data.getCurrentProfile());
        InventorySnapshot snapshot = data.getProfiles().remove(oldName);
        if (!renamedCurrent && snapshot == null) {
            sender.sendMessage(Component.text("'" + oldName + "' doesn't exist for " + target.getName() + ".", NamedTextColor.RED));
            return;
        }

        if (snapshot != null) {
            data.setProfile(newName, snapshot);
        }
        if (renamedCurrent) {
            data.setCurrentProfile(newName);
        }

        profileManager.save(target.getUniqueId());
        sender.sendMessage(Component.text("Renamed profile '" + oldName + "' to '" + newName + "' for " + target.getName() + ".", NamedTextColor.GREEN));
    }

    private boolean profileExists(PlayerProfileData data, String name) {
        return name.equalsIgnoreCase(data.getCurrentProfile()) || data.getProfiles().containsKey(name);
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
        sender.sendMessage(Component.text("/invswap list [player] - list a player's profiles", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/invswap current [player] - show a player's active profile", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/invswap switch <profile> [player] - swap a player's active inventory", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/invswap create <profile> [player] - create an empty profile", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/invswap delete <profile> [player] - delete a profile", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/invswap rename <old> <new> [player] - rename a profile", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list", "current" -> args.length == 2 ? filter(onlinePlayerNames(), args[1]) : List.of();
            case "switch", "create", "delete" -> switch (args.length) {
                case 2 -> filter(profileNamesFor(sender), args[1]);
                case 3 -> filter(onlinePlayerNames(), args[2]);
                default -> List.of();
            };
            case "rename" -> switch (args.length) {
                case 2 -> filter(profileNamesFor(sender), args[1]);
                case 4 -> filter(onlinePlayerNames(), args[3]);
                default -> List.of();
            };
            default -> List.of();
        };
    }

    private List<String> profileNamesFor(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        PlayerProfileData data = profileManager.getData(player);
        List<String> names = new ArrayList<>();
        names.add(data.getCurrentProfile());
        names.addAll(data.getProfiles().keySet());
        return names;
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
