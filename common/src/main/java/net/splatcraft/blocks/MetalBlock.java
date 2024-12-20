package net.splatcraft.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;

public class MetalBlock extends Block
{
    public MetalBlock(MapColor color)
    {
        super(AbstractBlock.Settings.create().mapColor(color).requiresTool().strength(5.0F, 6.0F).sounds(BlockSoundGroup.METAL));
    }
}
