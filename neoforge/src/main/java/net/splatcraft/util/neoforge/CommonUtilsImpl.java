package net.splatcraft.util.neoforge;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import net.splatcraft.client.renderer.InkSquidRenderer;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class CommonUtilsImpl
{
	public static <T> int @Nullable [] findMatches(List<T> inputs, List<? extends Predicate<T>> tests)
	{
		return RecipeMatcher.findMatches(inputs, tests);
	}
	public static void doPlayerSquidForgeEvent(AbstractClientPlayerEntity player, InkSquidRenderer squidRenderer, float g, MatrixStack matrixStack, VertexConsumerProvider consumerProvider, int i)
	{
		NeoForge.EVENT_BUS.post(new RenderLivingEvent.Post<>(player, squidRenderer, g, matrixStack, consumerProvider, i));
	}
	public static CommonUtils.InteractionEventResultDummy doPlayerUseItemForgeEvent(int i, KeyBinding useKey, Hand hand)
	{
		InputEvent.InteractionKeyMappingTriggered eventResult = ClientHooks.onClickInput(i, useKey, hand);
		return new CommonUtils.InteractionEventResultDummy(eventResult.shouldSwingHand(), eventResult.isCanceled());
	}
	public static ItemStack callGetPickItemStack(BlockState state, HitResult target, WorldView level, BlockPos pos, PlayerEntity player)
	{
		return state.getBlock().getCloneItemStack(state, target, level, pos, player);
	}
	public static boolean callCanHarvestBlock(BlockState state, BlockView level, BlockPos pos, PlayerEntity player)
	{
		return state.getBlock().canHarvestBlock(state, level, pos, player);
	}
}
