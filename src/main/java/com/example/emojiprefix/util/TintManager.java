package com.example.emojiprefix.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TintManager {

    private final JavaPlugin plugin;
    private final Map<String, List<TextColor>> availableTints = new LinkedHashMap<>();
    private final Map<UUID, Set<String>> playerUnlockedTints = new HashMap<>();
    private final Map<UUID, String> playerCurrentTint = new HashMap<>();
    private final Map<UUID, String> playerTeamNames = new HashMap<>();

    private File dataFile;
    private FileConfiguration dataConfig;

    private static final String TEAM_PREFIX = "et_";

    public TintManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadDataFile();
    }

    // ==================== Загрузка конфигурации ====================

    public void loadTints() {
        availableTints.clear();
        File tintFile = new File(plugin.getDataFolder(), "colors.yml");
        if (!tintFile.exists()) {
            plugin.saveResource("colors.yml", false);
        }
        FileConfiguration tintConfig = YamlConfiguration.loadConfiguration(tintFile);
        ConfigurationSection section = tintConfig.getConfigurationSection("tints");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                List<String> hexes = section.getStringList(key);
                List<TextColor> colors = new ArrayList<>();
                for (String hex : hexes) {
                    TextColor color = TextColor.fromHexString(hex);
                    if (color != null) {
                        colors.add(color);
                    } else {
                        plugin.getLogger().warning("Invalid hex color '" + hex + "' in tint '" + key + "'");
                    }
                }
                if (!colors.isEmpty()) {
                    availableTints.put(key, colors);
                }
            }
        }
    }

    private void loadDataFile() {
        dataFile = new File(plugin.getDataFolder(), "tintdata.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create tintdata.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadPlayerData();
    }

    private void loadPlayerData() {
        playerUnlockedTints.clear();
        playerCurrentTint.clear();

        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidStr : playersSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);

                    List<String> unlocked = playerSection.getStringList("unlocked");
                    playerUnlockedTints.put(uuid, new HashSet<>(unlocked));

                    String current = playerSection.getString("current");
                    if (current != null && !current.isEmpty()) {
                        playerCurrentTint.put(uuid, current);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in tintdata.yml: " + uuidStr);
                }
            }
        }
    }

    public void savePlayerData() {
        dataConfig.set("players", null);

        Set<UUID> allUuids = new HashSet<>();
        allUuids.addAll(playerUnlockedTints.keySet());
        allUuids.addAll(playerCurrentTint.keySet());

        for (UUID uuid : allUuids) {
            String path = "players." + uuid.toString();

            Set<String> unlocked = playerUnlockedTints.get(uuid);
            if (unlocked != null && !unlocked.isEmpty()) {
                dataConfig.set(path + ".unlocked", new ArrayList<>(unlocked));
            }

            String current = playerCurrentTint.get(uuid);
            if (current != null) {
                dataConfig.set(path + ".current", current);
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save tintdata.yml!");
        }
    }

    // ==================== Управление покрасками ====================

    public Map<String, List<TextColor>> getAvailableTints() {
        return Collections.unmodifiableMap(availableTints);
    }

    public int getTintCount() {
        return availableTints.size();
    }

    public Set<String> getUnlockedTints(UUID uuid) {
        return playerUnlockedTints.getOrDefault(uuid, Collections.emptySet());
    }

    public List<String> getUnlockedTintsForDisplay(UUID uuid) {
        Set<String> unlocked = getUnlockedTints(uuid);
        List<String> result = new ArrayList<>();
        for (String tintId : availableTints.keySet()) {
            if (unlocked.contains(tintId)) {
                result.add(tintId);
            }
        }
        return result;
    }

    public String getCurrentTintId(UUID uuid) {
        return playerCurrentTint.get(uuid);
    }

    public boolean unlockTint(UUID uuid, String tintId) {
        if (!availableTints.containsKey(tintId)) {
            return false;
        }
        playerUnlockedTints.computeIfAbsent(uuid, k -> new HashSet<>()).add(tintId);
        savePlayerData();
        return true;
    }

    public boolean lockTint(UUID uuid, String tintId) {
        if (!availableTints.containsKey(tintId)) {
            return false;
        }

        Set<String> unlocked = playerUnlockedTints.get(uuid);
        if (unlocked == null || !unlocked.contains(tintId)) {
            return false;
        }

        unlocked.remove(tintId);
        if (unlocked.isEmpty()) {
            playerUnlockedTints.remove(uuid);
        }

        if (tintId.equals(playerCurrentTint.get(uuid))) {
            playerCurrentTint.remove(uuid);

            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                applyTint(player, "");
            }
        }

        savePlayerData();
        return true;
    }

    public int lockAllTints(UUID uuid) {
        Set<String> unlocked = playerUnlockedTints.get(uuid);
        int count = (unlocked == null) ? 0 : unlocked.size();

        boolean hadCurrent = playerCurrentTint.containsKey(uuid);

        if (unlocked != null) {
            unlocked.clear();
            playerUnlockedTints.remove(uuid);
        }
        playerCurrentTint.remove(uuid);

        if (hadCurrent || count > 0) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                applyTint(player, "");
            }
        }

        savePlayerData();
        return count;
    }

    public boolean setPlayerTint(UUID uuid, String tintId) {
        if (!availableTints.containsKey(tintId)) {
            return false;
        }
        if (!getUnlockedTints(uuid).contains(tintId)) {
            return false;
        }

        playerCurrentTint.put(uuid, tintId);
        savePlayerData();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyTint(player, tintId);
        }
        return true;
    }

    public boolean clearCurrentTint(UUID uuid) {
        if (!playerCurrentTint.containsKey(uuid)) {
            return false;
        }

        playerCurrentTint.remove(uuid);
        savePlayerData();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyTint(player, "");
        }
        return true;
    }

    // ==================== Применение покраски ====================

    /**
     * Применяет покраску к нику игрока (в табе и над головой).
     * @param tintId ID покраски или пустая строка, чтобы снять.
     */
    public void applyTint(Player player, String tintId) {
        if (player == null || !player.isOnline()) return;

        Component coloredName;
        String plainName = player.getName();

        if (tintId == null || tintId.isEmpty() || !availableTints.containsKey(tintId)) {
            coloredName = Component.text(plainName);
        } else {
            coloredName = buildGradient(plainName, availableTints.get(tintId));
        }

        // 1. TabList
        if (plugin.getConfig().getBoolean("tablist-enabled", true)) {
            player.playerListName(coloredName);
        }

        // 2. Nametag через Scoreboard Team
        if (plugin.getConfig().getBoolean("nametag-enabled", true)) {
            applyNametag(player, tintId);
        }
    }

    /**
     * Строит компонент с градиентом по символам.
     */
    private Component buildGradient(String text, List<TextColor> colors) {
        if (colors.isEmpty()) {
            return Component.text(text);
        }
        if (colors.size() == 1) {
            return Component.text(text, colors.get(0));
        }

        Component result = Component.empty();
        int length = text.length();
        int segments = colors.size() - 1;

        for (int i = 0; i < length; i++) {
            double ratio = (length <= 1) ? 0 : (double) i / (length - 1);
            double scaled = ratio * segments;
            int idx = (int) Math.floor(scaled);
            if (idx >= segments) idx = segments - 1;
            double localRatio = scaled - idx;

            TextColor c1 = colors.get(idx);
            TextColor c2 = colors.get(idx + 1);

            int r = (int) Math.round(c1.red() + (c2.red() - c1.red()) * localRatio);
            int g = (int) Math.round(c1.green() + (c2.green() - c1.green()) * localRatio);
            int b = (int) Math.round(c1.blue() + (c2.blue() - c1.blue()) * localRatio);

            TextColor mixed = TextColor.color(r, g, b);
            result = result.append(Component.text(String.valueOf(text.charAt(i)), mixed));
        }
        return result;
    }

    private void applyNametag(Player player, String tintId) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = TEAM_PREFIX + player.getUniqueId().toString().substring(0, 12);

        Team team = scoreboard.getTeam(teamName);

        if (tintId == null || tintId.isEmpty() || !availableTints.containsKey(tintId)) {
            if (team != null) {
                team.unregister();
                playerTeamNames.remove(player.getUniqueId());
            }
            return;
        }

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        // В Scoreboard Team нельзя задать градиент напрямую — только один цвет.
        // Используем первый цвет градиента как основной.
        List<TextColor> colors = availableTints.get(tintId);
        String hex = colors.get(0).asHexString();
        // Формат §x§R§R§G§G§B§B для точного HEX
        String legacy = "§x§" + hex.substring(1).replaceAll("(..)", "§$1");
        team.setPrefix(legacy);

        if (!team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }

        playerTeamNames.put(player.getUniqueId(), teamName);
    }

    public void cleanup() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (String teamName : playerTeamNames.values()) {
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        }
        playerTeamNames.clear();
    }

    // ==================== Утилиты ====================

    public boolean isTintAvailable(String tintId) {
        return availableTints.containsKey(tintId);
    }

    public List<String> getAllTintIds() {
        return new ArrayList<>(availableTints.keySet());
    }
}
