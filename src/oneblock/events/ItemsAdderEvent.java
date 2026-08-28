package oneblock.events;

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static oneblock.OneBlock.configManager;

public class ItemsAdderEvent implements Listener {
    @EventHandler
    public void ItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        configManager.Blockfile();
    }
}
