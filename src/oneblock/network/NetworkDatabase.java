package oneblock.network;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import oneblock.OneBlock;

/**
 * PostgreSQL persistence for the new network-owned systems only.
 * The fork's own island persistence remains in oneblock.storage.DatabaseManager.
 */
public final class NetworkDatabase {
    private static HikariDataSource dataSource;
    private static boolean enabled;

    private NetworkDatabase() {}

    public static void initialize(OneBlock plugin) {
        File file = new File(plugin.getDataFolder(), "database.yml");
        if (!file.exists()) plugin.saveResource("database.yml", false);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        enabled = cfg.getBoolean("enabled", true);
        if (!enabled) {
            plugin.getLogger().info("Network PostgreSQL database is disabled.");
            return;
        }

        try {
            HikariConfig hikari = new HikariConfig();
            String host = cfg.getString("host", "localhost");
            int port = cfg.getInt("port", 5432);
            String database = cfg.getString("database", "oneblock");
            String username = cfg.getString("username", "postgres");
            String password = cfg.getString("password", "superpassword");

            hikari.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
            hikari.setDriverClassName("org.postgresql.Driver");
            hikari.setUsername(username);
            hikari.setPassword(password);
            hikari.setMaximumPoolSize(Math.max(2, cfg.getInt("pool.maximum", 8)));
            hikari.setMinimumIdle(Math.max(1, cfg.getInt("pool.minimum-idle", 2)));
            hikari.setConnectionTimeout(cfg.getLong("pool.connection-timeout-ms", 10000L));
            hikari.setMaxLifetime(cfg.getLong("pool.max-lifetime-ms", 1800000L));
            hikari.setPoolName("OneBlock-NetworkDB");

            dataSource = new HikariDataSource(hikari);
            createTables();
            plugin.getLogger().info("Network PostgreSQL database initialized successfully.");
        } catch (Exception ex) {
            enabled = false;
            dataSource = null;
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to initialize the network PostgreSQL database. Plugin gameplay will continue without the new DB systems.", ex);
        }
    }

    public static boolean isEnabled() {
        return enabled && dataSource != null && !dataSource.isClosed();
    }

    private static void createTables() throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS player (" +
                    "uuid UUID PRIMARY KEY, username VARCHAR(16) NOT NULL, " +
                    "first_joined TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "last_seen TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE IF NOT EXISTS player_economy (" +
                    "uuid UUID PRIMARY KEY REFERENCES player(uuid) ON DELETE CASCADE, " +
                    "balance NUMERIC(19,2) NOT NULL DEFAULT 0)");
            s.execute("CREATE TABLE IF NOT EXISTS player_skills (" +
                    "uuid UUID PRIMARY KEY REFERENCES player(uuid) ON DELETE CASCADE, " +
                    "mining_level INT NOT NULL DEFAULT 1, " +
                    "building_level INT NOT NULL DEFAULT 1, " +
                    "combat_level INT NOT NULL DEFAULT 1)");
            s.execute("CREATE TABLE IF NOT EXISTS player_quest (" +
                    "uuid UUID NOT NULL REFERENCES player(uuid) ON DELETE CASCADE, " +
                    "quest_id VARCHAR(64) NOT NULL, progress INT NOT NULL DEFAULT 0, " +
                    "completed BOOLEAN NOT NULL DEFAULT FALSE, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "PRIMARY KEY(uuid, quest_id))");
            s.execute("CREATE TABLE IF NOT EXISTS island_member (" +
                    "island_id INT NOT NULL, uuid UUID NOT NULL REFERENCES player(uuid) ON DELETE CASCADE, " +
                    "role VARCHAR(16) NOT NULL CHECK(role IN ('OWNER','MEMBER')), " +
                    "PRIMARY KEY(island_id, uuid))");
            s.execute("CREATE TABLE IF NOT EXISTS island_settings (" +
                    "island_id INT PRIMARY KEY, allow_visits BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "visitor_interact BOOLEAN NOT NULL DEFAULT FALSE, building_size INT NOT NULL DEFAULT 50)");
        }
    }

    public static PlayerData loadPlayer(UUID uuid, String username, double startingBalance) throws SQLException {
        if (!isEnabled()) return new PlayerData(uuid, username, BigDecimal.valueOf(startingBalance).setScale(2));
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                boolean exists;
                try (PreparedStatement p = c.prepareStatement("SELECT username FROM player WHERE uuid=?")) {
                    p.setObject(1, uuid);
                    try (ResultSet rs = p.executeQuery()) { exists = rs.next(); }
                }

                if (!exists) {
                    try (PreparedStatement p = c.prepareStatement("INSERT INTO player(uuid, username) VALUES (?, ?)")) {
                        p.setObject(1, uuid); p.setString(2, username == null ? "Unknown" : username); p.executeUpdate();
                    }
                    try (PreparedStatement p = c.prepareStatement("INSERT INTO player_economy(uuid,balance) VALUES (?,?)")) {
                        p.setObject(1, uuid); p.setBigDecimal(2, BigDecimal.valueOf(startingBalance).setScale(2)); p.executeUpdate();
                    }
                    try (PreparedStatement p = c.prepareStatement("INSERT INTO player_skills(uuid) VALUES (?)")) {
                        p.setObject(1, uuid); p.executeUpdate();
                    }
                } else {
                    try (PreparedStatement p = c.prepareStatement("UPDATE player SET username=?, last_seen=CURRENT_TIMESTAMP WHERE uuid=?")) {
                        p.setString(1, username == null ? "Unknown" : username); p.setObject(2, uuid); p.executeUpdate();
                    }
                    try (PreparedStatement p = c.prepareStatement("INSERT INTO player_economy(uuid) VALUES (?) ON CONFLICT (uuid) DO NOTHING")) {
                        p.setObject(1, uuid); p.executeUpdate();
                    }
                    try (PreparedStatement p = c.prepareStatement("INSERT INTO player_skills(uuid) VALUES (?) ON CONFLICT (uuid) DO NOTHING")) {
                        p.setObject(1, uuid); p.executeUpdate();
                    }
                }

                BigDecimal balance = BigDecimal.ZERO.setScale(2);
                try (PreparedStatement p = c.prepareStatement("SELECT balance FROM player_economy WHERE uuid=?")) {
                    p.setObject(1, uuid);
                    try (ResultSet rs = p.executeQuery()) { if (rs.next()) balance = rs.getBigDecimal(1); }
                }
                PlayerData data = new PlayerData(uuid, username, balance);
                try (PreparedStatement p = c.prepareStatement("SELECT mining_level, building_level, combat_level FROM player_skills WHERE uuid=?")) {
                    p.setObject(1, uuid);
                    try (ResultSet rs = p.executeQuery()) {
                        if (rs.next()) {
                            data.getSkills().put("mining", rs.getInt(1));
                            data.getSkills().put("building", rs.getInt(2));
                            data.getSkills().put("combat", rs.getInt(3));
                        }
                    }
                }
                try (PreparedStatement p = c.prepareStatement("SELECT quest_id, progress, completed FROM player_quest WHERE uuid=?")) {
                    p.setObject(1, uuid);
                    try (ResultSet rs = p.executeQuery()) {
                        while (rs.next()) data.getQuests().put(rs.getString(1),
                                new PlayerData.QuestData(rs.getString(1), rs.getInt(2), rs.getBoolean(3)));
                    }
                }
                c.commit();
                return data;
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            }
        }
    }

    public static void savePlayer(PlayerData data) throws SQLException {
        if (!isEnabled() || data == null) return;
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("UPDATE player SET username=?, last_seen=CURRENT_TIMESTAMP WHERE uuid=?")) {
                    p.setString(1, data.getUsername()); p.setObject(2, data.getUuid()); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO player_economy(uuid,balance) VALUES (?,?) ON CONFLICT (uuid) DO UPDATE SET balance=EXCLUDED.balance")) {
                    p.setObject(1, data.getUuid()); p.setBigDecimal(2, data.getBalance()); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO player_skills(uuid,mining_level,building_level,combat_level) VALUES (?,?,?,?) " +
                        "ON CONFLICT (uuid) DO UPDATE SET mining_level=EXCLUDED.mining_level, building_level=EXCLUDED.building_level, combat_level=EXCLUDED.combat_level")) {
                    p.setObject(1, data.getUuid());
                    p.setInt(2, skill(data, "mining")); p.setInt(3, skill(data, "building")); p.setInt(4, skill(data, "combat"));
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO player_quest(uuid,quest_id,progress,completed) VALUES (?,?,?,?) " +
                        "ON CONFLICT (uuid,quest_id) DO UPDATE SET progress=EXCLUDED.progress, completed=EXCLUDED.completed, updated_at=CURRENT_TIMESTAMP")) {
                    for (PlayerData.QuestData q : data.getQuests().values()) {
                        p.setObject(1, data.getUuid()); p.setString(2, q.getQuestId()); p.setInt(3, q.getProgress()); p.setBoolean(4, q.isCompleted()); p.addBatch();
                    }
                    p.executeBatch();
                }
                c.commit();
            } catch (SQLException ex) {
                c.rollback(); throw ex;
            }
        }
    }

    private static int skill(PlayerData data, String name) {
        Integer value = data.getSkills().get(name);
        return value == null ? 1 : Math.max(1, value);
    }

    public static List<IslandMemberRow> loadIslandMembers() throws SQLException {
        List<IslandMemberRow> rows = new ArrayList<>();
        if (!isEnabled()) return rows;
        try (Connection c = dataSource.getConnection(); PreparedStatement p = c.prepareStatement("SELECT island_id,uuid,role FROM island_member")) {
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) rows.add(new IslandMemberRow(rs.getInt(1), (UUID) rs.getObject(2), IslandRole.valueOf(rs.getString(3))));
            }
        }
        return rows;
    }

    public static List<IslandSettingsRow> loadIslandSettings() throws SQLException {
        List<IslandSettingsRow> rows = new ArrayList<>();
        if (!isEnabled()) return rows;
        try (Connection c = dataSource.getConnection(); PreparedStatement p = c.prepareStatement("SELECT island_id,allow_visits,visitor_interact,building_size FROM island_settings")) {
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) rows.add(new IslandSettingsRow(rs.getInt(1), rs.getBoolean(2), rs.getBoolean(3), rs.getInt(4)));
            }
        }
        return rows;
    }

    public static void ensureIsland(int islandId, UUID owner, boolean allowVisits, int buildingSize) throws SQLException {
        if (!isEnabled()) return;
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("INSERT INTO island_settings(island_id,allow_visits,building_size) VALUES (?,?,?) " +
                        "ON CONFLICT(island_id) DO NOTHING")) {
                    p.setInt(1, islandId); p.setBoolean(2, allowVisits); p.setInt(3, buildingSize); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO island_member(island_id,uuid,role) VALUES (?,?, 'OWNER') " +
                        "ON CONFLICT (island_id,uuid) DO UPDATE SET role='OWNER'")) {
                    p.setInt(1, islandId); p.setObject(2, owner); p.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) { c.rollback(); throw ex; }
        }
    }

    public static void setIslandMember(int islandId, UUID uuid, IslandRole role) throws SQLException {
        if (!isEnabled() || role == IslandRole.VISITOR) return;
        try (Connection c = dataSource.getConnection(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO island_member(island_id,uuid,role) VALUES (?,?,?) ON CONFLICT(island_id,uuid) DO UPDATE SET role=EXCLUDED.role")) {
            p.setInt(1, islandId); p.setObject(2, uuid); p.setString(3, role.name()); p.executeUpdate();
        }
    }

    public static void removeIslandMember(int islandId, UUID uuid) throws SQLException {
        if (!isEnabled()) return;
        try (Connection c = dataSource.getConnection(); PreparedStatement p = c.prepareStatement("DELETE FROM island_member WHERE island_id=? AND uuid=? AND role='MEMBER'")) {
            p.setInt(1, islandId); p.setObject(2, uuid); p.executeUpdate();
        }
    }

    public static void setIslandSettings(int islandId, boolean allowVisits, boolean visitorInteract) throws SQLException {
        if (!isEnabled()) return;
        try (Connection c = dataSource.getConnection(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO island_settings(island_id,allow_visits,visitor_interact) VALUES (?,?,?) " +
                "ON CONFLICT(island_id) DO UPDATE SET allow_visits=EXCLUDED.allow_visits, visitor_interact=EXCLUDED.visitor_interact")) {
            p.setInt(1, islandId); p.setBoolean(2, allowVisits); p.setBoolean(3, visitorInteract); p.executeUpdate();
        }
    }

    public static boolean purchaseUpgrade(int islandId, UUID payer, int nextSize, BigDecimal price) throws SQLException {
        if (!isEnabled()) return false;
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                int updated;
                try (PreparedStatement p = c.prepareStatement("UPDATE player_economy SET balance=balance-? WHERE uuid=? AND balance>=?")) {
                    p.setBigDecimal(1, price); p.setObject(2, payer); p.setBigDecimal(3, price); updated = p.executeUpdate();
                }
                if (updated != 1) { c.rollback(); return false; }
                try (PreparedStatement p = c.prepareStatement("UPDATE island_settings SET building_size=? WHERE island_id=?")) {
                    p.setInt(1, nextSize); p.setInt(2, islandId); p.executeUpdate();
                }
                c.commit();
                return true;
            } catch (SQLException ex) { c.rollback(); throw ex; }
        }
    }

    public static void transferOwnership(int islandId, UUID oldOwner, UUID newOwner) throws SQLException {
        if (!isEnabled()) return;
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("UPDATE island_member SET role='MEMBER' WHERE island_id=? AND uuid=?")) {
                    p.setInt(1, islandId); p.setObject(2, oldOwner); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO island_member(island_id,uuid,role) VALUES (?,?, 'OWNER') " +
                        "ON CONFLICT(island_id,uuid) DO UPDATE SET role='OWNER'")) {
                    p.setInt(1, islandId); p.setObject(2, newOwner); p.executeUpdate();
                }
                c.commit();
            } catch (SQLException ex) { c.rollback(); throw ex; }
        }
    }

    public static BigDecimal getBalance(UUID uuid) throws SQLException {
        if (!isEnabled()) return BigDecimal.ZERO.setScale(2);
        try (Connection c = dataSource.getConnection(); PreparedStatement p = c.prepareStatement("SELECT balance FROM player_economy WHERE uuid=?")) {
            p.setObject(1, uuid); try (ResultSet rs = p.executeQuery()) { return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO.setScale(2); }
        }
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
        dataSource = null;
    }

    public static final class IslandMemberRow {
        public final int islandId; public final UUID uuid; public final IslandRole role;
        public IslandMemberRow(int islandId, UUID uuid, IslandRole role) { this.islandId = islandId; this.uuid = uuid; this.role = role; }
    }
    public static final class IslandSettingsRow {
        public final int islandId; public final boolean allowVisits; public final boolean visitorInteract; public final int buildingSize;
        public IslandSettingsRow(int islandId, boolean allowVisits, boolean visitorInteract, int buildingSize) {
            this.islandId = islandId; this.allowVisits = allowVisits; this.visitorInteract = visitorInteract; this.buildingSize = buildingSize;
        }
    }
}
