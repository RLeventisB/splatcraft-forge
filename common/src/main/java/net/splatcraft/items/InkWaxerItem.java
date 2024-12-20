package net.splatcraft.items;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.RelativeBlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InkWaxerItem extends Item
{
    public InkWaxerItem()
    {
        super(new Settings().maxDamage(256));
    }

    // wait why does it work like this
    public void onBlockStartBreak(BlockPos pos, World world, @Nullable Direction face)
    {
        if (InkBlockUtils.isInkedAny(world, pos))
        {
            ColorUtils.addInkDestroyParticle(world, pos, InkBlockUtils.getInkInFace(world, pos, face).color());

            BlockSoundGroup soundType = SplatcraftSounds.SOUND_TYPE_INK;
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), soundType.getBreakSound(), SoundCategory.PLAYERS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

            InkBlockUtils.clearInk(world, pos, face, true);

            // alternative behaviour where breaking an inked block only breaks the wax, making it not permanent
			/*

			ChunkInk.BlockEntry ink = worldInk.getInk(pos);
			ink.permanent = true;

			context.getWorld().levelEvent(context.getPlayer(), 3004, context.getClickedPos(), 0);

			 */
        }
    }

    @Override
    public @NotNull ActionResult useOnBlock(ItemUsageContext context)
    {
        BlockPos clickedPos = context.getBlockPos();
        RelativeBlockPos offset = RelativeBlockPos.fromAbsolute(clickedPos);
        ChunkInk worldInk = ChunkInkCapability.get(context.getWorld(), clickedPos);
        ChunkInk.BlockEntry ink = worldInk.getInk(offset);
        if (ink != null && ink.inmutable)
            return ActionResult.FAIL;
        worldInk.markInmutable(offset);

        context.getWorld().syncWorldEvent(context.getPlayer(), 3003, clickedPos, 0);
        ChunkInkHandler.addInkToUpdate(context.getWorld(), clickedPos);
        BlockState state = context.getWorld().getBlockState(clickedPos);
        context.getWorld().updateListeners(clickedPos, state, state, 0);

        return ActionResult.SUCCESS;
    }

    @Override
    public boolean canMine(@NotNull BlockState state, @NotNull World world, @NotNull BlockPos pos, @NotNull PlayerEntity player)
    {
        return false;
    }

    @Override
    public float getMiningSpeed(@NotNull ItemStack stack, @NotNull BlockState state)
    {
        return 0;
    }

    @Override
    public boolean canRepair(@NotNull ItemStack toRepair, ItemStack repair)
    {
        return repair.getItem().equals(Items.HONEYCOMB) || super.canRepair(toRepair, repair);
    }
}