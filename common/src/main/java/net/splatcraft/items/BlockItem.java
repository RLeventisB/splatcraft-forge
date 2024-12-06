package net.splatcraft.forge.items;

import net.minecraft.world.level.block.Block;

public class BlockItem extends net.minecraft.world.item.BlockItem
{
    public BlockItem(Block block)
    {
        super(block, new Properties());
    }

    public BlockItem(Block block, Properties properties)
    {
        super(block, properties);
    }
}
