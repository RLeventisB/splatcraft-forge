package net.splatcraft.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;

public class OreBlock extends Block
{
    public OreBlock()
    {
        super(AbstractBlock.Settings.create().mapColor(MapColor.STONE_GRAY).instrument(NoteBlockInstrument.BASEDRUM).strength(3.0F, 3.0F).requiresTool());
    }
}
