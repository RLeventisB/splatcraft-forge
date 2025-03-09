package net.splatcraft.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.neoforge.InkedBakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelBlockRenderer.class)
public class BlockModelRendererMixin
{
	@WrapOperation(method = "putQuadData", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/color/block/BlockColors;getColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;I)I"))
	public int splatcraft$modifyColorProvider(BlockColors instance, BlockState state, BlockAndTintGetter world, BlockPos pos, int tintIndex, Operation<Integer> original, @Local(argsOnly = true) BakedQuad quad)
	{
		if (quad instanceof InkedBakedQuad inkedQuad)
		{
			return inkedQuad.getTintIndex();
		}
		return original.call(instance, state, world, pos, tintIndex);
	}
	@WrapOperation(method = "putQuadData", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V"))
	public void splatcraft$makeInkGlowy(VertexConsumer instance, PoseStack.Pose matrixEntry, BakedQuad quad, float[] brightnesses, float red, float green, float blue, float f, int[] is, int i, boolean bl, Operation<Void> original)
	{
		original.call(instance, matrixEntry, quad, brightnesses, red, green, blue, f, is, i, bl);
	}
}
