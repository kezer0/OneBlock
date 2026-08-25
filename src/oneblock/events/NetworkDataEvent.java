package oneblock.events;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import oneblock.OneBlock;
import oneblock.network.IslandDataService;
import oneblock.network.IslandPermission;
import oneblock.network.IslandRole;
import oneblock.network.PlayerDataManager;

public final class NetworkDataEvent implements Listener {
    @EventHandler
    public void onJoin(PlayerJoinEvent event) { PlayerDataManager.handleJoin(event.getPlayer()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) { PlayerDataManager.handleQuit(event.getPlayer()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        int islandId = IslandDataService.islandAt(player.getLocation());
        if (islandId < 0) return;
        if (!IslandDataService.hasPermission(player.getUniqueId(), islandId, IslandPermission.MODIFY_ISLAND)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        int islandId = IslandDataService.islandAt(event.getBlock().getLocation());
        if (islandId < 0) return;
        if (!IslandDataService.hasPermission(player.getUniqueId(), islandId, IslandPermission.MODIFY_ISLAND)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!event.hasBlock()) return;
        Player player = event.getPlayer();
        int islandId = IslandDataService.islandAt(event.getClickedBlock().getLocation());
        if (islandId < 0) return;
        IslandRole role = IslandDataService.getRole(player.getUniqueId(), islandId);
        if (role == IslandRole.OWNER || role == IslandRole.MEMBER) return;
        if (role == IslandRole.VISITOR && IslandDataService.hasPermission(player.getUniqueId(), islandId, IslandPermission.INTERACT_ALLOWED)) return;
        event.setCancelled(true);
    }
}
