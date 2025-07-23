package net.splatcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.util.InkBlockUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(AxeItem.class)
public class AxeWaxMixin
{
	@Inject(method = "useOn", at = @At(value = "INVOKE", target = "Ljava/util/Optional;isEmpty()Z"))
	public void splatcraft$stripInkWax(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir, @Local Level world, @Local BlockPos blockPos, @Local Optional optional)
	{
		if (optional.isEmpty())
		{
			ChunkInk.BlockEntry entry = InkBlockUtils.getInkBlock(world, blockPos);
			if (entry != null && entry.immutable)
			{
				entry.immutable = false;
				
				ChunkInkHandler.addInkToUpdate(world, blockPos);
				world.getChunk(blockPos).setUnsaved(true);
				world.playSound(context.getPlayer(), blockPos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
				world.levelEvent(context.getPlayer(), 3004, blockPos, 0);
			}
		}
	}
}
