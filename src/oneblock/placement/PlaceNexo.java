package oneblock.placement;

import com.nexomc.nexo.api.NexoBlocks;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class PlaceNexo extends Place {

    @Override
    public boolean setType(Block block, Object material_, boolean physics) {
        if (material_ instanceof Material)
            block.setType((Material) material_, physics);
        else if (material_ instanceof String) {
            String material = (String) material_;
            if (NexoBlocks.isCustomBlock(material)) {
                NexoBlocks.place(material, block.getLocation());
                return true;
            }
        }

        return false;
    }
}