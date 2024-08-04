package net.splatcraft.forge.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInk;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.forge.registries.SplatcraftItemGroups;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkWaxerItem extends Item
{
	public InkWaxerItem()
	{
		super(new Properties().durability(256));
		SplatcraftItemGroups.addGeneralItem(this);
	}
	// wait why does it work like this
	public void onBlockStartBreak(BlockPos pos, Level level, @Nullable Direction face)
	{
		if (InkBlockUtils.isInkedAny(level, pos))
		{
			ColorUtils.addInkDestroyParticle(level, pos, InkBlockUtils.getInkInFace(level, pos, face).color());
			
			SoundType soundType = SplatcraftSounds.SOUND_TYPE_INK;
			level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), soundType.getBreakSound(), SoundSource.PLAYERS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
			
			InkBlockUtils.clearInk(level, pos, face, true);
			
			// alternative behaviour where breaking an inked block only breaks the wax, making it not permanent
			/*
			
			ChunkInk.BlockEntry ink = worldInk.getInk(pos);
			ink.permanent = true;
			
			context.getLevel().levelEvent(context.getPlayer(), 3004, context.getClickedPos(), 0);
			
			 */
		}
	}
	@Override
	public @NotNull InteractionResult useOn(UseOnContext context)
	{
		ChunkInk worldInk = ChunkInkCapability.get(context.getLevel(), context.getClickedPos());
		
		if (worldInk.isInkedAny(context.getClickedPos()))
		{
			ChunkInk.BlockEntry ink = worldInk.getInk(context.getClickedPos());
			ink.permanent = true;
			
			context.getLevel().levelEvent(context.getPlayer(), 3003, context.getClickedPos(), 0);
			
			return InteractionResult.SUCCESS;
		}
		
		return super.useOn(context);
	}
	@Override
	public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level levelIn, @NotNull BlockPos pos, @NotNull Player player)
	{
		return false;
	}
	@Override
	public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state)
	{
		return 0;
	}
	@Override
	public boolean isValidRepairItem(@NotNull ItemStack toRepair, ItemStack repair)
	{
		return repair.getItem().equals(Items.HONEYCOMB) || super.isValidRepairItem(toRepair, repair);
	}
}
