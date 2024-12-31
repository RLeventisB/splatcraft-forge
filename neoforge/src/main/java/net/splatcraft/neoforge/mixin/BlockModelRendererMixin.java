package net.splatcraft.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.splatcraft.neoforge.InkedBakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin
{
	@WrapOperation(method = "renderQuad", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/color/block/BlockColors;getColor(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/util/math/BlockPos;I)I"))
	public int splatcraft$modifyColorProvider(BlockColors instance, BlockState state, BlockRenderView world, BlockPos pos, int tintIndex, Operation<Integer> original, @Local(argsOnly = true) BakedQuad quad)
	{
		if (quad instanceof InkedBakedQuad inkedQuad)
		{
			return inkedQuad.getColorIndex();
		}
		return original.call(instance, state, world, pos, tintIndex);
	}
	@WrapOperation(method = "renderQuad", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;quad(Lnet/minecraft/client/util/math/MatrixStack$Entry;Lnet/minecraft/client/render/model/BakedQuad;[FFFFF[IIZ)V"))
	public void splatcraft$makeInkGlowy(VertexConsumer instance, MatrixStack.Entry matrixEntry, BakedQuad quad, float[] brightnesses, float red, float green, float blue, float f, int[] is, int i, boolean bl, Operation<Void> original)
	{
		original.call(instance, matrixEntry, quad, brightnesses, red, green, blue, f, is, i, bl);
	}
}
