package oneblock.gui;

import static oneblock.OneBlock.*;
import static oneblock.utils.Utils.getBase64Head;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import oneblock.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.cryptomorin.xseries.XMaterial;

import oneblock.utils.Utils;

public class GUI {
    public static boolean enabled = true;
    public static boolean legacy = false;

    private static final int SIZE = 54;
    private static final int[] BORDER = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};

    public static void openGUI(Player p) {
        if (!enabled || p == null) return;
        PlayerInfo inf = islandInfo(p);
        if (inf == null || inf.uuid == null) return;

        Inventory inv = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.MAIN_MENU), SIZE,
                ChatColor.DARK_GREEN + "Island Menu");
        fillBorder(inv);

        ItemStack profile = getPlayerHead(Bukkit.getOfflinePlayer(inf.uuid),
                ChatColor.GREEN + "Island Profile");
        ItemMeta profileMeta = profile.getItemMeta();
        if (profileMeta != null) {
            profileMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Owner: " + ChatColor.WHITE + nameOf(inf.uuid),
                    ChatColor.GRAY + "Phase: " + ChatColor.GREEN + levelName(inf),
                    ChatColor.GRAY + "Progress: " + ChatColor.WHITE + inf.breaks + ChatColor.GRAY + "/" + ChatColor.WHITE + inf.getRequiredBreaks(),
                    ChatColor.GRAY + "Members: " + ChatColor.WHITE + (inf.uuids.size() + 1),
                    ChatColor.GRAY + "Visitors: " + ChatColor.WHITE + OneBlock.countVisitors(inf.uuid),
                    "",
                    ChatColor.YELLOW + "Click to view phases"
            ));
            profile.setItemMeta(profileMeta);
        }
        inv.setItem(13, profile);
        inv.setItem(19, item(XMaterial.DIAMOND_SWORD,
                ChatColor.GREEN + "Skills & Abilities",
                ChatColor.GRAY + "Your skill and abilities.",
                "", ChatColor.YELLOW + "Click to open"));
        inv.setItem(20, item(XMaterial.EXPERIENCE_BOTTLE,
                ChatColor.GREEN + "Phases",
                ChatColor.GRAY + "View every OneBlock phase and progress.",
                "", ChatColor.YELLOW + "Click to open"));
        inv.setItem(21, item(XMaterial.GRASS_BLOCK,
                ChatColor.GREEN + "Island Home",
                ChatColor.GRAY + "Teleport to the center of your OneBlock.",
                "", ChatColor.YELLOW + "Click to teleport"));
        inv.setItem(22, item(XMaterial.WRITABLE_BOOK,
                ChatColor.GREEN + "Island Members",
                ChatColor.GRAY + "View members and manage invites/kicks.",
                "", ChatColor.YELLOW + "Click to manage"));
        inv.setItem(23, item(XMaterial.CLOCK,
                ChatColor.GREEN + "Island Settings",
                ChatColor.GRAY + "Configure visitor access for your island.",
                "", ChatColor.YELLOW + "Click to open"));
        inv.setItem(24, item(XMaterial.ENDER_PEARL,
                ChatColor.GREEN + "Visit Islands",
                ChatColor.GRAY + "Browse islands that allow visitors.",
                "", ChatColor.YELLOW + "Click to browse"));
        inv.setItem(25, item(XMaterial.NETHER_STAR,
                ChatColor.GREEN + "Top Islands",
                ChatColor.GRAY + "View the island leaderboard.",
                "", ChatColor.YELLOW + "Click to open"));

        ItemStack spawn = getBase64Head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTU3N2M0ZGUxZjUxYTcwNzIyMDIzZTg1NmI1NDNjZDU3MGYxZDBlZTZiOWQxNjdiNTkwMjhjZTFiYzkyZTQ1OCJ9fX0=");
        ItemMeta spawnMeta = spawn.getItemMeta();
        if (spawnMeta != null) {
            spawnMeta.setDisplayName(ChatColor.AQUA + "Fast Travel");
            spawnMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Return to the OneBlock spawn.",
                    ChatColor.GRAY + "Uses your configured leave location.",
                    "",
                    ChatColor.YELLOW + "Click to travel"
            ));
            spawn.setItemMeta(spawnMeta);
        }
        inv.setItem(40, spawn);

        inv.setItem(49, item(XMaterial.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close this menu."));
        p.openInventory(inv);
    }

    public static void skillGUI(Player p){
        if (!enabled || p == null) return;
        p.performCommand("skill");
    }

    public static void phasesGUI(Player p) {
        if (!enabled || p == null) return;
        PlayerInfo inf = islandInfo(p);
        if (inf == null) return;

        Inventory inv = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.PHASES), 45,
                ChatColor.DARK_GREEN + "Island Phases");
        fillBorder45(inv);
        int max = Math.min(Level.size(), 27);
        int[] phaseSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42};
        for (int i = 0; i < max; i++) {
            Level level = Level.get(i);
            ChatColor color = i < inf.lvl ? ChatColor.GREEN : (i == inf.lvl ? ChatColor.YELLOW : ChatColor.GRAY);
            String state = i < inf.lvl ? "Completed" : (i == inf.lvl ? "Current" : "Locked");
            inv.setItem(phaseSlots[i], item(XMaterial.BOOK,
                    color + level.name,
                    ChatColor.GRAY + "Phase: " + ChatColor.WHITE + i,
                    ChatColor.GRAY + "Length: " + ChatColor.WHITE + level.length,
                    "",
                    color + state));
        }
        inv.setItem(40, item(XMaterial.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to the island menu."));
        p.openInventory(inv);
    }

    public static void settingsGUI(Player p) {
        if (!enabled || p == null) return;
        PlayerInfo inf = islandInfo(p);
        if (inf == null) return;
        boolean owner = p.getUniqueId().equals(inf.uuid);

        Inventory inv = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.SETTINGS), 27,
                ChatColor.DARK_GREEN + "Island Settings");
        fillBorder27(inv);
        String visitorState = inf.allowVisit ? ChatColor.GREEN + "PUBLIC" : ChatColor.RED + "PRIVATE";
        inv.setItem(13, item(inf.allowVisit ? XMaterial.EMERALD : XMaterial.REDSTONE,
                ChatColor.GREEN + "Visitor Access",
                ChatColor.GRAY + "Current: " + visitorState,
                ChatColor.GRAY + "Allow other players to visit your island.",
                "",
                owner && p.hasPermission("oneblock.allow_visit")
                        ? ChatColor.YELLOW + "Click to toggle"
                        : ChatColor.RED + "Owner permission required"));
        inv.setItem(18, item(XMaterial.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to the island menu."));
        inv.setItem(22, item(XMaterial.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close this menu."));
        p.openInventory(inv);
    }

    public static void membersGUI(Player p) {
        if (!enabled || p == null) return;
        PlayerInfo inf = islandInfo(p);
        if (inf == null) return;
        boolean owner = p.getUniqueId().equals(inf.uuid);

        Inventory inv = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.MEMBERS), SIZE,
                ChatColor.DARK_GREEN + "Island Members");
        fillBorder(inv);

        ItemStack ownerHead = getPlayerHead(Bukkit.getOfflinePlayer(inf.uuid), ChatColor.GOLD + nameOf(inf.uuid));
        ItemMeta om = ownerHead.getItemMeta();
        if (om != null) {
            om.setLore(Arrays.asList(ChatColor.YELLOW + "Island Owner", ChatColor.GRAY + "Full island access"));
            ownerHead.setItemMeta(om);
        }
        inv.setItem(13, ownerHead);

        int[] memberSlots = {28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        int memberIndex = 0;
        for (UUID uuid : inf.uuids) {
            if (memberIndex >= memberSlots.length) break;
            int slot = memberSlots[memberIndex++];
            ItemStack member = getPlayerHead(Bukkit.getOfflinePlayer(uuid), ChatColor.GREEN + nameOf(uuid));
            ItemMeta mm = member.getItemMeta();
            if (mm != null) {
                mm.setLore(Arrays.asList(
                        ChatColor.GRAY + "Island Member",
                        ChatColor.GRAY + "Click for member options"
                ));
                member.setItemMeta(mm);
            }
            inv.setItem(slot++, member);
        }

        if (owner && p.hasPermission("oneblock.invite")) {
            inv.setItem(49, item(XMaterial.EMERALD, ChatColor.GREEN + "Invite Player",
                    ChatColor.GRAY + "Choose an online player to invite."));
        }
        inv.setItem(45, item(XMaterial.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to the island menu."));
        inv.setItem(53, item(XMaterial.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close this menu."));
        p.openInventory(inv);
    }

    public static void memberGUI(Player p, UUID memberUuid) {
        if (!enabled || p == null || memberUuid == null) return;
        PlayerInfo inf = islandInfo(p);
        if (inf == null || !inf.uuids.contains(memberUuid)) return;
        boolean owner = p.getUniqueId().equals(inf.uuid);

        Inventory inv = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.MEMBER, memberUuid.toString()), 27,
                ChatColor.DARK_GREEN + "Member: " + nameOf(memberUuid));
        fillBorder27(inv);

        ItemStack head = getPlayerHead(Bukkit.getOfflinePlayer(memberUuid), ChatColor.GREEN + nameOf(memberUuid));
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Island Member",
                    "",
                    ChatColor.GRAY + "Invite permission: " + permissionState(memberUuid, "oneblock.invite"),
                    ChatColor.GRAY + "Kick permission: " + permissionState(memberUuid, "oneblock.kick"),
                    "",
                    ChatColor.DARK_GRAY + "Server permissions control these actions."
            ));
            head.setItemMeta(meta);
        }
        inv.setItem(13, head);

        if (owner && p.hasPermission("oneblock.kick")) {
            inv.setItem(11, item(XMaterial.REDSTONE_BLOCK, ChatColor.RED + "Kick Member",
                    ChatColor.GRAY + "Remove this player from the island."));
        }
        inv.setItem(15, item(XMaterial.ENDER_PEARL, ChatColor.AQUA + "Visit Island",
                ChatColor.GRAY + "Travel to your own island while managing members."));
        inv.setItem(18, item(XMaterial.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to members."));
        inv.setItem(26, item(XMaterial.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close this menu."));
        p.openInventory(inv);
    }

    public static void inviteGUI(Player p) {
        if (!enabled || p == null) return;
        Inventory inv = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.PLAYER_SELECT), SIZE,
                ChatColor.DARK_GREEN + "Invite Player");
        fillBorder(inv);
        PlayerInfo inf = islandInfo(p);
        int[] playerSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        int playerIndex = 0;
        if (inf != null) {
            for (Player candidate : Bukkit.getOnlinePlayers()) {
                if (candidate.getUniqueId().equals(p.getUniqueId()) || inf.uuids.contains(candidate.getUniqueId())) continue;
                if (playerIndex >= playerSlots.length) break;
                int slot = playerSlots[playerIndex++];
                ItemStack head = getPlayerHead(candidate, ChatColor.GREEN + candidate.getName());
                ItemMeta meta = head.getItemMeta();
                if (meta != null) {
                    meta.setLore(Arrays.asList(ChatColor.GRAY + "Click to invite this player."));
                    head.setItemMeta(meta);
                }
                inv.setItem(slot++, head);
            }
        }
        inv.setItem(45, item(XMaterial.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to members."));
        inv.setItem(53, item(XMaterial.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close this menu."));
        p.openInventory(inv);
    }

    public static void acceptGUI(Player p, String name) {
        if (!enabled || p == null) return;
        Inventory inv = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.INVITE), 9, Messages.acceptGUI);
        inv.setItem(6, item(XMaterial.REDSTONE_BLOCK, Messages.acceptGUIignore));
        inv.setItem(2, item(XMaterial.EMERALD_BLOCK, String.format(Messages.acceptGUIjoin, name), Messages.idresetGUI));
        p.openInventory(inv);
    }

    public static void topGUI(Player p) {
        if (!enabled || p == null) return;
        Inventory inv = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.TOP), 27, Messages.topGUI);
        List<PlayerInfo> toplist = oneblock.OneBlock.getTopList();
        setTop(inv, 4, 0, XMaterial.NETHERITE_BLOCK, ChatColor.GOLD + "1st");
        setTop(inv, 12, 1, XMaterial.DIAMOND_BLOCK, ChatColor.GRAY + "2nd");
        setTop(inv, 14, 2, XMaterial.IRON_BLOCK, ChatColor.GRAY + "3rd");
        setTop(inv, 20, 3, XMaterial.GOLD_BLOCK, ChatColor.DARK_RED + "4th");
        setTop(inv, 22, 4, XMaterial.COPPER_BLOCK, ChatColor.DARK_RED + "5th");
        setTop(inv, 24, 5, XMaterial.COAL_BLOCK, ChatColor.DARK_RED + "6th");
        p.openInventory(inv);
    }

    public static void visitGUI(Player p, OfflinePlayer[] offlinePlayers) {
        if (!enabled || p == null) return;
        Inventory inv = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.VISIT), SIZE, Messages.visitGUI);
        ArrayList<OfflinePlayer> matched = new ArrayList<>();
        for (OfflinePlayer pl : offlinePlayers) {
            if (pl == null || pl.getUniqueId().equals(p.getUniqueId())) continue;
            PlayerInfo info = PlayerInfo.get(pl.getUniqueId());
            if (info == null || info.uuid == null || !info.allowVisit) continue;
            matched.add(pl);
        }
        int[] visitSlots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        int visitIndex = 0;
        for (OfflinePlayer pl : matched) {
            if (visitIndex >= visitSlots.length) break;
            int slot = visitSlots[visitIndex++];
            ItemStack head = getPlayerHead(pl, ChatColor.AQUA + nameOf(pl.getUniqueId()));
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setLore(Arrays.asList(ChatColor.GRAY + "Island is open for visitors.", "", ChatColor.YELLOW + "Click to visit"));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }
        inv.setItem(45, item(XMaterial.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to the island menu."));
        inv.setItem(53, item(XMaterial.BARRIER, ChatColor.RED + "Close", ChatColor.GRAY + "Close this menu."));
        p.openInventory(inv);
    }

    public static ItemStack getPlayerHead(OfflinePlayer player, String title) {
        ItemStack skull = XMaterial.PLAYER_HEAD.parseItem();
        if (skull == null) skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            if (!legacy && player != null) meta.setOwningPlayer(player);
            meta.setDisplayName(title);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    public static void chestGUI(Player p, String chestType) {
        if (p == null) return;
        List<ItemStack> list = ChestItems.getItems(chestType);
        if (list == null) return;
        Inventory inv = Bukkit.createInventory(new ChestHolder(chestType), 54,
                String.format("%sEdit: %s%s", ChatColor.BLACK, ChatColor.DARK_GRAY, chestType));
        for (ItemStack itm : list) if (itm != null) inv.addItem(itm);
        p.openInventory(inv);
    }

    private static PlayerInfo islandInfo(Player p) {
        int id = PlayerInfo.getId(p.getUniqueId());
        return id == -1 ? null : PlayerInfo.get(id);
    }

    private static String levelName(PlayerInfo info) {
        return Level.get(info.lvl).name;
    }

    private static String nameOf(UUID uuid) {
        try {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            return name == null ? "Unknown" : name;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private static String yesNo(boolean value) {
        return value ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No";
    }

    private static String permissionState(UUID uuid, String permission) {
        Player online = Bukkit.getPlayer(uuid);
        return online == null ? ChatColor.GRAY + "Offline" : yesNo(online.hasPermission(permission));
    }

    private static ItemStack item(XMaterial material, String title, String... lore) {
        Material m = material.get();
        ItemStack stack = new ItemStack(m == null ? Material.EMERALD_BLOCK : m);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(title);
            meta.setLore(Arrays.asList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack pane = item(XMaterial.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot : BORDER) inv.setItem(slot, pane);
    }

    private static void fillBorder45(Inventory inv) {
        ItemStack pane = item(XMaterial.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 36; i < 45; i++) inv.setItem(i, pane);
    }

    private static void fillBorder27(Inventory inv) {
        ItemStack pane = item(XMaterial.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);
        for (int i = 18; i < 27; i++) inv.setItem(i, pane);
        inv.setItem(9, pane); inv.setItem(17, pane);
    }

    private static void setTop(Inventory inv, int slot, int index, XMaterial material, String prefix) {
        PlayerInfo info = oneblock.OneBlock.getTop(index);
        if (info == PlayerInfo.not_found || info.uuid == null) return;
        inv.setItem(slot, item(material, prefix + " - " + nameOf(info.uuid),
                ChatColor.GRAY + "Level: " + ChatColor.WHITE + info.lvl,
                ChatColor.GRAY + "Members: " + ChatColor.WHITE + (info.uuids.size() + 1)));
    }
}
