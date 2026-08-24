package oneblock.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GUIHolder implements InventoryHolder {
    public enum GUIType {
        MAIN_MENU,
        TOP,
        INVITE,
        VISIT,
        PHASES,
        SETTINGS,
        MEMBERS,
        MEMBER,
        PLAYER_SELECT
    }

    private final GUIType guiType;
    private final String context;

    public GUIHolder(GUIType guiType) {
        this(guiType, null);
    }

    public GUIHolder(GUIType guiType, String context) {
        this.guiType = guiType;
        this.context = context;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public GUIType getGuiType() {
        return guiType;
    }

    public String getContext() {
        return context;
    }
}
