package net.splatcraft.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class MetalBlock extends Block
{
    public MetalBlock(MapColor color)
    {
        super(BlockBehaviour.Properties.of().mapColor(color).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL));
    }
}
