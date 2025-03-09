package net.splatcraft.items;

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
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.RelativeBlockPos;
import org.jetbrains.annotations.NotNull;

public class InkWaxerItem extends Item
{
	public InkWaxerItem()
	{
		super(new Properties().durability(256));
	}
	// wait why does it work like this
	public void onBlockStartBreak(BlockPos pos, Level world, Direction face)
	{
		if (InkBlockUtils.isInkedAny(world, pos))
		{
			ColorUtils.addInkDestroyParticle(world, pos, InkBlockUtils.getInkInFace(world, pos, face).color());
			
			SoundType soundType = SplatcraftSounds.SOUND_TYPE_INK;
			world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), soundType.getBreakSound(), SoundSource.PLAYERS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
			
			InkBlockUtils.clearInk(world, pos, face, true);
			BlockState state = world.getBlockState(pos);
			world.sendBlockUpdated(pos, state, state, 0);
		}
	}
	@Override
	public @NotNull InteractionResult useOn(UseOnContext context)
	{
		BlockPos clickedPos = context.getClickedPos();
		RelativeBlockPos offset = RelativeBlockPos.fromAbsolute(clickedPos);
		ChunkInk worldInk = ChunkInkCapability.get(context.getLevel(), clickedPos);
		ChunkInk.BlockEntry ink = worldInk.getInk(offset);
		if (ink != null && ink.immutable)
			return InteractionResult.FAIL;
		worldInk.markInmutable(offset);
		
		context.getLevel().levelEvent(context.getPlayer(), 3003, clickedPos, 0);
		ChunkInkHandler.addInkToUpdate(context.getLevel(), clickedPos);
		BlockState state = context.getLevel().getBlockState(clickedPos);
		context.getLevel().sendBlockUpdated(clickedPos, state, state, 0);
		
		return InteractionResult.SUCCESS;
	}
	@Override
	public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player)
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