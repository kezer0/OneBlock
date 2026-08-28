package oneblock.gui;

import oneblock.ChestItems;
import oneblock.PlayerInfo;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.UUID;

public class GUIListener implements Listener {
    private static PlayerInfo islandInfo(Player p) {
        int id = PlayerInfo.getId(p.getUniqueId());
        return id == -1 ? null : PlayerInfo.get(id);
    }

    private static UUID parseUUID(String raw) {
        try {
            return raw == null ? null : UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String nameFromUuid(UUID uuid) {
        String name = org.bukkit.Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? uuid.toString() : name;
    }

    @EventHandler
    public void onPlayerClickInventory(final InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (!(holder instanceof GUIHolder)) return;
        e.setCancelled(true);

        HumanEntity he = e.getWhoClicked();
        if (!(he instanceof Player)) return;
        Player p = (Player) he;
        if (e.getClickedInventory() != inv) return;

        ItemStack item = e.getCurrentItem();
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        String name = meta == null || meta.getDisplayName() == null ? "" : meta.getDisplayName();
        GUIHolder gui = (GUIHolder) holder;

        switch (gui.getGuiType()) {
            case MAIN_MENU:
                switch (e.getRawSlot()) {
                    case 13:
                        GUI.phasesGUI(p);
                        return;
                    case 20:
                        GUI.phasesGUI(p);
                        return;
                    case 21:
                        p.performCommand("is join");
                        p.closeInventory();
                        return;
                    case 22:
                        GUI.membersGUI(p);
                        return;
                    case 23:
                        GUI.settingsGUI(p);
                        return;
                    case 24:
                        p.closeInventory();
                        p.performCommand("is visit");
                        return;
                    case 25:
                        GUI.topGUI(p);
                        return;
                    case 30:
                        GUI.upgradesGUI(p);
                        return;
                    case 31:
                        p.closeInventory();
                        p.performCommand("is spawn");
                        return;
                    case 49:
                        p.closeInventory();
                        return;
                    default:
                        return;
                }
            case PHASES:
                if (e.getRawSlot() == 40) GUI.openGUI(p);
                return;
            case SETTINGS:
                if (e.getRawSlot() == 11) {
                    p.closeInventory();
                    p.performCommand("is allow_visit");
                    GUI.settingsGUI(p);
                } else if (e.getRawSlot() == 15) {
                    p.closeInventory();
                    p.performCommand("is visitor_interact");
                    GUI.settingsGUI(p);
                } else if (e.getRawSlot() == 18) GUI.openGUI(p);
                else if (e.getRawSlot() == 22) p.closeInventory();
                return;
            case MEMBERS:
                if (e.getRawSlot() == 45) {
                    GUI.openGUI(p);
                    return;
                }
                if (e.getRawSlot() == 53) {
                    p.closeInventory();
                    return;
                }
                if (e.getRawSlot() == 49) {
                    GUI.inviteGUI(p);
                    return;
                }
                PlayerInfo info = islandInfo(p);
                if (info != null) {
                    int[] memberSlots = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
                    for (int index = 0; index < memberSlots.length && index < info.uuids.size(); index++) {
                        if (e.getRawSlot() == memberSlots[index]) {
                            GUI.memberGUI(p, info.uuids.get(index));
                            break;
                        }
                    }
                }
                return;
            case MEMBER:
                if (e.getRawSlot() == 18) {
                    GUI.membersGUI(p);
                    return;
                }
                if (e.getRawSlot() == 26) {
                    p.closeInventory();
                    return;
                }
                UUID member = parseUUID(gui.getContext());
                if (member == null) return;
                if (e.getRawSlot() == 11) {
                    p.closeInventory();
                    p.performCommand("is kick " + nameFromUuid(member));
                } else if (e.getRawSlot() == 15) {
                    p.closeInventory();
                    p.performCommand("is transfer " + nameFromUuid(member));
                }
                return;
            case PLAYER_SELECT:
                if (e.getRawSlot() == 45) {
                    GUI.membersGUI(p);
                    return;
                }
                if (e.getRawSlot() == 53) {
                    p.closeInventory();
                    return;
                }
                if (e.getRawSlot() >= 10 && e.getRawSlot() < 44 && !name.trim().isEmpty() && !org.bukkit.ChatColor.stripColor(name).trim().isEmpty()) {
                    String target = org.bukkit.ChatColor.stripColor(name);
                    p.closeInventory();
                    p.performCommand("is invite " + target);
                    GUI.membersGUI(p);
                }
                return;
            case UPGRADES:
                if (e.getRawSlot() == 13) {
                    p.closeInventory();
                    p.performCommand("is upgrade");
                } else if (e.getRawSlot() == 18) GUI.openGUI(p);
                else if (e.getRawSlot() == 22) p.closeInventory();
                return;
            case INVITE:
                p.closeInventory();
                if (e.getRawSlot() == 2) p.performCommand("is accept");
                return;
            case VISIT:
                if (e.getRawSlot() == 45) {
                    GUI.openGUI(p);
                    return;
                }
                if (e.getRawSlot() == 53) {
                    p.closeInventory();
                    return;
                }
                if (e.getRawSlot() >= 10 && e.getRawSlot() < 44 && !name.trim().isEmpty() && !org.bukkit.ChatColor.stripColor(name).trim().isEmpty()) {
                    String target = org.bukkit.ChatColor.stripColor(name);
                    p.closeInventory();
                    p.performCommand("is visit " + target);
                }
                return;
            case TOP:
                p.closeInventory();
                return;
            default:
        }
    }

    @EventHandler
    public void onPlayerCloseInventory(final InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (!(holder instanceof ChestHolder)) return;
        String type = ((ChestHolder) holder).getType();
        ArrayList<ItemStack> newList = new ArrayList<>();
        for (ItemStack item : inv.getContents()) if (item != null) newList.add(item);
        ChestItems.setItems(type, newList);
        ChestItems.save();
    }
}
