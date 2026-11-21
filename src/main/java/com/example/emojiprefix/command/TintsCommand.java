package com.example.emojiprefix.command;

import com.example.emojiprefix.EmojiPrefixPlugin;
import com.example.emojiprefix.util.TintManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TintsCommand implements CommandExecutor, TabCompleter {

    private final EmojiPrefixPlugin plugin;
    private final TintManager tintManager;

    public TintsCommand(EmojiPrefixPlugin plugin, TintManager tintManager) {
        this.plugin = plugin;
        this.tintManager = tintManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Эта команда только для игроков!", NamedTextColor.RED));
                return true;
            }
            showAvailableTints(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "set" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Эта команда только для игроков!", NamedTextColor.RED));
                    return true;
                }
                handleSet(player, args);
            }
            case "clear", "reset" -> {
                if (!(sender instanceof Player) && args.length < 2) {
                    sender.sendMessage(Component.text("Из консоли используйте: /tints clear <ник>", NamedTextColor.RED));
                    return true;
                }
                handleClear(sender, args);
            }
            case "give" -> {
                if (!sender.hasPermission("emojiprefix.admin")) {
                    sender.sendMessage(Component.text("У вас нет прав на эту команду!", NamedTextColor.RED));
                    return true;
                }
                handleGive(sender, args);
            }
            case "take" -> {
                if (!sender.hasPermission("emojiprefix.admin")) {
                    sender.sendMessage(Component.text("У вас нет прав на эту команду!", NamedTextColor.RED));
                    return true;
                }
                handleTake(sender, args);
            }
            case "takeall" -> {
                if (!sender.hasPermission("emojiprefix.admin")) {
                    sender.sendMessage(Component.text("У вас нет прав на эту команду!", NamedTextColor.RED));
                    return true;
                }
                handleTakeAll(sender, args);
            }
            default -> sender.sendMessage(Component.text(
                "Неизвестная подкоманда. Доступно: /tints, /tints set <tint>, /tints clear [player], /tints give <player> <tint>, /tints take <player> <tint>, /tints takeall <player>",
                NamedTextColor.RED));
        }
        return true;
    }

    private void showAvailableTints(Player player) {
        List<String> unlocked = tintManager.getUnlockedTintsForDisplay(player.getUniqueId());
        String currentTintId = tintManager.getCurrentTintId(player.getUniqueId());

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Ваши доступные покраски:", NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));

        if (unlocked.isEmpty()) {
            player.sendMessage(Component.text("У вас пока нет доступных покрасок.", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Попросите администратора выдать вам покраску!", NamedTextColor.GRAY));
        } else {
            for (String tintId : unlocked) {
                boolean isCurrent = tintId.equals(currentTintId);

                Component line = Component.text("[" + tintId + "]", isCurrent ? NamedTextColor.GREEN : NamedTextColor.YELLOW);

                if (isCurrent) {
                    line = line.append(Component.text(" ✔ (установлен)", NamedTextColor.GREEN));
                }

                player.sendMessage(line);
            }

            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("Используйте: /tints set <id>", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Снять покраску: /tints clear", NamedTextColor.GRAY));
        }

        player.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));
    }

    private void handleSet(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Использование: /tints set <tint_id>", NamedTextColor.RED));
            player.sendMessage(Component.text("Просмотреть доступные: /tints", NamedTextColor.GRAY));
            return;
        }

        String tintId = args[1];

        if (!tintManager.isTintAvailable(tintId)) {
            player.sendMessage(Component.text("Покраска '" + tintId + "' не существует!", NamedTextColor.RED));
            return;
        }

        if (!tintManager.getUnlockedTints(player.getUniqueId()).contains(tintId)) {
            player.sendMessage(Component.text("Вы не разблокировали эту покраску!", NamedTextColor.RED));
            return;
        }

        boolean success = tintManager.setPlayerTint(player.getUniqueId(), tintId);

        if (success) {
            player.sendMessage(Component.text("Покраска '" + tintId + "' установлена!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Не удалось установить покраску!", NamedTextColor.RED));
        }
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Из консоли нужно указать ник: /tints clear <ник>", NamedTextColor.RED));
                return;
            }

            boolean success = tintManager.clearCurrentTint(player.getUniqueId());
            if (success) {
                player.sendMessage(Component.text("Ваша покраска снята.", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("У вас не установлена покраска.", NamedTextColor.YELLOW));
            }
            return;
        }

        if (!sender.hasPermission("emojiprefix.admin")) {
            sender.sendMessage(Component.text("У вас нет прав снимать покраски у других игроков!", NamedTextColor.RED));
            return;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Игрок '" + playerName + "' не найден!", NamedTextColor.RED));
            return;
        }

        boolean success = tintManager.clearCurrentTint(target.getUniqueId());
        if (success) {
            sender.sendMessage(Component.text("Вы сняли покраску с игрока " + target.getName() + ".", NamedTextColor.GREEN));
            target.sendMessage(Component.text("С вас сняли покраску.", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("У игрока " + target.getName() + " нет установленной покраски.", NamedTextColor.YELLOW));
        }
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Использование: /tints give <player> <tint_id>", NamedTextColor.RED));

            StringBuilder sb = new StringBuilder();
            for (String id : tintManager.getAllTintIds()) {
                sb.append(id).append(" ");
            }
            sender.sendMessage(Component.text("Доступные: " + sb.toString().trim(), NamedTextColor.YELLOW));
            return;
        }

        String playerName = args[1];
        String tintId = args[2];

        Player target = Bukkit.getPlayerExact(playerName);

        if (target == null) {
            sender.sendMessage(Component.text("Игрок '" + playerName + "' не найден!", NamedTextColor.RED));
            return;
        }

        if (!tintManager.isTintAvailable(tintId)) {
            sender.sendMessage(Component.text("Покраска '" + tintId + "' не существует!", NamedTextColor.RED));
            return;
        }

        boolean success = tintManager.unlockTint(target.getUniqueId(), tintId);

        if (success) {
            sender.sendMessage(Component.text("Вы выдали покраску '" + tintId + "' игроку " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Вам выдана новая покраска: " + tintId, NamedTextColor.GOLD));
            target.sendMessage(Component.text("Установите: /tints set " + tintId, NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text("Не удалось выдать покраску!", NamedTextColor.RED));
        }
    }

    private void handleTake(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Использование: /tints take <player> <tint_id>", NamedTextColor.RED));
            return;
        }

        String playerName = args[1];
        String tintId = args[2];

        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Игрок '" + playerName + "' не найден!", NamedTextColor.RED));
            return;
        }

        if (!tintManager.isTintAvailable(tintId)) {
            sender.sendMessage(Component.text("Покраска '" + tintId + "' не существует!", NamedTextColor.RED));
            return;
        }

        boolean success = tintManager.lockTint(target.getUniqueId(), tintId);

        if (success) {
            sender.sendMessage(Component.text("Вы забрали покраску '" + tintId + "' у игрока " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("У вас забрали покраску: " + tintId, NamedTextColor.RED));
        } else {
            sender.sendMessage(Component.text("У игрока " + target.getName() + " нет этой покраски!", NamedTextColor.YELLOW));
        }
    }

    private void handleTakeAll(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Использование: /tints takeall <player>", NamedTextColor.RED));
            return;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayerExact(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Игрок '" + playerName + "' не найден!", NamedTextColor.RED));
            return;
        }

        int count = tintManager.lockAllTints(target.getUniqueId());

        if (count > 0) {
            sender.sendMessage(Component.text("Вы забрали " + count + " покраску(и) у игрока " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("У вас забрали все покраски (" + count + " шт.)", NamedTextColor.RED));
        } else {
            sender.sendMessage(Component.text("У игрока " + target.getName() + " нет покрасок!", NamedTextColor.YELLOW));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("set");
            subs.add("clear");
            if (sender.hasPermission("emojiprefix.admin")) {
                subs.add("give");
                subs.add("take");
                subs.add("takeall");
            }
            return subs.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set")) {
                if (sender instanceof Player player) {
                    return tintManager.getUnlockedTintsForDisplay(player.getUniqueId()).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }

            if (args[0].equalsIgnoreCase("give")
                || args[0].equalsIgnoreCase("take")
                || args[0].equalsIgnoreCase("takeall")
                || args[0].equalsIgnoreCase("clear")) {

                if (sender.hasPermission("emojiprefix.admin")) {
                    return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }

        if (args.length == 3
            && sender.hasPermission("emojiprefix.admin")
            && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("take"))) {
            return tintManager.getAllTintIds().stream()
                .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
