package oneblock.events;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import oneblock.network.IslandDataService;
import oneblock.network.IslandPermission;
import oneblock.network.IslandRole;
public final class NetworkDataEvent implements Listener {
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true) public void onBreak(BlockBreakEvent e){Player p=e.getPlayer();int id=IslandDataService.islandAt(p.getLocation());if(id>=0&&!IslandDataService.hasPermission(p.getUniqueId(),id,IslandPermission.MODIFY_ISLAND))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true) public void onPlace(BlockPlaceEvent e){Player p=e.getPlayer();int id=IslandDataService.islandAt(e.getBlock().getLocation());if(id>=0&&!IslandDataService.hasPermission(p.getUniqueId(),id,IslandPermission.MODIFY_ISLAND))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true) public void onInteract(PlayerInteractEvent e){if(!e.hasBlock())return;Player p=e.getPlayer();int id=IslandDataService.islandAt(e.getClickedBlock().getLocation());if(id<0)return;IslandRole role=IslandDataService.getRole(p.getUniqueId(),id);if(role==IslandRole.OWNER||role==IslandRole.MEMBER)return;if(role==IslandRole.VISITOR&&IslandDataService.hasPermission(p.getUniqueId(),id,IslandPermission.INTERACT_ALLOWED))return;e.setCancelled(true);}
}
