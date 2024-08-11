package net.splatcraft.forge.blocks;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import net.splatcraft.forge.registries.SplatcraftTileEntities;
import net.splatcraft.forge.tileentities.InkColorTileEntity;
import net.splatcraft.forge.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public class InkStainedBlock extends Block implements IColoredBlock, EntityBlock
{
    public static final BooleanProperty COLORED = BooleanProperty.create("colored");

    public InkStainedBlock(Properties properties)
    {
        super(properties);
        SplatcraftBlocks.inkColoredBlocks.add(this);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state)
    {
        int color = getColor((Level) level, pos);
        if (color < 0)
            return ColorUtils.setInkColor(super.getCloneItemStack(level, pos, state), color);
        return ColorUtils.setColorLocked(ColorUtils.setInkColor(super.getCloneItemStack(level, pos, state), color), true);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, ItemStack stack)
    {
        if (stack.getTag() != null && level.getBlockEntity(pos) instanceof InkColorTileEntity)
        {
            ColorUtils.setInkColor(level.getBlockEntity(pos), ColorUtils.getInkColor(stack));
        }
        super.setPlacedBy(level, pos, state, entity, stack);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
    {
        return SplatcraftTileEntities.colorTileEntity.get().create(pos, state);
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

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context)
    {
        return super.getStateForPlacement(context);
    }

    @Override
    public boolean setColor(Level level, BlockPos pos, int color)
    {
        return IColoredBlock.super.setColor(level, pos, color);
    }

    @Override
    public int getColor(Level level, BlockPos pos)
    {
        if (level.getBlockEntity(pos) instanceof InkColorTileEntity te)
        {
            return te.getColor();
        }
        return -1;
    }

    @Override
    public boolean remoteColorChange(Level level, BlockPos pos, int newColor)
    {
        BlockState state = level.getBlockState(pos);

        if (level.getBlockEntity(pos) instanceof InkColorTileEntity te && te.getColor() != newColor)
        {
            te.setColor(newColor);
            level.sendBlockUpdated(pos, state, state, 2);
            return true;
        }
        return false;
    }

    @Override
    public boolean remoteInkClear(Level level, BlockPos pos)
    {
        return false;
    }

    public static class WithUninkedVariant extends InkStainedBlock
    {
        public WithUninkedVariant(Properties properties)
        {
            super(properties);

            registerDefaultState(defaultBlockState().setValue(COLORED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
        {
            builder.add(COLORED);
        }

        @Override
        public boolean remoteColorChange(Level level, BlockPos pos, int newColor)
        {
            if (!level.getBlockState(pos).getValue(COLORED))
                return false;

            return super.remoteColorChange(level, pos, newColor);
        }

        @Override
        public boolean setColor(Level level, BlockPos pos, int color)
        {
            level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(COLORED, color >= 0));
            return super.setColor(level, pos, color);
        }

        @Override
        public int getColor(Level level, BlockPos pos)
        {
            if (level.getBlockState(pos).getValue(COLORED))
                return super.getColor(level, pos);
            else return -1;
        }

        @Override
        public @Nullable BlockState getStateForPlacement(@NotNull BlockPlaceContext context)
        {
            BlockState state = super.getStateForPlacement(context);
            if (state == null) {
                return null;
            }

            return state.setValue(COLORED, ColorUtils.getInkColor(context.getItemInHand()) >= 0);
        }

        @Override
        public @NotNull List<ItemStack> getDrops(@NotNull BlockState p_60537_, LootParams.@NotNull Builder p_60538_) {
            return super.getDrops(p_60537_, p_60538_);
        }
    }
}