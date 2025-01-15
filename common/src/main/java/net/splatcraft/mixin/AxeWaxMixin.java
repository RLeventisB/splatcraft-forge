package net.splatcraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.util.InkBlockUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(AxeItem.class)
public class AxeWaxMixin
{
	@Inject(method = "useOnBlock", at = @At(value = "INVOKE", target = "Ljava/util/Optional;isEmpty()Z"))
	public void splatcraft$stripInkWax(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir, @Local World world, @Local BlockPos blockPos, @Local Optional optional)
	{
		if (optional.isEmpty())
		{
			ChunkInk.BlockEntry entry = InkBlockUtils.getInkBlock(world, blockPos);
			if (entry != null && entry.immutable)
			{
				entry.immutable = false;
				world.getChunk(blockPos).setNeedsSaving(true);
				world.playSound(context.getPlayer(), blockPos, SoundEvents.ITEM_AXE_WAX_OFF, SoundCategory.BLOCKS, 1.0F, 1.0F);
				world.syncWorldEvent(context.getPlayer(), 3004, blockPos, 0);
			}
		}
	}
}
