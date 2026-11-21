package com.example.emojiprefix;

import com.example.emojiprefix.command.PinsCommand;
import com.example.emojiprefix.command.TintsCommand;
import com.example.emojiprefix.listener.PlayerListener;
import com.example.emojiprefix.listener.TintListener;
import com.example.emojiprefix.util.TintManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class EmojiPrefixPlugin extends JavaPlugin {

    private TintManager tintManager;

    // ==================== ХРАНИЛИЩЕ СМАЙЛИКОВ ====================
    private final Map<String, String> availableEmojis = new HashMap<>();
    private final Map<UUID, String> currentEmojis = new HashMap<>();
    private final Map<UUID, List<String>> unlockedEmojis = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // ==================== ЗАГРУЗКА СМАЙЛИКОВ ====================
        loadEmojiData();

        PinsCommand pinsCommand = new PinsCommand(this, null); // null передаем, так как логика теперь тут
        getCommand("pins").setExecutor(pinsCommand);
        getCommand("pins").setTabCompleter(pinsCommand);

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this, null), this);

        // ==================== ПОКРАСКИ ====================
        tintManager = new TintManager(this);
        tintManager.loadTints();

        TintsCommand tintsCommand = new TintsCommand(this, tintManager);
        getCommand("tints").setExecutor(tintsCommand);
        getCommand("tints").setTabCompleter(tintsCommand);

        Bukkit.getPluginManager().registerEvents(new TintListener(this, tintManager), this);

        getLogger().info("EmojiPrefix enabled! Loaded " + availableEmojis.size()
            + " emojis and " + tintManager.getTintCount() + " tints.");
    }

    @Override
    public void onDisable() {
        saveEmojiData(); // Сохраняем при выключении
        cleanupEmojis();
        if (tintManager != null) tintManager.cleanup();
        getLogger().info("EmojiPrefix disabled.");
    }

    // ==================== ЛОГИКА СМАЙЛИКОВ ====================

    public void loadEmojiData() {
        // 1. Читаем доступные смайлики из config.yml
        availableEmojis.clear();
        if (getConfig().getConfigurationSection("emojis") != null) {
            for (String key : getConfig().getConfigurationSection("emojis").getKeys(false)) {
                availableEmojis.put(key.toLowerCase(), getConfig().getString("emojis." + key));
            }
        }

        // 2. Читаем playerdata.yml
        dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                getLogger().severe("Не удалось создать playerdata.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        currentEmojis.clear();
        unlockedEmojis.clear();

        if (dataConfig.getConfigurationSection("players") != null) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String current = dataConfig.getString("players." + uuidStr + ".current");
                    if (current != null) currentEmojis.put(uuid, current.toLowerCase());

                    List<String> unlocked = dataConfig.getStringList("players." + uuidStr + ".unlocked");
                    List<String> lower = new ArrayList<>();
                    for (String s : unlocked) lower.add(s.toLowerCase());
                    unlockedEmojis.put(uuid, lower);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveEmojiData() {
        if (dataConfig == null || dataFile == null) return;
        dataConfig.set("players", null); // Очищаем старое
        for (UUID uuid : currentEmojis.keySet()) {
            dataConfig.set("players." + uuid + ".current", currentEmojis.get(uuid));
        }
        for (UUID uuid : unlockedEmojis.keySet()) {
            dataConfig.set("players." + uuid + ".unlocked", unlockedEmojis.get(uuid));
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Не удалось сохранить playerdata.yml!");
        }
    }

    // Методы для команд и листенеров
    public String getPlayerEmoji(UUID uuid) { return currentEmojis.getOrDefault(uuid, ""); }
    public String getCurrentEmojiId(UUID uuid) { return currentEmojis.get(uuid); }
    public boolean isEmojiAvailable(String id) { return availableEmojis.containsKey(id.toLowerCase()); }
    public String getEmojiSymbol(String id) { return availableEmojis.getOrDefault(id.toLowerCase(), ""); }
    public Set<String> getAllEmojiIds() { return availableEmojis.keySet(); }
    public int getEmojiCount() { return availableEmojis.size(); }
    public List<String> getUnlockedEmojis(UUID uuid) { return unlockedEmojis.getOrDefault(uuid, new ArrayList<>()); }
    public List<String> getUnlockedEmojisForDisplay(UUID uuid) { return getUnlockedEmojis(uuid); }

    public boolean setPlayerEmoji(UUID uuid, String id) {
        if (!isEmojiAvailable(id)) return false;
        currentEmojis.put(uuid, id.toLowerCase());
        saveEmojiData(); // СРАЗУ СОХРАНЯЕМ!
        return true;
    }

    public boolean clearCurrentEmoji(UUID uuid) {
        if (currentEmojis.containsKey(uuid)) {
            currentEmojis.remove(uuid);
            saveEmojiData();
            return true;
        }
        return false;
    }

    public boolean unlockEmoji(UUID uuid, String id) {
        if (!isEmojiAvailable(id)) return false;
        List<String> list = unlockedEmojis.computeIfAbsent(uuid, k -> new ArrayList<>());
        if (!list.contains(id.toLowerCase())) {
            list.add(id.toLowerCase());
            saveEmojiData();
            return true;
        }
        return false;
    }

    public boolean lockEmoji(UUID uuid, String id) {
        List<String> list = unlockedEmojis.get(uuid);
        if (list != null && list.remove(id.toLowerCase())) {
            if (id.equalsIgnoreCase(getCurrentEmojiId(uuid))) clearCurrentEmoji(uuid);
            saveEmojiData();
            return true;
        }
        return false;
    }

    public int lockAllEmojis(UUID uuid) {
        List<String> list = unlockedEmojis.get(uuid);
        if (list != null && !list.isEmpty()) {
            int count = list.size();
            unlockedEmojis.remove(uuid);
            clearCurrentEmoji(uuid);
            saveEmojiData();
            return count;
        }
        return 0;
    }

    public void applyEmoji(Player player, String emojiId) {
        String symbol = getEmojiSymbol(emojiId);
        if (symbol.isEmpty()) return;
        if (!getConfig().getBoolean("tablist-enabled", true) && !getConfig().getBoolean("nametag-enabled", true)) return;

        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "ep_" + player.getName();
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);

        if (getConfig().getBoolean("tablist-enabled", true)) team.setPrefix(symbol + " ");
        team.addEntry(player.getName());
    }

    public void cleanupEmojis() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : new ArrayList<>(board.getTeams())) {
            if (team.getName().startsWith("ep_")) team.unregister();
        }
    }

    public TintManager getTintManager() {
        return tintManager;
    }
}
