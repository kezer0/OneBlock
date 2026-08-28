package oneblock.network;

import net.milkbowl.vault.economy.Economy;
import oneblock.OneBlock;
import oneblock.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public final class IslandDataService {
    private static final ConcurrentMap<Integer, IslandSettings> SETTINGS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, ConcurrentMap<UUID, IslandRole>> MEMBERS = new ConcurrentHashMap<>();
    private static int[] upgradeSizes = {50, 80, 110, 140, 160};
    private static BigDecimal[] upgradePrices = {
            new BigDecimal("0.00"), new BigDecimal("1000.00"), new BigDecimal("5000.00"),
            new BigDecimal("15000.00"), new BigDecimal("50000.00")};

    private IslandDataService() {
    }

    public static void initialize(OneBlock plugin) {
        loadUpgradeConfig(plugin);
        if (OneBlockDatabase.isEnabled()) {
            try {
                for (OneBlockDatabase.IslandSettingsRow row : OneBlockDatabase.loadIslandSettings())
                    SETTINGS.put(row.islandId, new IslandSettings(row.allowVisits, row.visitorInteract, normalizeSize(row.buildingSize)));
                for (OneBlockDatabase.IslandMemberRow row : OneBlockDatabase.loadIslandMembers())
                    MEMBERS.computeIfAbsent(row.islandId, k -> new ConcurrentHashMap<>()).put(row.uuid, row.role);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load OneBlock database state", ex);
            }
        }
        syncFromFork();
    }

    private static void loadUpgradeConfig(OneBlock plugin) {
        File file = new File(plugin.getDataFolder(), "island-upgrades.yml");
        if (!file.exists()) plugin.saveResource("island-upgrades.yml", false);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<Integer> sizes = cfg.getIntegerList("building-size.sizes");
        List<String> prices = cfg.getStringList("building-size.prices");
        if (sizes.size() >= 1 && prices.size() == sizes.size()) {
            upgradeSizes = new int[sizes.size()];
            upgradePrices = new BigDecimal[sizes.size()];
            for (int i = 0; i < sizes.size(); i++) {
                upgradeSizes[i] = sizes.get(i);
                upgradePrices[i] = new BigDecimal(prices.get(i));
            }
        }
    }

    public static void syncFromFork() {
        for (int i = 0; i < PlayerInfo.size(); i++) {
            PlayerInfo info = PlayerInfo.get(i);
            if (info == null || info.uuid == null) continue;
            ensureIslandCache(i, info.uuid, info.allowVisit);
            for (UUID uuid : info.uuids) ensureMember(i, uuid);
        }
    }

    public static void ensureIslandCache(int islandId, UUID owner, boolean allowVisit) {
        SETTINGS.computeIfAbsent(islandId, k -> new IslandSettings(allowVisit, false, OneBlock.STARTER_ISLAND_SIZE));
        SETTINGS.get(islandId).allowVisits = allowVisit || SETTINGS.get(islandId).allowVisits;
        MEMBERS.computeIfAbsent(islandId, k -> new ConcurrentHashMap<>()).put(owner, IslandRole.OWNER);
        persistIslandAsync(islandId, owner, allowVisit);
    }

    public static void ensureMember(int islandId, UUID uuid) {
        if (uuid == null) return;
        MEMBERS.computeIfAbsent(islandId, k -> new ConcurrentHashMap<>()).put(uuid, IslandRole.MEMBER);
        if (OneBlockDatabase.isEnabled()) Bukkit.getScheduler().runTaskAsynchronously(OneBlock.plugin, () -> {
            try {
                OneBlockDatabase.setIslandMember(islandId, uuid, IslandRole.MEMBER);
            } catch (Exception ex) {
                OneBlock.plugin.getLogger().log(Level.WARNING, "Failed to save island member", ex);
            }
        });
    }

    private static void persistIslandAsync(int islandId, UUID owner, boolean allowVisit) {
        if (!OneBlockDatabase.isEnabled()) return;
        IslandSettings settings = SETTINGS.get(islandId);
        Bukkit.getScheduler().runTaskAsynchronously(OneBlock.plugin, () -> {
            try {
                OneBlockDatabase.ensureIsland(islandId, owner, allowVisit, settings != null && settings.visitorInteract, settings == null ? OneBlock.STARTER_ISLAND_SIZE : settings.buildingSize);
            } catch (Exception ex) {
                OneBlock.plugin.getLogger().log(Level.WARNING, "Failed to persist island " + islandId, ex);
            }
        });
    }

    public static IslandRole getRole(UUID uuid, int islandId) {
        if (uuid == null || islandId < 0) return IslandRole.VISITOR;
        Map<UUID, IslandRole> roles = MEMBERS.get(islandId);
        IslandRole role = roles == null ? null : roles.get(uuid);
        if (role != null) return role;
        IslandSettings settings = SETTINGS.get(islandId);
        return IslandRole.VISITOR;
    }

    public static boolean hasPermission(UUID uuid, int islandId, IslandPermission permission) {
        IslandRole role = getRole(uuid, islandId);
        switch (role) {
            case OWNER:
                return permission != IslandPermission.LOOK_AROUND;
            case MEMBER:
                return permission == IslandPermission.MODIFY_ISLAND || permission == IslandPermission.USE_ISLAND_FEATURES ||
                        permission == IslandPermission.LOOK_AROUND || permission == IslandPermission.INTERACT_ALLOWED;
            case VISITOR:
            default:
                if (permission == IslandPermission.LOOK_AROUND) return canVisitIsland(islandId);
                return permission == IslandPermission.INTERACT_ALLOWED && visitorInteract(islandId);
        }
    }

    public static boolean canVisitIsland(int islandId) {
        IslandSettings s = SETTINGS.get(islandId);
        return s != null && s.allowVisits;
    }

    public static boolean visitorInteract(int islandId) {
        IslandSettings s = SETTINGS.get(islandId);
        return s != null && s.visitorInteract;
    }

    public static void setAllowVisits(int islandId, boolean value) {
        SETTINGS.computeIfAbsent(islandId, k -> new IslandSettings(false, false, OneBlock.STARTER_ISLAND_SIZE)).allowVisits = value;
        saveSettingsAsync(islandId);
        PlayerInfo info = PlayerInfo.get(islandId);
        if (info != null) info.allowVisit = value;
    }

    public static void setVisitorInteract(int islandId, boolean value) {
        SETTINGS.computeIfAbsent(islandId, k -> new IslandSettings(false, false, OneBlock.STARTER_ISLAND_SIZE)).visitorInteract = value;
        saveSettingsAsync(islandId);
    }

    private static void saveSettingsAsync(int islandId) {
        if (!OneBlockDatabase.isEnabled()) return;
        IslandSettings s = SETTINGS.get(islandId);
        if (s == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(OneBlock.plugin, () -> {
            try {
                OneBlockDatabase.setIslandSettings(islandId, s.allowVisits, s.visitorInteract, s.buildingSize);
            } catch (Exception ex) {
                OneBlock.plugin.getLogger().log(Level.WARNING, "Failed to save island settings", ex);
            }
        });
    }

    public static void removeMember(int islandId, UUID uuid) {
        Map<UUID, IslandRole> map = MEMBERS.get(islandId);
        if (map != null) map.remove(uuid);
        if (OneBlockDatabase.isEnabled()) Bukkit.getScheduler().runTaskAsynchronously(OneBlock.plugin, () -> {
            try {
                OneBlockDatabase.removeIslandMember(islandId, uuid);
            } catch (Exception ex) {
                OneBlock.plugin.getLogger().log(Level.WARNING, "Failed to remove island member", ex);
            }
        });
    }

    public static boolean transferOwnership(Player player, UUID newOwner) {
        int islandId = PlayerInfo.getId(player.getUniqueId());
        if (islandId < 0) return false;
        PlayerInfo info = PlayerInfo.get(islandId);
        if (info.uuid == null || !info.uuid.equals(player.getUniqueId()) || !info.uuids.contains(newOwner))
            return false;
        if (!PlayerInfo.transferOwnership(islandId, newOwner)) return false;
        ConcurrentMap<UUID, IslandRole> map = MEMBERS.computeIfAbsent(islandId, k -> new ConcurrentHashMap<>());
        map.put(player.getUniqueId(), IslandRole.MEMBER);
        map.put(newOwner, IslandRole.OWNER);
        if (OneBlockDatabase.isEnabled()) Bukkit.getScheduler().runTaskAsynchronously(OneBlock.plugin, () -> {
            try {
                OneBlockDatabase.transferOwnership(islandId, player.getUniqueId(), newOwner);
            } catch (Exception ex) {
                OneBlock.plugin.getLogger().log(Level.WARNING, "Failed to transfer island ownership", ex);
            }
        });
        return true;
    }

    public static int islandAt(Location location) {
        if (location == null || OneBlock.getWorld() == null || location.getWorld() != OneBlock.getWorld()) return -1;
        int islandId = OneBlock.plugin.findNearestRegionId(location);
        if (islandId < 0 || islandId >= PlayerInfo.size()) return -1;
        int[] center = OneBlock.plugin.getIslandCoordinates(islandId);
        return OneBlock.plugin.isWithinIslandBounds(location, center[0], center[1], getBuildingSize(islandId)) ? islandId : -1;
    }

    public static int getBuildingSize(int islandId) {
        IslandSettings s = SETTINGS.get(islandId);
        return s == null ? OneBlock.STARTER_ISLAND_SIZE : s.buildingSize;
    }

    public static int getNextBuildingSize(int islandId) {
        int current = getBuildingSize(islandId);
        for (int size : upgradeSizes) if (size > current) return size;
        return current;
    }

    public static BigDecimal getNextUpgradePrice(int islandId) {
        int current = getBuildingSize(islandId);
        for (int i = 0; i < upgradeSizes.length; i++) if (upgradeSizes[i] > current) return upgradePrices[i];
        return null;
    }

    public static boolean canUpgrade(int islandId) {
        return getNextBuildingSize(islandId) > getBuildingSize(islandId);
    }

    public static void upgrade(Player owner) {
        if (owner == null) return;
        UUID uuid = owner.getUniqueId();
        int islandId = PlayerInfo.getId(uuid);
        if (islandId < 0 || !PlayerInfo.existsAsOwner(uuid) || !hasPermission(uuid, islandId, IslandPermission.SETTINGS))
            return;
        final int next = getNextBuildingSize(islandId);
        final BigDecimal price = getNextUpgradePrice(islandId);
        if (price == null) return;
        Economy economy = getEconomy();
        if (economy == null) {
            owner.sendMessage("§cNo Vault economy provider is available.");
            return;
        }
        if (!economy.has(owner, price.doubleValue())) {
            owner.sendMessage("§cYou do not have enough money for this upgrade.");
            return;
        }
        if (!economy.withdrawPlayer(owner, price.doubleValue()).transactionSuccess()) {
            owner.sendMessage("§cThe payment could not be completed.");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(OneBlock.plugin, () -> {
            try {
                if (!OneBlockDatabase.isEnabled()) throw new SQLException("OneBlock database unavailable");
                OneBlockDatabase.updateBuildingSize(islandId, next);
                SETTINGS.computeIfAbsent(islandId, k -> new IslandSettings(false, false, OneBlock.STARTER_ISLAND_SIZE)).buildingSize = next;
                Bukkit.getScheduler().runTask(OneBlock.plugin, () -> {
                    owner.sendMessage("§aYour island building area has been expanded to §f" + next + "x" + next + "§a.");
                    if (oneblock.worldguard.OBWorldGuard.isEnabled()) OneBlock.plugin.worldGuard.recreateRegions();
                    OneBlock.plugin.UpdateBorderLocation(owner, owner.getLocation());
                });
            } catch (Exception ex) {
                Bukkit.getScheduler().runTask(OneBlock.plugin, () -> {
                    Economy e = getEconomy();
                    if (e != null) e.depositPlayer(owner, price.doubleValue());
                    owner.sendMessage("§cThe upgrade could not be saved. Your money was returned.");
                });
                OneBlock.plugin.getLogger().log(Level.WARNING, "Island upgrade persistence failed", ex);
            }
        });
    }

    private static Economy getEconomy() {
        org.bukkit.plugin.RegisteredServiceProvider<Economy> r = Bukkit.getServicesManager().getRegistration(Economy.class);
        return r == null ? null : r.getProvider();
    }

    private static int normalizeSize(int size) {
        if (size < OneBlock.STARTER_ISLAND_SIZE) return OneBlock.STARTER_ISLAND_SIZE;
        return Math.min(OneBlock.MAX_ISLAND_SIZE, size);
    }

    private static final class IslandSettings {
        volatile boolean allowVisits;
        volatile boolean visitorInteract;
        volatile int buildingSize;

        IslandSettings(boolean allowVisits, boolean visitorInteract, int buildingSize) {
            this.allowVisits = allowVisits;
            this.visitorInteract = visitorInteract;
            this.buildingSize = buildingSize;
        }
    }
}
