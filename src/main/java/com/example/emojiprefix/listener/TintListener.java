package com.example.emojiprefix.listener;

import com.example.emojiprefix.EmojiPrefixPlugin;
import com.example.emojiprefix.util.TintManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TintListener implements Listener {

    private final EmojiPrefixPlugin plugin;
    private final TintManager tintManager;

    public TintListener(EmojiPrefixPlugin plugin, TintManager tintManager) {
        this.plugin = plugin;
        this.tintManager = tintManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String tintId = tintManager.getCurrentTintId(event.getPlayer().getUniqueId());
        if (tintId != null && !tintId.isEmpty()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    // Проверяем, включены ли покраски в таб-листе в конфиге
                    boolean tablistEnabled = plugin.getConfig().getBoolean("tablist-enabled", true);
                    
                    if (tablistEnabled) {
                        // Если включено, применяем покраску (но это может затереть пин)
                        tintManager.applyTint(event.getPlayer(), tintId);
                    } else {
                        // Если выключено в конфиге, покраска не трогает таб-лист
                        // (здесь можно добавить применение только в чат, если у вас есть такая логика)
                        plugin.getLogger().info("Покраска для " + event.getPlayer().getName() + " не применена в таб-листе (отключено в конфиге).");
                    }
                }
            }, 5L);
        }
    }
}
