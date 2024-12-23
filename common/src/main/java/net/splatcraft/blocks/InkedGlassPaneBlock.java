package net.splatcraft.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkedGlassPaneBlock extends PaneBlock implements IColoredBlock, Waterloggable, BlockEntityProvider, ISplatcraftForgeBlockDummy
{
	public InkedGlassPaneBlock()
	{
		super(AbstractBlock.Settings.create().instrument(NoteBlockInstrument.HAT).solidBlock(SplatcraftBlocks::noRedstoneConduct).strength(0.3F).sounds(BlockSoundGroup.GLASS).nonOpaque());
		SplatcraftBlocks.inkColoredBlocks.add(this);
	}
	@Override
	public boolean hasSidedTransparency(@NotNull BlockState state)
	{
		return true;
	}
	@Override
	public Integer phGetBeaconColorMultiplier(BlockState state, WorldView level, BlockPos pos, BlockPos beaconPos)
	{
		return getColor(level, pos).getColor();
	}
	@Override
	public ItemStack phGetCloneItemStack(BlockState state, HitResult target, WorldView level, BlockPos pos, PlayerEntity player)
	{
		return ColorUtils.withColorLocked(ColorUtils.withInkColor(ISplatcraftForgeBlockDummy.super.phGetCloneItemStack(state, target, level, pos, player), getColor(level, pos)), true);
	}
	@Override
	public void onPlaced(@NotNull World world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, ItemStack stack)
	{
		if (stack.contains(SplatcraftComponents.ITEM_COLOR_DATA) && world.getBlockEntity(pos) instanceof InkColorTileEntity)
		{
			ColorUtils.withInkColor(world.getBlockEntity(pos), ColorUtils.getInkColor(stack));
		}
		super.onPlaced(world, pos, state, entity, stack);
	}
	@Override
	public BlockState getPlacementState(@NotNull ItemPlacementContext context)
	{
		return super.getPlacementState(context).with(WATERLOGGED, context.getWorld().getFluidState(context.getBlockPos()).getRegistryEntry() == Fluids.WATER);
	}
	@Override
	public @NotNull ItemStack getPickStack(@NotNull WorldView reader, @NotNull BlockPos pos, @NotNull BlockState state)
	{
		ItemStack stack = super.getPickStack(reader, pos, state);
		
		if (reader.getBlockEntity(pos) instanceof InkColorTileEntity)
			ColorUtils.withColorLocked(ColorUtils.withInkColor(stack, ColorUtils.getInkColor(reader.getBlockEntity(pos))), true);
		
		return stack;
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
	public InkColor getColor(WorldView world, BlockPos pos)
	{
		if (world.getBlockEntity(pos) instanceof InkColorTileEntity colorTileEntity)
		{
			return colorTileEntity.getInkColor();
		}
		return InkColor.INVALID;
	}
	@Override
	public boolean remoteColorChange(World world, BlockPos pos, InkColor newColor)
	{
		BlockState state = world.getBlockState(pos);
		if (world.getBlockEntity(pos) instanceof InkColorTileEntity colorTileEntity && colorTileEntity.getInkColor() != newColor)
		{
			colorTileEntity.setColor(newColor);
			world.updateListeners(pos, state, state, 2);
			return true;
		}
		return false;
	}
	@Override
	public boolean remoteInkClear(World world, BlockPos pos)
	{
		return false;
	}
	@Nullable
	@Override
	public BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.colorTileEntity.get().instantiate(pos, state);
	}
}
