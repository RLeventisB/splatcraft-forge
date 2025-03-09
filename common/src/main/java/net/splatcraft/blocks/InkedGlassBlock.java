package net.splatcraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.HitResult;
import net.splatcraft.dummys.ISplatcraftForgeBlockDummy;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftTileEntities;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkedGlassBlock extends HalfTransparentBlock implements IColoredBlock, EntityBlock, ISplatcraftForgeBlockDummy
{
	public InkedGlassBlock(String name)
	{
		super(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.HAT).strength(0.3F).sound(SoundType.GLASS).noOcclusion()
			.isValidSpawn((state, level, pos, entity) -> false)
			.isRedstoneConductor((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false));
		SplatcraftBlocks.inkColoredBlocks.add(this);
	}
	@Override
	public boolean useShapeForLightOcclusion(@NotNull BlockState p_220074_1_)
	{
		return true;
	}
	@Override
	public Integer phGetBeaconColorMultiplier(BlockState state, LevelReader world, BlockPos pos, BlockPos beaconPos)
	{
		return getColor(world, pos).getColor();
	}
	//  ok guys i learned why were there two getPickStack methods one was a forge override oops
	@Override
	public ItemStack phGetCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player)
	{
		return ColorUtils.withColorLocked(ColorUtils.withInkColor(ISplatcraftForgeBlockDummy.super.phGetCloneItemStack(state, target, level, pos, player), getColor(level, pos)), true);
	}
	@Override
	public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, ItemStack stack)
	{
		if (stack.has(SplatcraftComponents.ITEM_COLOR_DATA) && world.getBlockEntity(pos) instanceof InkColorTileEntity)
		{
			ColorUtils.withInkColor(world.getBlockEntity(pos), ColorUtils.getEffectiveColor(stack));
		}
		super.setPlacedBy(world, pos, state, entity, stack);
	}
	@Override
	public @NotNull ItemStack getCloneItemStack(@NotNull LevelReader reader, @NotNull BlockPos pos, @NotNull BlockState state)
	{
		ItemStack stack = super.getCloneItemStack(reader, pos, state);
		
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
	public InkColor getColor(LevelReader world, BlockPos pos)
	{
		if (world.getBlockEntity(pos) instanceof InkColorTileEntity blockEntity)
		{
			return blockEntity.getInkColor();
		}
		return InkColor.INVALID;
	}
	@Override
	public boolean remoteColorChange(Level world, BlockPos pos, InkColor newColor)
	{
		BlockState state = world.getBlockState(pos);
		if (world.getBlockEntity(pos) instanceof InkColorTileEntity blockEntity && blockEntity.getInkColor() != newColor)
		{
			blockEntity.setColor(newColor);
			world.sendBlockUpdated(pos, state, state, 2);
			return true;
		}
		return false;
	}
	@Override
	public boolean remoteInkClear(Level world, BlockPos pos)
	{
		return false;
	}
	@Nullable
	@Override
	public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state)
	{
		return SplatcraftTileEntities.colorTileEntity.get().create(pos, state);
	}
}
