package net.splatcraft.blocks;

import com.mojang.serialization.MapCodec;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkVatTileEntity;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkVatBlock extends BlockWithEntity implements IColoredBlock
{
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final MapCodec<InkVatBlock> CODEC = createCodec(InkVatBlock::new);

    public InkVatBlock(Settings setting)
    {
        super(setting);
        SplatcraftBlocks.inkColoredBlocks.add(this);

        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(ACTIVE, false).with(POWERED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, ACTIVE, POWERED);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context)
    {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public ItemActionResult onUseWithItem(ItemStack stack, @NotNull BlockState state, World levelIn, @NotNull BlockPos pos, @NotNull PlayerEntity player, @NotNull Hand handIn, @NotNull BlockHitResult hit)
    {
        if (levelIn.isClient)
        {
            return ItemActionResult.SUCCESS;
        }

        if (levelIn.getBlockEntity(pos) instanceof InkVatTileEntity inkVatTile && player instanceof ServerPlayerEntity serverPlayer)
        {
            MenuRegistry.openMenu(serverPlayer, inkVatTile);
            return ItemActionResult.SUCCESS;
        }

        return ItemActionResult.FAIL;
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
    public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
    {
        return false;
    }

    @Override
    public boolean remoteInkClear(World world, BlockPos pos)
    {
        return false;
    }

    @Override
    public InkColor getColor(WorldView world, BlockPos pos)
    {
        if (world.getBlockEntity(pos) instanceof InkVatTileEntity tileEntity)
        {
            return tileEntity.getColor();
        }
        return InkColor.INVALID;
    }

    @Override
    public boolean setColor(World world, BlockPos pos, InkColor color)
    {
        if (!(world.getBlockEntity(pos) instanceof InkVatTileEntity tileEntity))
        {
            return false;
        }
        tileEntity.setColor(color);
        world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 2);
        return true;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec()
    {
        return null;
    }

    @Override
    public @NotNull BlockRenderType getRenderType(@NotNull BlockState state)
    {
        return BlockRenderType.MODEL;
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, BlockRotation rot)
    {
        return state.with(FACING, rot.rotate(state.get(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, BlockMirror mirrorIn)
    {
        return state.rotate(mirrorIn.getRotation(state.get(FACING)));
    }

    @Override
    public void onPlaced(@NotNull World levelIn, @NotNull BlockPos pos, @NotNull BlockState state, LivingEntity placer, ItemStack stack)
    {
        if (stack.contains(DataComponentTypes.CUSTOM_NAME))
        {
            BlockEntity tileentity = levelIn.getBlockEntity(pos);
            if (tileentity instanceof InkVatTileEntity inkVatTile)
            {
                inkVatTile.setComponents(ComponentMap.builder().add(DataComponentTypes.CUSTOM_NAME, stack.get(DataComponentTypes.CUSTOM_NAME)).build());
            }
        }
    }

    @Override
    public void onStateReplaced(BlockState state, @NotNull World world, @NotNull BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (!state.isOf(newState.getBlock()))
        {
            BlockEntity tileentity = world.getBlockEntity(pos);
            if (tileentity instanceof InkVatTileEntity)
            {
                ItemScatterer.onStateReplaced(state, newState, world, pos);
                world.updateComparators(pos, this);
            }

            super.onStateReplaced(state, world, pos, newState, isMoving);
        }
    }

    @Override
    public boolean hasComparatorOutput(@NotNull BlockState state)
    {
        return true;
    }

    @Override
    public int getComparatorOutput(@NotNull BlockState blockState, World world, @NotNull BlockPos pos)
    {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }

    @Override
    public void neighborUpdate(BlockState state, World world, @NotNull BlockPos pos, @NotNull Block blockIn, @NotNull BlockPos fromPos, boolean isMoving)
    {
        boolean isPowered = world.isReceivingRedstonePower(pos);
        if (isPowered != state.get(POWERED))
        {
            if (isPowered && world.getBlockEntity(pos) instanceof InkVatTileEntity tileEntity)
            {
                tileEntity.onRedstonePulse();
            }

            world.setBlockState(pos, state.with(POWERED, isPowered), 3);
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return SplatcraftTileEntities.inkVatTileEntity.get().instantiate(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull World world, @NotNull BlockState state, @NotNull BlockEntityType<T> type)
    {
        return validateTicker(type, SplatcraftTileEntities.inkVatTileEntity.get(), InkVatTileEntity::tick);
    }
}
