package oneblock.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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

import oneblock.ChestItems;
import oneblock.Messages;
import oneblock.PlayerInfo;
import oneblock.utils.Utils;
import oneblock.worldguard.OBWorldGuard;

import static oneblock.utils.Utils.getBase64Head;

public class GUI {
	public static boolean enabled = true;
	public static boolean legacy = false;
	
	static Inventory topGUI = null;

	public static void openGUI(Player p) {
		if (!enabled || p == null) return;

		// Full 54-slot (6-row) inventory
		Inventory mainGUI = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.MAIN_MENU), 54, ChatColor.DARK_GRAY + "Your Island");

		// Fill entire GUI with Gray Stained Glass Panes
		ItemStack borderPane = setMeta(XMaterial.GRAY_STAINED_GLASS_PANE, " ");
		for (int i = 0; i < 54; i++) {
			mainGUI.setItem(i, borderPane);
		}

		// --- ROW 2: Player Stats Head (Slot 13) ---
		ItemStack playerHead = getPlayerHead(p, ChatColor.GREEN + p.getName() + "'s Island");
		ItemMeta headMeta = playerHead.getItemMeta();
		if (headMeta != null) {
			headMeta.setLore(Arrays.asList(
					ChatColor.GRAY + "View and manage your core island",
					ChatColor.GRAY + "progression and statistics.",
					"",
					ChatColor.GRAY + "Phase: " + ChatColor.GREEN + "1",
					ChatColor.GRAY + "Blocks Mined: " + ChatColor.GREEN + "37 / 100",
					ChatColor.GRAY + "Mining Level: " + ChatColor.GREEN + "4",
					ChatColor.GRAY + "Balance: " + ChatColor.GOLD + "$1,250",
					"",
					ChatColor.YELLOW + "Click to view profile!"
			));
			playerHead.setItemMeta(headMeta);
		}
		mainGUI.setItem(13, playerHead);

		// --- ROW 3: Core Feature Grid ---

		// Upgrades (Slot 19)
		mainGUI.setItem(19, setMeta(XMaterial.DIAMOND_SWORD,
				ChatColor.GREEN + "Island Upgrades", 1,
				ChatColor.GRAY + "Enhance your island limits, generator",
				ChatColor.GRAY + "speed, and team sizes.",
				"",
				ChatColor.YELLOW + "Click to view!"
		));

		// Quests (Slot 20)
		mainGUI.setItem(20, setMeta(XMaterial.ITEM_FRAME,
				ChatColor.GREEN + "Quests & Chapters", 1,
				ChatColor.GRAY + "Each island has its own series of",
				ChatColor.GRAY + "tasks for you to complete!",
				"",
				ChatColor.GRAY + "Complete tasks to earn small " + ChatColor.GOLD + "rewards" + ChatColor.GRAY + ",",
				ChatColor.GRAY + "or complete entire chapters for big ones!",
				"",
				ChatColor.RED + "Coming soon!"
		));

		// Skills (Slot 21)
		mainGUI.setItem(21, setMeta(XMaterial.BOOK,
				ChatColor.GREEN + "Skills & Levels", 1,
				ChatColor.GRAY + "Level up your skill trees to unlock",
				ChatColor.GRAY + "passive buffs and unique perks.",
				"",
				ChatColor.RED + "Coming soon!"
		));

		// Teleport / Home (Slot 22)
		mainGUI.setItem(22, setMeta(XMaterial.GRASS_BLOCK,
				ChatColor.GREEN + "Island Home", 1,
				ChatColor.GRAY + "Teleport directly back to your primary",
				ChatColor.GRAY + "island spawn location.",
				"",
				ChatColor.YELLOW + "Click to teleport!"
		));

		// Members (Slot 23)
		mainGUI.setItem(23, setMeta(XMaterial.WRITABLE_BOOK,
				ChatColor.GREEN + "Island Team", 1,
				ChatColor.GRAY + "Manage island co-op members,",
				ChatColor.GRAY + "invites, and role permissions.",
				"",
				ChatColor.RED + "Coming soon!"
		));

		// Settings (Slot 24)
		mainGUI.setItem(24, setMeta(XMaterial.CLOCK,
				ChatColor.GREEN + "Island Settings", 1,
				ChatColor.GRAY + "Configure visitor rules, PvP states,",
				ChatColor.GRAY + "and island privacy modes.",
				"",
				ChatColor.RED + "Coming soon!"
		));

		// Top Leaderboard (Slot 25)
		mainGUI.setItem(25, setMeta(XMaterial.CHEST,
				ChatColor.GREEN + "Top Islands", 1,
				ChatColor.GRAY + "Check out the highest-ranking islands",
				ChatColor.GRAY + "across the entire server.",
				"",
				ChatColor.YELLOW + "Click to open!"
		));

		// --- ROW 4: Secondary Tools ---

		// Warp / Visit (Slot 31)
		mainGUI.setItem(31, setMeta(XMaterial.CRAFTING_TABLE,
				ChatColor.GREEN + "Visit Islands", 1,
				ChatColor.GRAY + "Explore and visit open public islands",
				ChatColor.GRAY + "created by other players.",
				"",
				ChatColor.YELLOW + "Click to browse!"
		));

		ItemStack earthHead = getBase64Head("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTU3N2M0ZGUxZjUxYTcwNzIyMDIzZTg1NmI1NDNjZDU3MGYxZDBlZTZiOWQxNjdiNTkwMjhjZTFiYzkyZTQ1OCJ9fX0=");
		ItemMeta earthMeta = earthHead.getItemMeta();
		if (earthMeta != null) {
			earthMeta.setItemName(ChatColor.AQUA + "Travel");
			earthMeta.setLore(Arrays.asList(
					ChatColor.GRAY + "Teleport to diffrent places",
					ChatColor.GRAY + "you have unlocked.",
					ChatColor.YELLOW + "Click to pick location!"
			));
			playerHead.setItemMeta(headMeta);
		}
		mainGUI.setItem(48, earthHead);
		// Close Menu (Slot 49)
		mainGUI.setItem(49, setMeta(XMaterial.BARRIER,
				ChatColor.RED + "Close", 1,
				ChatColor.GRAY + "Close this menu."
		));
		mainGUI.setItem(50, setMeta(XMaterial.COOKIE,
				ChatColor.GOLD + "Shop", 1,
				ChatColor.GRAY + "Open you the server shop."
		));
		p.openInventory(mainGUI);
	}
	
	public static void acceptGUI(Player p, String name) {
		if (!enabled) return;
		if (p == null) return;
		Inventory acceptGUI = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.INVITE), 9, Messages.acceptGUI);
		acceptGUI.setItem(6, setMeta(XMaterial.REDSTONE_BLOCK, Messages.acceptGUIignore));
		acceptGUI.setItem(2, setMeta(XMaterial.EMERALD_BLOCK, String.format(Messages.acceptGUIjoin, name), Messages.idresetGUI));
        p.openInventory(acceptGUI);
	}
	
	public static void topGUI(Player p) {
		if (!enabled) return;
		if (p == null) return;
		if (topGUI == null)
			topGUI = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.TOP), 27, Messages.topGUI);
		
		List<PlayerInfo> toplist = oneblock.OneBlock.getTopList();
		
		PlayerInfo inf = oneblock.OneBlock.getTop(0, toplist);
		topGUI.setItem(4, setMeta(XMaterial.NETHERITE_BLOCK, ChatColor.GOLD + "1st - " + parseUUID(inf.uuid), inf.lvl, parseUUIDs(inf.uuids)));
		inf = oneblock.OneBlock.getTop(1, toplist);
		topGUI.setItem(12, setMeta(XMaterial.DIAMOND_BLOCK, ChatColor.GRAY + "2nd - " + parseUUID(inf.uuid), inf.lvl, parseUUIDs(inf.uuids)));
		inf = oneblock.OneBlock.getTop(2, toplist);
		topGUI.setItem(14, setMeta(XMaterial.IRON_BLOCK, ChatColor.GRAY + "3rd - " + parseUUID(inf.uuid), inf.lvl, parseUUIDs(inf.uuids)));
		inf = oneblock.OneBlock.getTop(3, toplist);
		topGUI.setItem(20, setMeta(XMaterial.GOLD_BLOCK, ChatColor.DARK_RED + "4th - " + parseUUID(inf.uuid), inf.lvl, parseUUIDs(inf.uuids)));
		inf = oneblock.OneBlock.getTop(4, toplist);
		topGUI.setItem(22, setMeta(XMaterial.COPPER_BLOCK, ChatColor.DARK_RED + "5th - " + parseUUID(inf.uuid), inf.lvl, parseUUIDs(inf.uuids)));
		inf = oneblock.OneBlock.getTop(5, toplist);
		topGUI.setItem(24, setMeta(XMaterial.COAL_BLOCK, ChatColor.DARK_RED + "6th - " + parseUUID(inf.uuid), inf.lvl, parseUUIDs(inf.uuids)));
        p.openInventory(topGUI);
	}
	
	public static void visitGUI(Player p, OfflinePlayer[] offlinePlayers) {
		if (!enabled) return;
		if (p == null) return;
		Inventory visitGUI = Bukkit.createInventory(new GUIHolder(GUIHolder.GUIType.VISIT), 54, Messages.visitGUI);
		ArrayList<OfflinePlayer> matchedPlayers = new ArrayList<>();
		for (OfflinePlayer pl: offlinePlayers) {
			PlayerInfo inf = PlayerInfo.get(pl.getUniqueId());
			if (inf == null) continue;
			if (!inf.allowVisit) continue;
			matchedPlayers.add(pl);
		}
		int size = Math.min(matchedPlayers.size(), 54);
		for (int i = 0; i < size; i++) {
			OfflinePlayer pl = matchedPlayers.get(i);
			visitGUI.setItem(i, getPlayerHead(pl, pl.getName() != null ? pl.getName() : "Unknown"));
		}
        p.openInventory(visitGUI);
	}
	
	public static ItemStack getPlayerHead(OfflinePlayer player, String title) {
        ItemStack skull = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (!legacy) skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName(title);
        skull.setItemMeta(skullMeta);
        return skull;
    }
	
	/**
	 * Opens a GUI displaying the contents of a legacy chest.
	 *
	 * @param p         the player opening the GUI
	 * @param chestType the chest alias name (must exist in legacy storage)
	 */
	public static void chestGUI(Player p, String chestType) {
		if (p == null) return;
		List<ItemStack> list = ChestItems.getItems(chestType);
		if (list == null) return;
		
		Inventory chestGUI = Bukkit.createInventory(new ChestHolder(chestType), 54, String.format("%sEdit: %s%s", ChatColor.BLACK, ChatColor.DARK_GRAY, chestType));
		
		for(ItemStack itm : list)
			if (itm != null)
				chestGUI.addItem(itm);
		p.openInventory(chestGUI);
	}

	/**
	 * Render a UUID as a display name for GUI lore. Intentionally swallows any
	 * exception from {@link Bukkit#getOfflinePlayer(UUID)} (can NPE on null
	 * input, and some server forks throw on unresolvable UUIDs). Returning
	 * {@code "Unknown"} is the correct UX for a stale/missing invitee entry;
	 * this runs in the hot GUI-render path, so no log is emitted.
	 */
	private static String parseUUID(UUID uuid) {
		try { return Bukkit.getOfflinePlayer(uuid).getName();
		} catch (Exception e) {return "Unknown";}
	}
	
	private static String[] parseUUIDs(List<UUID> uuids) {
		String[] Lore = new String[uuids.size()];
		for (int i = 0; i < uuids.size(); i++)
			Lore[i] = parseUUID(uuids.get(i));
		return Lore;
	}
	
	private static ItemStack setMeta(XMaterial material, String title) {
		return setMeta(material, title, 1);
	}
	
	private static ItemStack setMeta(XMaterial material, String title, String ...Lore) {
		return setMeta(material, title, 1, Lore);
	}
	
	private static ItemStack setMeta(XMaterial material, String title, int amount, String ...Lore) {
		if (amount <= 0) amount = 1;
		Material m = material.get();
		ItemStack join = new ItemStack(m == null ? Material.EMERALD_BLOCK : m, amount);
        ItemMeta meta = join.getItemMeta();
        meta.setDisplayName(title);
	    meta.setLore(Arrays.asList(Lore));
        join.setItemMeta(meta);
		return join;
	}
}