package oneblock.network;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

import oneblock.OneBlock;

public final class PlayerDataManager {
    private static final ConcurrentMap<UUID, PlayerData> CACHE = new ConcurrentHashMap<>();
    private static double startingBalance = 1000.0D;

    private PlayerDataManager() {}

    public static void initializeConfig(OneBlock plugin) {
        File file = new File(plugin.getDataFolder(), "database.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        startingBalance = cfg.getDouble("starting-balance", 1000.0D);
    }

    public static void handleJoin(final Player player) {
        if (!NetworkDatabase.isEnabled()) return;
        final UUID uuid = player.getUniqueId();
        final String name = player.getName();
        CACHE.putIfAbsent(uuid, new PlayerData(uuid, name, BigDecimal.valueOf(startingBalance).setScale(2)));
        Bukkit.getScheduler().runTaskAsynchronously(OneBlock.plugin, () -> {
            try {
                PlayerData loaded = NetworkDatabase.loadPlayer(uuid, name, startingBalance);
                CACHE.put(uuid, loaded);
            } catch (Exception ex) {
                OneBlock.plugin.getLogger().log(Level.SEVERE, "Failed to load network player data for " + name, ex);
            }
        });
    }

    public static void handleQuit(Player player) {
        if (player == null) return;
        PlayerData data = CACHE.remove(player.getUniqueId());
        if (data == null || !NetworkDatabase.isEnabled()) return;
        saveAsync(data);
    }

    public static PlayerData get(Player player) {
        return player == null ? null : get(player.getUniqueId(), player.getName());
    }

    public static PlayerData get(UUID uuid, String username) {
        if (uuid == null) return null;
        PlayerData existing = CACHE.get(uuid);
        if (existing != null) return existing;
        PlayerData created = new PlayerData(uuid, username, BigDecimal.valueOf(startingBalance).setScale(2));
        PlayerData raced = CACHE.putIfAbsent(uuid, created);
        return raced == null ? created : raced;
    }

    public static void saveAsync(PlayerData data) {
        if (data == null || !NetworkDatabase.isEnabled()) return;
        Bukkit.getScheduler().runTaskAsynchronously(OneBlock.plugin, () -> {
            try { NetworkDatabase.savePlayer(data); }
            catch (Exception ex) { OneBlock.plugin.getLogger().log(Level.WARNING, "Failed to save network player data for " + data.getUuid(), ex); }
        });
    }

    public static void saveAll() {
        if (!NetworkDatabase.isEnabled()) return;
        for (Map.Entry<UUID, PlayerData> entry : CACHE.entrySet()) {
            try { NetworkDatabase.savePlayer(entry.getValue()); }
            catch (Exception ex) { OneBlock.plugin.getLogger().log(Level.WARNING, "Failed to save network player data for " + entry.getKey(), ex); }
        }
    }

    public static BigDecimal getBalance(UUID uuid) {
        PlayerData data = CACHE.get(uuid);
        return data == null ? BigDecimal.ZERO.setScale(2) : data.getBalance();
    }

    public static boolean tryWithdraw(UUID uuid, BigDecimal amount) {
        PlayerData data = CACHE.get(uuid);
        if (data == null || amount == null || amount.signum() < 0 || data.getBalance().compareTo(amount) < 0) return false;
        data.setBalance(data.getBalance().subtract(amount));
        saveAsync(data);
        return true;
    }
}
