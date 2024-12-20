package net.splatcraft.items;

import net.minecraft.block.Block;

public class BlockItem extends net.minecraft.item.BlockItem
{
    public BlockItem(Block block)
    {
        super(block, new Settings());
    }

    public BlockItem(Block block, Settings properties)
    {
        super(block, properties);
    }
}
