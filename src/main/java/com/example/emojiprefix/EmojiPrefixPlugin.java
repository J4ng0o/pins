package com.example.emojiprefix;

import com.example.emojiprefix.command.PinsCommand;
import com.example.emojiprefix.command.TintsCommand;
import com.example.emojiprefix.listener.PlayerListener;
import com.example.emojiprefix.listener.TintListener;
import com.example.emojiprefix.util.EmojiManager;
import com.example.emojiprefix.util.TintManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EmojiPrefixPlugin extends JavaPlugin {

    private EmojiManager emojiManager;
    private TintManager tintManager;

    @Override
    public void onEnable() {
        // Сохраняем конфиг по умолчанию
        saveDefaultConfig();

        // ==================== Смайлики ====================
        emojiManager = new EmojiManager(this);
        emojiManager.loadEmojis();

        PinsCommand pinsCommand = new PinsCommand(this, emojiManager);
        getCommand("pins").setExecutor(pinsCommand);
        getCommand("pins").setTabCompleter(pinsCommand);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this, emojiManager), this);

        // Применяем префиксы ко всем игрокам при старте
        Bukkit.getOnlinePlayers().forEach(player -> {
            String emoji = emojiManager.getPlayerEmoji(player.getUniqueId());
            if (emoji != null && !emoji.isEmpty()) {
                emojiManager.applyEmoji(player, emoji);
            }
        });

        // ==================== Покраски ====================
        tintManager = new TintManager(this);
        tintManager.loadTints();

        TintsCommand tintsCommand = new TintsCommand(this, tintManager);
        getCommand("tints").setExecutor(tintsCommand);
        getCommand("tints").setTabCompleter(tintsCommand);

        Bukkit.getPluginManager().registerEvents(new TintListener(this, tintManager), this);

        // Применяем покраски ко всем игрокам при старте
        Bukkit.getOnlinePlayers().forEach(player -> {
            String tintId = tintManager.getCurrentTintId(player.getUniqueId());
            if (tintId != null && !tintId.isEmpty()) {
                tintManager.applyTint(player, tintId);
            }
        });

        getLogger().info("EmojiPrefix enabled! Loaded " + emojiManager.getEmojiCount()
            + " emojis and " + tintManager.getTintCount() + " tints.");
    }

    @Override
    public void onDisable() {
        // Убираем Scoreboard Team при выключении
        emojiManager.cleanup();
        tintManager.cleanup();
        getLogger().info("EmojiPrefix disabled.");
    }

    public EmojiManager getEmojiManager() {
        return emojiManager;
    }

    public TintManager getTintManager() {
        return tintManager;
    }
}
