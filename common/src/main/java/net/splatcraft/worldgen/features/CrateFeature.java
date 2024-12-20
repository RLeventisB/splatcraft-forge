package net.splatcraft.worldgen.features;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.CountConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.splatcraft.blocks.CrateBlock;
import net.splatcraft.blocks.DebrisBlock;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.tileentities.CrateTileEntity;

public class CrateFeature extends Feature<CountConfig>
{
    public CrateFeature(Codec<CountConfig> codec)
    {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<CountConfig> context)
    {
        int i = 0;
        Random random = context.getRandom();
        StructureWorldAccess worldAccess = context.getWorld();
        BlockPos blockpos = context.getOrigin();
        int j = context.getConfig().getCount().get(random);

        int area = 8;

        for (int k = 0; k < j; ++k)
        {
            int l = random.nextInt(area) - random.nextInt(area);
            int i1 = random.nextInt(area) - random.nextInt(area);
            int j1 = worldAccess.getTopY(Heightmap.Type.OCEAN_FLOOR, blockpos.getX() + l, blockpos.getZ() + i1);
            BlockPos blockpos1 = new BlockPos(blockpos.getX() + l, j1, blockpos.getZ() + i1);

            boolean isSunken = random.nextFloat() <= 0.05f;

            BlockState state = isSunken ? SplatcraftBlocks.sunkenCrate.get().getDefaultState() : SplatcraftBlocks.crate.get().getDefaultState();
            worldAccess.setBlockState(blockpos1, state, 2);

            if (!isSunken)
            {
                if (worldAccess.getBlockEntity(blockpos1) instanceof CrateTileEntity crate)
                    crate.setLootTable(CrateBlock.STORAGE_EGG_CRATE);
            }

            ++i;
        }

        if (random.nextFloat() <= 0.0125f * j)
        {
            int l = random.nextInt(area) - random.nextInt(area);
            int i1 = random.nextInt(area) - random.nextInt(area);
            int j1 = worldAccess.getTopY(Heightmap.Type.OCEAN_FLOOR, blockpos.getX() + l, blockpos.getZ() + i1);
            BlockPos blockpos1 = new BlockPos(blockpos.getX() + l, j1, blockpos.getZ() + i1);
            BlockState state = SplatcraftBlocks.ammoKnightsDebris.get().getDefaultState().with(DebrisBlock.DIRECTION, Direction.fromHorizontal(random.nextInt(4)));

            if (state.canPlaceAt(worldAccess, blockpos1))
            {
                worldAccess.setBlockState(blockpos1, state.with(DebrisBlock.WATERLOGGED, worldAccess.getFluidState(blockpos1).isOf(Fluids.WATER)), 2);
                ++i;
            }
        }

        return i > 0;
    }
}
