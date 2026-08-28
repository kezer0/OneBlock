package oneblock.placement;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class PlaceOraxen extends Place {

    @Override
    public boolean setType(Block block, Object material_, boolean physics) {
        if (material_ instanceof Material)
            block.setType((Material) material_, physics);
        else if (material_ instanceof String) {
            String material = (String) material_;
            if (OraxenItems.exists(material)) {
                OraxenBlocks.place(material, block.getLocation());
                return true;
            }
        }

        return false;
    }
}