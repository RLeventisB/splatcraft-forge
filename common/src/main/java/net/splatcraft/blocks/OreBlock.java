package net.splatcraft.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public class OreBlock extends Block
{
    public OreBlock()
    {
        super(Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).strength(3.0F, 3.0F).requiresCorrectToolForDrops());
    }
}
