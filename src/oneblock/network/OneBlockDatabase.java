package oneblock.network;

import me.kezer0.networkCore.api.DatabaseService;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import oneblock.OneBlock;

/** OneBlock-owned repository using NetworkCore's shared PostgreSQL pool. */
public final class OneBlockDatabase {
    public static final class IslandSettingsRow {
        public final int islandId;
        public final boolean allowVisits;
        public final boolean visitorInteract;
        public final int buildingSize;

        public IslandSettingsRow(int islandId, boolean allowVisits, boolean visitorInteract, int buildingSize) {
            this.islandId = islandId;
            this.allowVisits = allowVisits;
            this.visitorInteract = visitorInteract;
            this.buildingSize = buildingSize;
        }
    }

    public static final class IslandMemberRow {
        public final int islandId;
        public final UUID uuid;
        public final IslandRole role;

        public IslandMemberRow(int islandId, UUID uuid, IslandRole role) {
            this.islandId = islandId;
            this.uuid = uuid;
            this.role = role;
        }
    }

    private static DatabaseService database;
    private static volatile boolean enabled;

    private OneBlockDatabase() {}

    public static void initialize(OneBlock plugin) {
        org.bukkit.plugin.RegisteredServiceProvider<DatabaseService> registration = Bukkit.getServicesManager().getRegistration(DatabaseService.class);
        if (registration == null || registration.getProvider() == null) {
            plugin.getLogger().severe("NetworkCore DatabaseService was not found. OneBlock database persistence is disabled.");
            enabled = false;
            return;
        }

        database = registration.getProvider();
        if (!database.isAvailable()) {
            plugin.getLogger().severe("NetworkCore PostgreSQL service is unavailable. OneBlock database persistence is disabled.");
            enabled = false;
            return;
        }

        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS oneblock");
            statement.execute("CREATE TABLE IF NOT EXISTS oneblock.islands (" +
                    "island_id INTEGER PRIMARY KEY," +
                    "owner_uuid UUID NOT NULL," +
                    "allow_visits BOOLEAN NOT NULL DEFAULT FALSE," +
                    "visitor_interact BOOLEAN NOT NULL DEFAULT FALSE," +
                    "building_size INTEGER NOT NULL DEFAULT 50," +
                    "created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            statement.execute("CREATE TABLE IF NOT EXISTS oneblock.island_members (" +
                    "island_id INTEGER NOT NULL REFERENCES oneblock.islands(island_id) ON DELETE CASCADE," +
                    "player_uuid UUID NOT NULL," +
                    "role VARCHAR(16) NOT NULL CHECK (role IN ('OWNER','MEMBER'))," +
                    "PRIMARY KEY (island_id, player_uuid)" +
                    ")");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_oneblock_island_members_player ON oneblock.island_members(player_uuid)");
            migrateLegacyPublicTables(statement);
            enabled = true;
            plugin.getLogger().info("OneBlock connected to NetworkCore PostgreSQL (schema: oneblock).");
        } catch (SQLException ex) {
            enabled = false;
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to initialize OneBlock PostgreSQL schema", ex);
        }
    }

    private static void migrateLegacyPublicTables(Statement statement) throws SQLException {
        statement.execute("DO $$ BEGIN " +
                "IF to_regclass('public.island_settings') IS NOT NULL AND to_regclass('public.island_member') IS NOT NULL THEN " +
                "INSERT INTO oneblock.islands(island_id,owner_uuid,allow_visits,visitor_interact,building_size) " +
                "SELECT ss.island_id,m.uuid,ss.allow_visits,ss.visitor_interact,ss.building_size " +
                "FROM public.island_settings ss JOIN public.island_member m ON m.island_id=ss.island_id AND m.role='OWNER' " +
                "ON CONFLICT(island_id) DO UPDATE SET owner_uuid=EXCLUDED.owner_uuid,allow_visits=EXCLUDED.allow_visits,visitor_interact=EXCLUDED.visitor_interact,building_size=EXCLUDED.building_size; " +
                "END IF; END $$");
        statement.execute("DO $$ BEGIN " +
                "IF to_regclass('public.island_member') IS NOT NULL THEN " +
                "INSERT INTO oneblock.island_members(island_id,player_uuid,role) " +
                "SELECT island_id,uuid,role FROM public.island_member WHERE role IN ('OWNER','MEMBER') " +
                "ON CONFLICT(island_id,player_uuid) DO UPDATE SET role=EXCLUDED.role; " +
                "END IF; END $$");
    }

    public static boolean isEnabled() {
        return enabled && database != null && database.isAvailable();
    }

    private static Connection connection() throws SQLException {
        if (!isEnabled()) throw new SQLException("NetworkCore DatabaseService unavailable");
        return database.getConnection();
    }

    public static List<IslandSettingsRow> loadIslandSettings() throws SQLException {
        List<IslandSettingsRow> rows = new ArrayList<>();
        try (Connection c = connection();
             PreparedStatement p = c.prepareStatement("SELECT island_id,allow_visits,visitor_interact,building_size FROM oneblock.islands");
             ResultSet r = p.executeQuery()) {
            while (r.next()) rows.add(new IslandSettingsRow(r.getInt(1), r.getBoolean(2), r.getBoolean(3), r.getInt(4)));
        }
        return rows;
    }

    public static List<IslandMemberRow> loadIslandMembers() throws SQLException {
        List<IslandMemberRow> rows = new ArrayList<>();
        try (Connection c = connection();
             PreparedStatement p = c.prepareStatement("SELECT island_id,player_uuid,role FROM oneblock.island_members");
             ResultSet r = p.executeQuery()) {
            while (r.next()) {
                try {
                    rows.add(new IslandMemberRow(r.getInt(1), r.getObject(2, UUID.class), IslandRole.valueOf(r.getString(3))));
                } catch (IllegalArgumentException ignored) {
                    // Ignore a role added by a newer version rather than preventing startup.
                }
            }
        }
        return rows;
    }

    public static void ensureIsland(int islandId, UUID owner, boolean allowVisits, boolean visitorInteract, int buildingSize) throws SQLException {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO oneblock.islands(island_id,owner_uuid,allow_visits,visitor_interact,building_size) VALUES(?,?,?,?,?) " +
                    "ON CONFLICT(island_id) DO UPDATE SET owner_uuid=EXCLUDED.owner_uuid,updated_at=CURRENT_TIMESTAMP")) {
                p.setInt(1, islandId);
                p.setObject(2, owner);
                p.setBoolean(3, allowVisits);
                p.setBoolean(4, visitorInteract);
                p.setInt(5, buildingSize);
                p.executeUpdate();
            }
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO oneblock.island_members(island_id,player_uuid,role) VALUES(?,?, 'OWNER') " +
                    "ON CONFLICT(island_id,player_uuid) DO UPDATE SET role='OWNER'")) {
                p.setInt(1, islandId);
                p.setObject(2, owner);
                p.executeUpdate();
            }
            c.commit();
        }
    }

    public static void setIslandMember(int islandId, UUID uuid, IslandRole role) throws SQLException {
        if (role == IslandRole.VISITOR) return;
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO oneblock.island_members(island_id,player_uuid,role) VALUES(?,?,?) " +
                "ON CONFLICT(island_id,player_uuid) DO UPDATE SET role=EXCLUDED.role")) {
            p.setInt(1, islandId);
            p.setObject(2, uuid);
            p.setString(3, role.name());
            p.executeUpdate();
        }
    }

    public static void removeIslandMember(int islandId, UUID uuid) throws SQLException {
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(
                "DELETE FROM oneblock.island_members WHERE island_id=? AND player_uuid=? AND role='MEMBER'")) {
            p.setInt(1, islandId);
            p.setObject(2, uuid);
            p.executeUpdate();
        }
    }

    public static void setIslandSettings(int islandId, boolean allowVisits, boolean visitorInteract, int buildingSize) throws SQLException {
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(
                "UPDATE oneblock.islands SET allow_visits=?,visitor_interact=?,building_size=?,updated_at=CURRENT_TIMESTAMP WHERE island_id=?")) {
            p.setBoolean(1, allowVisits);
            p.setBoolean(2, visitorInteract);
            p.setInt(3, buildingSize);
            p.setInt(4, islandId);
            p.executeUpdate();
        }
    }

    public static void updateBuildingSize(int islandId, int buildingSize) throws SQLException {
        try (Connection c = connection(); PreparedStatement p = c.prepareStatement(
                "UPDATE oneblock.islands SET building_size=?,updated_at=CURRENT_TIMESTAMP WHERE island_id=?")) {
            p.setInt(1, buildingSize);
            p.setInt(2, islandId);
            if (p.executeUpdate() != 1) throw new SQLException("Island does not exist");
        }
    }

    public static void transferOwnership(int islandId, UUID oldOwner, UUID newOwner) throws SQLException {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try (PreparedStatement p1 = c.prepareStatement(
                    "UPDATE oneblock.islands SET owner_uuid=?,updated_at=CURRENT_TIMESTAMP WHERE island_id=?");
                 PreparedStatement p2 = c.prepareStatement(
                    "UPDATE oneblock.island_members SET role='MEMBER' WHERE island_id=? AND player_uuid=?");
                 PreparedStatement p3 = c.prepareStatement(
                    "INSERT INTO oneblock.island_members(island_id,player_uuid,role) VALUES(?,?, 'OWNER') " +
                    "ON CONFLICT(island_id,player_uuid) DO UPDATE SET role='OWNER'")) {
                p1.setObject(1, newOwner);
                p1.setInt(2, islandId);
                if (p1.executeUpdate() != 1) throw new SQLException("Island does not exist");
                p2.setInt(1, islandId);
                p2.setObject(2, oldOwner);
                p2.executeUpdate();
                p3.setInt(1, islandId);
                p3.setObject(2, newOwner);
                p3.executeUpdate();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
}
