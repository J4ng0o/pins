package com.example.emojiprefix.listener;

import com.example.emojiprefix.EmojiPrefixPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final EmojiPrefixPlugin plugin;

    public PlayerListener(EmojiPrefixPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        String emoji = plugin.getPlayerEmoji(event.getPlayer().getUniqueId());
        if (emoji != null && !emoji.isEmpty()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    plugin.applyEmoji(event.getPlayer(), emoji);
                }
            }, 5L);
        }
    }
}
