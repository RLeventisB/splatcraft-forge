package net.splatcraft.blocks;

import net.minecraft.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerWorld;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkedBlock extends Block implements EntityBlock, IColoredBlock
{
    public static final int GLOWING_LIGHT_LEVEL = 6;

    public InkedBlock()
    {
        this(defaultProperties());
    }

    public InkedBlock(Properties properties)
    {
        super(properties);
        SplatcraftBlocks.inkColoredBlocks.add(this);
    }

    private static Properties defaultProperties()
    {
        return Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).randomTicks().requiresCorrectToolForDrops().sound(SplatcraftSounds.SOUND_TYPE_INK).noOcclusion().dynamicShape();
    }

    public static boolean isTouchingLiquid(BlockGetter reader, BlockPos pos, Direction direction)
    {
        return isTouchingLiquid(reader, pos, new Direction[]{direction});
    }

    public static boolean isTouchingLiquid(BlockGetter reader, BlockPos pos, Direction... directions)
    {
        boolean flag = false;
        BlockPos.MutableBlockPos blockpos$mutable = pos.mutable();

        BlockState currentState = reader.getBlockState(pos);

        if (currentState.hasProperty(BlockStateProperties.WATERLOGGED) && currentState.getValue(BlockStateProperties.WATERLOGGED))
        {
            return true;
        }

        for (Direction direction : directions)
        {
            blockpos$mutable.setWithOffset(pos, direction);
            BlockState blockstate = reader.getBlockState(blockpos$mutable);

            if (causesClear(reader, pos, blockstate, direction))
            {
                flag = true;
                break;
            }
        }

        return flag;
    }

    public static boolean causesClear(BlockGetter level, BlockPos pos, BlockState state)
    {
        return causesClear(level, pos, state, Direction.UP);
    }

    public static boolean causesClear(BlockGetter level, BlockPos pos, BlockState state, Direction dir)
    {
        if (state.is(SplatcraftTags.Blocks.INK_CLEARING_BLOCKS))
            return true;

        if (dir != Direction.DOWN && state.getFluidState().is(FluidTags.WATER))
            return !state.isFaceSturdy(level, pos, dir.getOpposite());

        return false;
    }

    public static InkedBlock glowing()
    {
        return new InkedBlock(defaultProperties().lightLevel(state -> GLOWING_LIGHT_LEVEL));
    }

    private static BlockState clearInk(LevelAccessor level, BlockPos pos)
    {
        return level.getBlockState(pos);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
    {
        return type == SplatcraftTileEntities.inkedTileEntity.get() ? InkedBlockTileEntity::tick : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {

        return SplatcraftTileEntities.inkedTileEntity.get().create(pos, state);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player)
    {
        if (level.getBlockEntity(pos) instanceof InkedBlockTileEntity)
        {
            BlockState savedState = ((InkedBlockTileEntity) level.getBlockEntity(pos)).getSavedState();
            return savedState.getBlock().getCloneItemStack(savedState, target, level, pos, player);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player)
    {
        if (level.getBlockEntity(pos) instanceof InkedBlockTileEntity)
        {
            BlockState savedState = ((InkedBlockTileEntity) level.getBlockEntity(pos)).getSavedState();
            return savedState.getBlock().canHarvestBlock(savedState, level, pos, player);
        }
        return super.canHarvestBlock(state, level, pos, player);
    }

    @Override
    public void playerDestroy(@NotNull Level level, @NotNull Player playerEntity, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable BlockEntity tileEntity, @NotNull ItemStack stack)
    {
        if (tileEntity instanceof InkedBlockTileEntity)
        {
            BlockState savedState = ((InkedBlockTileEntity) tileEntity).getSavedState();
            savedState.getBlock().playerDestroy(level, playerEntity, pos, savedState, null, stack);
        }
        super.playerDestroy(level, playerEntity, pos, state, tileEntity, stack);
    }

    /*
    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
    */
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, BlockGetter levelIn, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        if (!(levelIn.getBlockEntity(pos) instanceof InkedBlockTileEntity))
            return Shapes.empty();
        BlockState savedState = ((InkedBlockTileEntity) levelIn.getBlockEntity(pos)).getSavedState();

        if (savedState == null || savedState.getBlock().equals(this))
            return super.getShape(state, levelIn, pos, context);

        VoxelShape result = savedState.getBlock().getShape(savedState, levelIn, pos, context);
        if (!result.isEmpty())
            return result;
        return super.getShape(state, levelIn, pos, context);
    }

    @Override
    public boolean collisionExtendsVertically(BlockState state, BlockGetter level, BlockPos pos, Entity collidingEntity)
    {
        if (!(level.getBlockEntity(pos) instanceof InkedBlockTileEntity))
            return super.collisionExtendsVertically(state, level, pos, collidingEntity);
        BlockState savedState = ((InkedBlockTileEntity) level.getBlockEntity(pos)).getSavedState();

        if (savedState == null || savedState.getBlock().equals(this))
            return super.collisionExtendsVertically(state, level, pos, collidingEntity);
        return savedState.getBlock().collisionExtendsVertically(savedState, level, pos, collidingEntity);
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, BlockGetter levelIn, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        if (!(levelIn.getBlockEntity(pos) instanceof InkedBlockTileEntity))
            return super.getCollisionShape(state, levelIn, pos, context);
        BlockState savedState = ((InkedBlockTileEntity) levelIn.getBlockEntity(pos)).getSavedState();

        if (savedState == null || savedState.getBlock().equals(this))
        {
            return super.getCollisionShape(state, levelIn, pos, context);
        }
        VoxelShape result = savedState.getBlock().getCollisionShape(savedState, levelIn, pos, context);
        if (!result.isEmpty())
            return result;
        return super.getCollisionShape(state, levelIn, pos, context);
    }

    @Override
    public @NotNull VoxelShape getVisualShape(@NotNull BlockState state, BlockGetter levelIn, @NotNull BlockPos pos, @NotNull CollisionContext context)
    {
        if (!(levelIn.getBlockEntity(pos) instanceof InkedBlockTileEntity))
            return super.getVisualShape(state, levelIn, pos, context);
        BlockState savedState = ((InkedBlockTileEntity) levelIn.getBlockEntity(pos)).getSavedState();

        if (savedState == null || savedState.getBlock().equals(this))
        {
            return super.getVisualShape(state, levelIn, pos, context);
        }
        VoxelShape result = savedState.getBlock().getVisualShape(savedState, levelIn, pos, context);
        if (!result.isEmpty())
            return result;
        return super.getVisualShape(state, levelIn, pos, context);
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction(@NotNull BlockState state)
    {
        return PushReaction.BLOCK;
    }

    @Override
    public float getDestroyProgress(@NotNull BlockState state, @NotNull Player player, BlockGetter levelIn, @NotNull BlockPos pos)
    {
        if (!(levelIn.getBlockEntity(pos) instanceof InkedBlockTileEntity te))
            return super.getDestroyProgress(state, player, levelIn, pos);

        if (te.getSavedState().getBlock() instanceof InkedBlock)
            return super.getDestroyProgress(state, player, levelIn, pos);

        return te.getSavedState().getBlock().getDestroyProgress(te.getSavedState(), player, levelIn, pos);
    }

    @Override
    public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion)
    {
        if (!(level.getBlockEntity(pos) instanceof InkedBlockTileEntity te))
            return super.getExplosionResistance(state, level, pos, explosion);

        if (te.getSavedState().getBlock() instanceof InkedBlock)
            return super.getExplosionResistance(state, level, pos, explosion);

        return te.getSavedState().getBlock().getExplosionResistance(te.getSavedState(), level, pos, explosion);
    }

    @Override
    public boolean addLandingEffects(BlockState state1, ServerWorld levelserver, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles)
    {
        ColorUtils.addInkSplashParticle(levelserver, getColor(levelserver, pos), entity.getX(), entity.getY(levelserver.random.nextFloat() * 0.3f), entity.getZ(), (float) Math.sqrt(numberOfParticles) * 0.3f);
        return true;
    }

    @Override
    public boolean addRunningEffects(BlockState state, Level level, BlockPos pos, Entity entity)
    {
        ColorUtils.addInkSplashParticle(level, getColor(level, pos), entity.getX(), entity.getY(level.getRandom().nextFloat() * 0.3f), entity.getZ(), 0.6f);
        return true;
    }

    @Override
    public void randomTick(@NotNull BlockState state, @NotNull ServerWorld level, @NotNull BlockPos pos, @NotNull RandomSource randomSource)
    {
        if (SplatcraftGameRules.getLocalizedRule(level, pos, SplatcraftGameRules.INK_DECAY) && level.getBlockEntity(pos) instanceof InkedBlockTileEntity)
        {
            boolean decay = level.isRainingAt(pos);

            if (!decay)
            {
                int i = 0;
                for (Direction dir : Direction.values())
                    if (level.getBlockEntity(pos.relative(dir)) instanceof InkedBlockTileEntity)
                        i++;
                decay = i <= 0 || randomSource.nextInt(i * 2) == 0;
            }

            if (decay)
                level.sendBlockUpdated(pos, level.getBlockState(pos), clearInk(level, pos), 3);
        }
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState stateIn, @NotNull Direction facing, @NotNull BlockState facingState, @NotNull LevelAccessor levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
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

                inkedBlock.setSavedState(savedState.getBlock().updateShape(savedState, facing, facingState, levelIn, currentPos, facingPos));
            }
        }

        return super.updateShape(stateIn, facing, facingState, levelIn, currentPos, facingPos);
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
    public int getColor(Level level, BlockPos pos)
    {
        if (level.getBlockEntity(pos) instanceof InkColorTileEntity)
        {
            return ((InkColorTileEntity) level.getBlockEntity(pos)).getColor();
        }
        return -1;
    }

    @Override
    public boolean remoteColorChange(Level level, BlockPos pos, int newColor)
    {

        return false;
    }

    @Override
    public boolean remoteInkClear(Level level, BlockPos pos)
    {
        BlockState oldState = level.getBlockState(pos);
        if (level.getBlockEntity(pos) instanceof InkedBlockTileEntity)
        {
            int color = ((InkedBlockTileEntity) level.getBlockEntity(pos)).getColor();

            if (clearInk(level, pos).equals(oldState) && (!(level.getBlockEntity(pos) instanceof InkedBlockTileEntity) || ((InkedBlockTileEntity) level.getBlockEntity(pos)).getColor() == color))
                return false;
            level.sendBlockUpdated(pos, oldState, level.getBlockState(pos), 3);
            return true;
        }
        return false;
    }

    @Override
    public BlockInkedResult inkBlock(Level level, BlockPos pos, int color, float damage, InkBlockUtils.InkType inkType)
    {
        if (!(level.getBlockEntity(pos) instanceof InkedBlockTileEntity te))
            return BlockInkedResult.FAIL;

        BlockState oldState = level.getBlockState(pos);
        BlockState state = level.getBlockState(pos);
        boolean changeColor = te.getColor() != color;

        if (changeColor)
            te.setColor(color);
        BlockState inkState = InkBlockUtils.getInkState(inkType);

        if (inkState.getBlock() != state.getBlock())
        {
            state = inkState;
            level.setBlock(pos, state, 2);
            InkedBlockTileEntity newTe = (InkedBlockTileEntity) level.getBlockEntity(pos);
            newTe.setSavedState(te.getSavedState());
            newTe.setSavedColor(te.getSavedColor());
            newTe.setColor(te.getColor());
            newTe.setPermanentInkType(te.getPermanentInkType());
            newTe.setPermanentColor(te.getPermanentColor());

            //level.setBlockEntity(pos, newTe);
        }
        level.sendBlockUpdated(pos, oldState, state, 2);

        return changeColor ? BlockInkedResult.SUCCESS : BlockInkedResult.ALREADY_INKED;
    }
}
