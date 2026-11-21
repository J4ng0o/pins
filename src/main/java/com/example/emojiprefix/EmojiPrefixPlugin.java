package com.example.emojiprefix;

import com.example.emojiprefix.command.TintsCommand;
import com.example.emojiprefix.listener.TintListener;
import com.example.emojiprefix.util.TintManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EmojiPrefixPlugin extends JavaPlugin {

    private TintManager tintManager;

    @Override
    public void onEnable() {
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

        getLogger().info("Nots enabled! Loaded " + tintManager.getTintCount() + " tints.");
    }

    @Override
    public void onDisable() {
        if (tintManager != null) {
            tintManager.cleanup();
        }
        getLogger().info("Nots disabled.");
    }

    public TintManager getTintManager() {
        return tintManager;
    }
}
