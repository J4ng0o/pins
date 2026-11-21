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
                    tintManager.applyTint(event.getPlayer(), tintId);
                }
            }, 5L);
        }
    }
}
