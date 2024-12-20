package net.splatcraft.worldgen.features;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.splatcraft.registries.SplatcraftBlocks;

public class SardiniumDepositFeature extends Feature<DefaultFeatureConfig>
{
    public SardiniumDepositFeature(Codec<DefaultFeatureConfig> codec)
    {
        super(codec);
    }

    private static boolean isIcebergState(BlockState state)
    {
        return state.isOf(SplatcraftBlocks.coralite.get()) || state.isOf(SplatcraftBlocks.sardiniumOre.get()) || state.isOf(SplatcraftBlocks.rawSardiniumBlock.get());
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context)
    {
        Random random = context.getRandom();
        BlockPos centerPos = context.getOrigin();

        // what do these meannnnnnnn did this mod get obfuscated and recovered or what
        StructureWorldAccess worldAccess = context.getWorld();
        centerPos = new BlockPos(centerPos.getX(), worldAccess.getTopY(Heightmap.Type.OCEAN_FLOOR, centerPos.getX(), centerPos.getZ()), centerPos.getZ());
        boolean flag = random.nextDouble() > 0.7D;
        double d0 = random.nextDouble() * 2.0D * Math.PI;
        int i = 11 - random.nextInt(5);
        int j = 3 + random.nextInt(3);
        boolean flag1 = random.nextDouble() > 0.7D;
        int k = 11;
        int l = flag1 ? random.nextInt(6) + 6 : random.nextInt(15) + 3;
        if (!flag1 && random.nextDouble() > 0.9D)
        {
            l += random.nextInt(19) + 7;
        }

        int i1 = Math.min(l + random.nextInt(11), 18);
        int j1 = Math.min(l + random.nextInt(7) - random.nextInt(5), 11);
        int k1 = flag1 ? i : 11;

        int radius = 8;

        for (int l1 = -k1; l1 < k1; ++l1)
        {
            for (int i2 = -k1; i2 < k1; ++i2)
            {
                for (int j2 = 0; j2 < l; ++j2)
                {
                    int k2 = flag1 ? heightDependentRadiusEllipse(j2, l, j1) : heightDependentRadiusRound(random, j2, l, j1);
                    if (flag1 || l1 < k2)
                    {
                        generateIcebergBlock(worldAccess, random, centerPos, l, l1, j2, i2, k2, k1, flag1, j, d0, random.nextFloat() < 0.2f ? SplatcraftBlocks.sardiniumOre.get().getDefaultState() : SplatcraftBlocks.coralite.get().getDefaultState());
                    }
                }
            }
        }

        for (int i3 = -k1; i3 < k1; ++i3)
        {
            for (int j3 = -k1; j3 < k1; ++j3)
            {
                for (int k3 = -1; k3 > -i1; --k3)
                {
                    int l3 = flag1 ? MathHelper.ceil((float) k1 * (1.0F - (float) Math.pow(k3, 2.0D) / ((float) i1 * 8.0F))) : k1;
                    int l2 = heightDependentRadiusSteep(random, -k3, i1, j1);
                    if (i3 < l2)
                    {
                        generateIcebergBlock(context.getWorld(), random, centerPos, i1, i3, k3, j3, l2, l3, flag1, j, d0,
                            random.nextFloat() < 0.05f ? SplatcraftBlocks.rawSardiniumBlock.get().getDefaultState() : random.nextFloat() < 0.3f ? SplatcraftBlocks.sardiniumOre.get().getDefaultState() : SplatcraftBlocks.coralite.get().getDefaultState());
                    }
                }
            }
        }

		/*
		for(int xx = -radius; xx <= radius; xx++)
			for(int yy = -radius; yy <= radius; yy++)
				for(int zz = -radius; zz <= radius; zz++)
				{
					BlockPos pos = centerPos.offset(xx,yy,zz);
					int i = heightDependentRadiusRound(random, xx,yy,zz);
					if (Math.abs(i) <= 2)
					{
						System.out.println(i);
					}
				}
		*/
        return true;
    }

    private void generateIcebergBlock(WorldAccess level, Random raqndom, BlockPos p_66061_, int p_66062_, int p_66063_, int p_66064_, int p_66065_, int p_66066_, int p_66067_, boolean p_66068_, int p_66069_, double p_66070_, BlockState state)
    {
        double d0 = p_66068_ ? signedDistanceEllipse(p_66063_, p_66065_, new BlockPos(BlockPos.ZERO), p_66067_, getEllipseC(p_66064_, p_66062_, p_66069_), p_66070_) : signedDistanceCircle(p_66063_, p_66065_, new BlockPos(BlockPos.ZERO), p_66066_, raqndom);
        if (d0 < 0.0D)
        {
            BlockPos blockpos = p_66061_.add(p_66063_, p_66064_, p_66065_);
            double d1 = p_66068_ ? -0.5D : (double) (-6 - raqndom.nextInt(3));
            if (d0 > d1 && raqndom.nextDouble() > 0.9D)
            {
                return;
            }

            level.setBlockState(blockpos, state, 2);
        }
    }

    private int getEllipseC(int p_66019_, int p_66020_, int p_66021_)
    {
        int i = p_66021_;
        if (p_66019_ > 0 && p_66020_ - p_66019_ <= 3)
        {
            i = p_66021_ - (4 - (p_66020_ - p_66019_));
        }

        return i;
    }

    private double signedDistanceCircle(int p_66030_, int p_66031_, BlockPos p_66032_, int p_66033_, Random p_66034_)
    {
        float f = 10.0F * MathHelper.clamp(p_66034_.nextFloat(), 0.2F, 0.8F) / (float) p_66033_;
        return (double) f + Math.pow(p_66030_ - p_66032_.getX(), 2.0D) + Math.pow(p_66031_ - p_66032_.getZ(), 2.0D) - Math.pow(p_66033_, 2.0D);
    }

    private double signedDistanceEllipse(int p_66023_, int p_66024_, BlockPos p_66025_, int p_66026_, int p_66027_, double p_66028_)
    {
        return Math.pow(((double) (p_66023_ - p_66025_.getX()) * Math.cos(p_66028_) - (double) (p_66024_ - p_66025_.getZ()) * Math.sin(p_66028_)) / (double) p_66026_, 2.0D) + Math.pow(((double) (p_66023_ - p_66025_.getX()) * Math.sin(p_66028_) + (double) (p_66024_ - p_66025_.getZ()) * Math.cos(p_66028_)) / (double) p_66027_, 2.0D) - 1.0D;
    }

    private int heightDependentRadiusSteep(Random p_66114_, int p_66115_, int p_66116_, int p_66117_)
    {
        float f = 1.0F + p_66114_.nextFloat() / 2.0F;
        float f1 = (1.0F - (float) p_66115_ / ((float) p_66116_ * f)) * (float) p_66117_;
        return MathHelper.ceil(f1 / 2.0F);
    }

    private int heightDependentRadiusRound(Random p_66095_, int p_66096_, int p_66097_, int p_66098_)
    {
        float f = 3.5F - p_66095_.nextFloat();
        float f1 = (1.0F - (float) Math.pow(p_66096_, 2.0D) / ((float) p_66097_ * f)) * (float) p_66098_;
        if (p_66097_ > 15 + p_66095_.nextInt(5))
        {
            int i = p_66096_ < 3 + p_66095_.nextInt(6) ? p_66096_ / 2 : p_66096_;
            f1 = (1.0F - (float) i / ((float) p_66097_ * f * 0.4F)) * (float) p_66098_;
        }

        return MathHelper.ceil(f1 / 2.0F);
    }

    private int heightDependentRadiusEllipse(int p_66110_, int p_66111_, int p_66112_)
    {
        float f = 1.0F;
        float f1 = (1.0F - (float) Math.pow(p_66110_, 2.0D) / ((float) p_66111_)) * (float) p_66112_;
        return MathHelper.ceil(f1 / 2.0F);
    }

    private void smooth(WorldAccess access, BlockPos pos, int p_66054_, int p_66055_, boolean p_66056_, int p_66057_)
    {
        int i = p_66056_ ? p_66057_ : p_66054_ / 2;

        for (int j = -i; j <= i; ++j)
        {
            for (int k = -i; k <= i; ++k)
            {
                for (int l = 0; l <= p_66055_; ++l)
                {
                    BlockPos blockpos = pos.add(j, l, k);
                    BlockState blockstate = access.getBlockState(blockpos);
                    if (isIcebergState(blockstate) || blockstate.isOf(Blocks.SNOW))
                    {
                        if (belowIsAir(access, blockpos))
                        {
                            setBlockState(access, blockpos, Blocks.AIR.getDefaultState());
                            setBlockState(access, blockpos.up(), Blocks.AIR.getDefaultState());
                        }
                        else if (isIcebergState(blockstate))
                        {
                            BlockState[] ablockstate = new BlockState[]{access.getBlockState(blockpos.west()), access.getBlockState(blockpos.east()), access.getBlockState(blockpos.north()), access.getBlockState(blockpos.south())};
                            int i1 = 0;

                            for (BlockState blockstate1 : ablockstate)
                            {
                                if (!isIcebergState(blockstate1))
                                {
                                    ++i1;
                                }
                            }

                            if (i1 >= 3)
                            {
                                setBlockState(access, blockpos, Blocks.AIR.getDefaultState());
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean belowIsAir(BlockView access, BlockPos pos)
    {
        return access.getBlockState(pos.down()).isAir();
    }
}
