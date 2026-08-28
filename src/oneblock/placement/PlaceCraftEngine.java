package oneblock.placement;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class PlaceCraftEngine extends Place {

    @Override
    public boolean setType(Block block, Object material_, boolean physics) {
        if (material_ instanceof Material)
            block.setType((Material) material_, physics);
        else if (material_ instanceof Key) {
            Key key = (Key) material_;
            return CraftEngineBlocks.place(block.getLocation(), key, false);
        }

        return false;
    }
}