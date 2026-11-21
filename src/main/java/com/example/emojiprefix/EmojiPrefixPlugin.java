private TintManager tintManager;

@Override
public void onEnable() {
    saveDefaultConfig();

    // --- Смайлики ---
    emojiManager = new EmojiManager(this);
    emojiManager.loadEmojis();

    PinsCommand pinsCommand = new PinsCommand(this, emojiManager);
    getCommand("pins").setExecutor(pinsCommand);
    getCommand("pins").setTabCompleter(pinsCommand);

    Bukkit.getPluginManager().registerEvents(new PlayerListener(this, emojiManager), this);

    Bukkit.getOnlinePlayers().forEach(player -> {
        String emoji = emojiManager.getPlayerEmoji(player.getUniqueId());
        if (emoji != null && !emoji.isEmpty()) {
            emojiManager.applyEmoji(player, emoji);
        }
    });

    // --- Покраски ---
    tintManager = new TintManager(this);
    tintManager.loadTints();

    TintsCommand tintsCommand = new TintsCommand(this, tintManager);
    getCommand("tints").setExecutor(tintsCommand);
    getCommand("tints").setTabCompleter(tintsCommand);

    Bukkit.getPluginManager().registerEvents(new TintListener(this, tintManager), this);

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
    emojiManager.cleanup();
    tintManager.cleanup();
    getLogger().info("EmojiPrefix disabled.");
}

public TintManager getTintManager() {
    return tintManager;
}
