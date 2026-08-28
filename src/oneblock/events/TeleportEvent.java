package oneblock.events;

import static oneblock.OneBlock.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import oneblock.PlayerInfo;

public class TeleportEvent implements Listener {
    private static final Set<UUID> VOID_RESCUE_DAMAGE_GUARD = ConcurrentHashMap.newKeySet();

    /** Cancels fall damage caused by the void-rescue teleport. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void cancelVoidRescueFallDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) return;
        if (event.getCause() != DamageCause.FALL) return;
        if (VOID_RESCUE_DAMAGE_GUARD.remove(entity.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void Teleport(final PlayerTeleportEvent e) {
        if (!border) return;
        Location loc = e.getTo();
        if (loc == null) return;
        World to = loc.getWorld();
        Player p = e.getPlayer();
        if (to == null || !to.equals(getWorld())) {
            p.setWorldBorder(null);
            return;
        }
        plugin.UpdateBorderLocation(p, loc);
        plugin.UpdateBorder(p);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void Respawn(final PlayerRespawnEvent e) {
        if (!border) return;
        Location loc = e.getRespawnLocation();
        Player p = e.getPlayer();
        if (loc != null && getWorld().equals(loc.getWorld())) {
            plugin.UpdateBorderLocation(p, loc);
            plugin.UpdateBorder(p);
        } else {
            p.setWorldBorder(null);
        }
    }

    @EventHandler
    public void PlayerChangedWorldEvent(PlayerChangedWorldEvent e) {
        if (!progress_bar) return;
        if (PlayerInfo.list.isEmpty()) return;
        if (e.getFrom().equals(getWorld())) PlayerInfo.removeBarFor(e.getPlayer());
    }

    /**
     * Rescue players who fall into the void. Members/owners are returned to
     * the same island location used by /is join. Guests who have no island
     * association are sent to the configured leave/spawn location instead.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void PlayerFellOfTheWorld(PlayerMoveEvent e) {
        Location to = e.getTo();
        if (to == null || getWorld() == null || !getWorld().equals(to.getWorld())) return;
        if (to.getY() > -64) return;

        Player p = e.getPlayer();
        int plID = PlayerInfo.getId(p.getUniqueId());

        // Clear the falling state before teleporting so the landing is not
        // followed by inherited downward momentum or fall damage.
        VOID_RESCUE_DAMAGE_GUARD.add(p.getUniqueId());
        p.setFallDistance(0.0F);
        p.setVelocity(new Vector(0.0D, 0.0D, 0.0D));

        if (plID == -1) {
            p.teleport(plugin.getLeave());
        } else {
            int[] result = plugin.getIslandCoordinates(plID);
            Location home = new Location(getWorld(), result[0] + 0.5, getY() + 1.2013, result[1] + 0.5,
                    p.getLocation().getYaw(), p.getLocation().getPitch());
            p.teleport(home);
        }

        // Keep the guard alive long enough for a same-tick fall-damage event,
        // but do not leave stale UUIDs around.
        Bukkit.getScheduler().runTaskLater(plugin, () -> VOID_RESCUE_DAMAGE_GUARD.remove(p.getUniqueId()), 2L);
    }
}
