package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.tileentities.InkedBlockTileEntity;
import net.splatcraft.util.BlockInkedResult;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkedBlock extends Block implements BlockEntityProvider, IColoredBlock, ISplatcraftForgeBlockDummy
{
    public static final int GLOWING_LIGHT_LEVEL = 6;

    public InkedBlock()
    {
        this(defaultProperties());
    }

    public InkedBlock(Settings properties)
    {
        super(properties);
        SplatcraftBlocks.inkColoredBlocks.add(this);
    }

    private static Settings defaultProperties()
    {
        return Settings.create().mapColor(MapColor.TERRACOTTA_BLACK).ticksRandomly().requiresTool().sounds(SplatcraftSounds.SOUND_TYPE_INK).nonOpaque().dynamicBounds();
    }

    public static boolean isTouchingLiquid(BlockView reader, BlockPos pos, Direction direction)
    {
        return isTouchingLiquid(reader, pos, new Direction[]{direction});
    }

    public static boolean isTouchingLiquid(BlockView reader, BlockPos pos, Direction... directions)
    {
        boolean flag = false;
        BlockPos.Mutable blockpos$mutable = pos.mutableCopy();

        BlockState currentState = reader.getBlockState(pos);

        if (currentState.contains(Properties.WATERLOGGED) && currentState.get(Properties.WATERLOGGED))
        {
            return true;
        }

        for (Direction direction : directions)
        {
            blockpos$mutable.set(pos, direction);
            BlockState blockstate = reader.getBlockState(blockpos$mutable);

            if (causesClear(reader, pos, blockstate, direction))
            {
                flag = true;
                break;
            }
        }

        return flag;
    }

    public static boolean causesClear(BlockView level, BlockPos pos, BlockState state)
    {
        return causesClear(level, pos, state, Direction.UP);
    }

    public static boolean causesClear(BlockView level, BlockPos pos, BlockState state, Direction dir)
    {
        if (state.isIn(SplatcraftTags.Blocks.INK_CLEARING_BLOCKS))
            return true;

        if (dir != Direction.DOWN && state.getFluidState().isIn(FluidTags.WATER))
            return !state.isSideSolidFullSquare(level, pos, dir.getOpposite());

        return false;
    }

    public static InkedBlock glowing()
    {
        return new InkedBlock(defaultProperties().luminance(state -> GLOWING_LIGHT_LEVEL));
    }

    private static BlockState clearInk(WorldAccess level, BlockPos pos)
    {
        return level.getBlockState(pos);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull World world, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
    {
        return type == SplatcraftTileEntities.inkedTileEntity.get() ? InkedBlockTileEntity::tick : null;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return SplatcraftTileEntities.inkedTileEntity.get().instantiate(pos, state);
    }

    /*@Override
    public ItemStack getPickStack(BlockState state, HitResult target, BlockView level, BlockPos pos, PlayerEntity player)
    {
        if (level.getBlockEntity(pos) instanceof InkedBlockTileEntity blockEntity)
        {
            BlockState savedState = blockEntity.getSavedState();
            return savedState.getBlock().getPickStack(savedState, target, level, pos, player);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockView level, BlockPos pos, PlayerEntity player)
    {
        if (level.getBlockEntity(pos) instanceof InkedBlockTileEntity)
        {
            BlockState savedState = ((InkedBlockTileEntity) level.getBlockEntity(pos)).getSavedState();
            return savedState.getBlock().canHarvestBlock(savedState, level, pos, player);
        }
        return super.canHarvestBlock(state, level, pos, player);
    }*/

    @Override
    public void afterBreak(@NotNull World world, @NotNull PlayerEntity playerEntity, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable BlockEntity no, @NotNull ItemStack stack)
    {
        if (no instanceof InkedBlockTileEntity tileEntity)
        {
            BlockState savedState = tileEntity.getSavedState();
            savedState.getBlock().afterBreak(world, playerEntity, pos, savedState, null, stack);
        }
        super.afterBreak(world, playerEntity, pos, state, no, stack);
    }

    /*@Override
    public boolean collisionExtendsVertically(BlockState state, BlockView level, BlockPos pos, Entity collidingEntity)
    {
        if (!(level.getBlockEntity(pos) instanceof InkedBlockTileEntity))
            return super.collisionExtendsVertically(state, level, pos, collidingEntity);
        BlockState savedState = ((InkedBlockTileEntity) level.getBlockEntity(pos)).getSavedState();

        if (savedState == null || savedState.getBlock().equals(this))
            return super.collisionExtendsVertically(state, level, pos, collidingEntity);
        return savedState.getBlock().collisionExtendsVertically(savedState, level, pos, collidingEntity);
    }*/

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
    {
        if (!(levelIn.getBlockEntity(pos) instanceof InkedBlockTileEntity tileEntity))
            return super.getCollisionShape(state, levelIn, pos, context);
        BlockState savedState = tileEntity.getSavedState();

        if (savedState == null || savedState.getBlock().equals(this))
        {
            return super.getCollisionShape(state, levelIn, pos, context);
        }
        VoxelShape result = savedState.getCollisionShape(levelIn, pos, context);
        if (!result.isEmpty())
            return result;
        return super.getCollisionShape(state, levelIn, pos, context);
    }

    @Override
    public @NotNull VoxelShape getCameraCollisionShape(@NotNull BlockState state, BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
    {
        if (!(levelIn.getBlockEntity(pos) instanceof InkedBlockTileEntity))
            return super.getCameraCollisionShape(state, levelIn, pos, context);
        BlockState savedState = ((InkedBlockTileEntity) levelIn.getBlockEntity(pos)).getSavedState();

        if (savedState == null || savedState.getBlock().equals(this))
        {
            return super.getCameraCollisionShape(state, levelIn, pos, context);
        }
        VoxelShape result = savedState.getCameraCollisionShape(levelIn, pos, context);
        if (!result.isEmpty())
            return result;
        return super.getCameraCollisionShape(state, levelIn, pos, context);
    }

    @Override
    public @NotNull PistonBehavior getPistonBehavior(@NotNull BlockState state)
    {
        return PistonBehavior.BLOCK;
    }

    @Override
    public float calcBlockBreakingDelta(@NotNull BlockState state, @NotNull PlayerEntity player, BlockView levelIn, @NotNull BlockPos pos)
    {
        if (!(levelIn.getBlockEntity(pos) instanceof InkedBlockTileEntity te))
            return super.calcBlockBreakingDelta(state, player, levelIn, pos);

        if (te.getSavedState().getBlock() instanceof InkedBlock)
            return super.calcBlockBreakingDelta(state, player, levelIn, pos);

        return te.getSavedState().calcBlockBreakingDelta(player, levelIn, pos);
    }

    //todo
    /*@Override
    public float getExplosionResistance(BlockState state, BlockView level, BlockPos pos, Explosion explosion)
    {
        if (!(level.getBlockEntity(pos) instanceof InkedBlockTileEntity te))
            return super.getExplosionResistance(state, level, pos, explosion);

        if (te.getSavedState().getBlock() instanceof InkedBlock)
            return super.getExplosionResistance(state, level, pos, explosion);

        return te.getSavedState().getBlock().getExplosionResistance(te.getSavedState(), level, pos, explosion);
    }*/

    @Override
    public boolean addLandingEffects(BlockState state1, ServerWorld level, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles)
    {
        ColorUtils.addInkSplashParticle(level, getColor(level, pos), entity.getX(), entity.getBodyY(level.random.nextFloat() * 0.3f), entity.getZ(), (float) Math.sqrt(numberOfParticles) * 0.3f);
        return true;
    }

    @Override
    public boolean addRunningEffects(BlockState state, World world, BlockPos pos, Entity entity)
    {
        ColorUtils.addInkSplashParticle(world, getColor(world, pos), entity.getX(), entity.getBodyY(world.getRandom().nextFloat() * 0.3f), entity.getZ(), 0.6f);
        return true;
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerWorld world, @NotNull BlockPos pos, @NotNull Random randomSource)
    {
        if (SplatcraftGameRules.getLocalizedRule(world, pos, SplatcraftGameRules.INK_DECAY) && world.getBlockEntity(pos) instanceof InkedBlockTileEntity)
        {
            boolean decay = world.hasRain(pos);

            if (!decay)
            {
                int i = 0;
                for (Direction dir : Direction.values())
                    if (world.getBlockEntity(pos.offset(dir)) instanceof InkedBlockTileEntity)
                        i++;
                decay = i <= 0 || randomSource.nextInt(i * 2) == 0;
            }

            if (decay)
                world.updateListeners(pos, world.getBlockState(pos), clearInk(world, pos), 3);
        }
    }

    @Override
    public @NotNull BlockState getStateForNeighborUpdate(@NotNull BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull WorldAccess levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
    {
        if (isTouchingLiquid(levelIn, currentPos))
        {
            if (levelIn.getBlockEntity(currentPos) instanceof InkedBlockTileEntity)
                return clearInk(levelIn, currentPos);
        }

        if (levelIn.getBlockEntity(currentPos) instanceof InkedBlockTileEntity inkedBlock)
        {
            BlockState savedState = inkedBlock.getSavedState();

            if (savedState != null && !savedState.getBlock().equals(this))
            {
                if (levelIn.getBlockEntity(facingPos) instanceof InkedBlockTileEntity facedInkedBlock)
                    facingState = facedInkedBlock.getSavedState();

                inkedBlock.setSavedState(savedState.getStateForNeighborUpdate(facing, facingState, levelIn, currentPos, facingPos));
            }
        }

        return super.getStateForNeighborUpdate(stateIn, facing, facingState, levelIn, currentPos, facingPos);
    }

    @Override
    public boolean canClimb()
    {
        return true;
    }

    @Override
    public boolean canSwim()
    {
        return true;
    }

    @Override
    public boolean canDamage()
    {
        return true;
    }

    @Override
    public InkColor getColor(WorldView world, BlockPos pos)
    {
        if (world.getBlockEntity(pos) instanceof InkColorTileEntity colorTile)
        {
            return colorTile.getInkColor();
        }
        return InkColor.INVALID;
    }

    @Override
    public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
    {

        return false;
    }

    @Override
    public boolean remoteInkClear(World world, BlockPos pos)
    {
        BlockState oldState = world.getBlockState(pos);
        if (world.getBlockEntity(pos) instanceof InkedBlockTileEntity blockEntity)
        {
            InkColor color = blockEntity.getInkColor();

            if (clearInk(world, pos).equals(oldState) && (!(world.getBlockEntity(pos) instanceof InkedBlockTileEntity blockEntity2) || blockEntity2.getInkColor() == color))
                return false;
            world.updateListeners(pos, oldState, world.getBlockState(pos), 3);
            return true;
        }
        return false;
    }

    @Override
    public BlockInkedResult inkBlock(World world, BlockPos pos, InkColor color, float damage, InkBlockUtils.InkType inkType)
    {
        if (!(world.getBlockEntity(pos) instanceof InkedBlockTileEntity te))
            return BlockInkedResult.FAIL;

        BlockState oldState = world.getBlockState(pos);
        BlockState state = world.getBlockState(pos);
        boolean changeColor = te.getInkColor() != color;

        if (changeColor)
            te.setColor(color);
        BlockState inkState = InkBlockUtils.getInkState(inkType);

        if (inkState.getBlock() != state.getBlock())
        {
            state = inkState;
            world.setBlockState(pos, state, 2);
            InkedBlockTileEntity newTe = (InkedBlockTileEntity) world.getBlockEntity(pos);
            newTe.setSavedState(te.getSavedState());
            newTe.setSavedColor(te.getSavedColor());
            newTe.setColor(te.getInkColor());
            newTe.setPermanentInkType(te.getPermanentInkType());
            newTe.setPermanentColor(te.getPermanentColor());

            //level.setBlockEntity(pos, newTe);
        }
        world.updateListeners(pos, oldState, state, 2);

        return changeColor ? BlockInkedResult.SUCCESS : BlockInkedResult.ALREADY_INKED;
    }
}
