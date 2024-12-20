package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.items.IColoredItem;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.RemotePedestalTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemotePedestalBlock extends Block implements IColoredBlock, BlockEntityProvider, ISplatcraftForgeBlockDummy
{
    public static final BooleanProperty POWERED = Properties.POWERED;
    private static final VoxelShape SHAPE = VoxelShapes.union(
        createCuboidShape(3, 0, 3, 13, 2, 13),
        createCuboidShape(4, 2, 4, 12, 3, 12),
        createCuboidShape(5, 3, 5, 11, 11, 11),
        createCuboidShape(4, 11, 4, 12, 13, 12)
    );

    public RemotePedestalBlock()
    {
        super(AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY).strength(2.0f).requiresTool());
        SplatcraftBlocks.inkColoredBlocks.add(this);
        setDefaultState(getDefaultState().with(POWERED, false));
    }

    @Override
    public VoxelShape getOutlineShape(@NotNull BlockState state, @NotNull BlockView levelIn, @NotNull BlockPos pos, @NotNull ShapeContext context)
    {
        return SHAPE;
    }

    @Override
    public ItemStack getPickStack(WorldView level, BlockPos pos, BlockState state)
    {
        return ColorUtils.setColorLocked(ColorUtils.setInkColor(super.getPickStack(level, pos, state), getColor(level, pos)), true);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> container)
    {
        container.add(POWERED);
    }

    @Override
    public @NotNull ActionResult onUse(@NotNull BlockState state, World world, @NotNull BlockPos pos, @NotNull PlayerEntity player, @NotNull BlockHitResult rayTrace)
    {
        if (world.getBlockEntity(pos) instanceof RemotePedestalTileEntity te)
        {
            if (te.isEmpty() && player.getStackInHand(player.getActiveHand()).isIn(SplatcraftTags.Items.REMOTES))
            {
                te.setStack(0, player.getStackInHand(player.getActiveHand()).copy());
                player.getStackInHand(player.getActiveHand()).setCount(0);
                return ActionResult.success(world.isClient);
            }
            else if (!te.isEmpty())
            {
                ItemStack remote = te.removeStack(0);
                if (!player.giveItemStack(remote))
                    CommonUtils.spawnItem(world, pos.up(), remote);
                return ActionResult.success(world.isClient);
            }
        }

        return super.onUse(state, world, pos, player, rayTrace);
    }

    @Override
    public void neighborUpdate(BlockState state, World levelIn, @NotNull BlockPos pos, @NotNull Block blockIn, @NotNull BlockPos fromPos, boolean isMoving)
    {
        boolean isPowered = levelIn.isReceivingRedstonePower(pos);

        if (isPowered != state.get(POWERED))
        {
            if (isPowered && levelIn.getBlockEntity(pos) instanceof RemotePedestalTileEntity tileEntity)
            {
                tileEntity.onPowered();
            }

            levelIn.setBlockState(pos, state.with(POWERED, isPowered), 3);
            updateColor(levelIn, pos, pos.down());
        }
    }

    @Override
    public void onPlaced(@NotNull World world, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity entity, ItemStack stack)
    {
        super.onPlaced(world, pos, state, entity, stack);
        updateColor(world, pos, pos.down());
    }

    @Override
    public @NotNull BlockState getStateForNeighborUpdate(@NotNull BlockState stateIn, Direction facing, @NotNull BlockState facingState, @NotNull WorldAccess levelIn, @NotNull BlockPos currentPos, @NotNull BlockPos facingPos)
    {
        if (facing.equals(Direction.DOWN) && levelIn instanceof World world)
            updateColor(world, currentPos, facingPos);
        return stateIn;
    }

    public void updateColor(World levelIn, BlockPos currentPos, BlockPos facingPos)
    {
        if (levelIn.getBlockState(facingPos).getBlock() instanceof InkwellBlock)
            setColor(levelIn, currentPos, ColorUtils.getInkColorOrInverted(levelIn, facingPos));
    }

    @Override
    public boolean hasComparatorOutput(@NotNull BlockState p_149740_1_)
    {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, @NotNull World world, @NotNull BlockPos pos)
    {
        if (state.get(POWERED) && world.getBlockEntity(pos) instanceof RemotePedestalTileEntity te)
            return te.getSignal();

        return 0;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockView level, BlockPos pos, @Nullable Direction side)
    {
        return true;
    }

    @Override
    public boolean canClimb()
    {
        return false;
    }

    @Override
    public boolean canSwim()
    {
        return false;
    }

    @Override
    public boolean canDamage()
    {
        return false;
    }

    @Override
    public boolean canRemoteColorChange(World world, BlockPos pos, InkColor color, InkColor newColor)
    {
        RemotePedestalTileEntity te = (RemotePedestalTileEntity) world.getBlockEntity(pos);
        if (!te.isEmpty() && te.getStack(0).getItem() instanceof IColoredItem)
            return ColorUtils.getInkColor(te.getStack(0)) != newColor;
        return false;
    }

    @Override
    public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
    {
        if (world.getBlockEntity(pos) instanceof RemotePedestalTileEntity te)
        {
            if (!te.isEmpty() && te.getStack(0).getItem() instanceof IColoredItem)
            {
                ItemStack stack = te.getStack(0);
                ColorUtils.setColorLocked(stack, true);
                if (ColorUtils.getInkColor(stack) != newColor)
                {
                    ColorUtils.setInkColor(stack, newColor);
                    world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean setColor(World world, BlockPos pos, InkColor color)
    {
        if (!(world.getBlockEntity(pos) instanceof RemotePedestalTileEntity te))
            return false;
        te.setColor(color);
        world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
        return true;
    }

    @Override
    public boolean remoteInkClear(World world, BlockPos pos)
    {
        return false;
    }

    @Override
    public void onStateReplaced(BlockState state, @NotNull World world, @NotNull BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.isOf(newState.getBlock()))
        {
            if (world.getBlockEntity(pos) instanceof RemotePedestalTileEntity)
            {
                ItemScatterer.onStateReplaced(state, newState, world, pos);
                world.updateNeighbors(pos, this);
            }

            super.onStateReplaced(state, world, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return SplatcraftTileEntities.remotePedestalTileEntity.get().instantiate(pos, state);
    }
}
