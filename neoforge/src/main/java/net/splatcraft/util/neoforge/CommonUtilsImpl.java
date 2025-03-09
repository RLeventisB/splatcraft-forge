package net.splatcraft.util.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.renderer.InkSquidRenderer;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class CommonUtilsImpl
{
	public static final DeferredRegister<EntityDataSerializer<?>> DATA_TRACKER_REGISTRY = Splatcraft.deferredRegistryOf(NeoForgeRegistries.ENTITY_DATA_SERIALIZERS);
	public static <T> int @Nullable [] findMatches(List<T> inputs, List<? extends Predicate<T>> tests)
	{
		return RecipeMatcher.findMatches(inputs, tests);
	}
	public static void doPlayerSquidForgeEvent(AbstractClientPlayer player, InkSquidRenderer squidRenderer, float g, PoseStack matrixStack, MultiBufferSource consumerProvider, int i)
	{
		NeoForge.EVENT_BUS.post(new RenderLivingEvent.Post<>(player, squidRenderer, g, matrixStack, consumerProvider, i));
	}
	public static CommonUtils.InteractionEventResultDummy doPlayerUseItemForgeEvent(int i, KeyMapping useKey, InteractionHand hand)
	{
		InputEvent.InteractionKeyMappingTriggered eventResult = ClientHooks.onClickInput(i, useKey, hand);
		return new CommonUtils.InteractionEventResultDummy(eventResult.shouldSwingHand(), eventResult.isCanceled());
	}
	public static void doForgeEmptyClickEvent(LocalPlayer player, InteractionHand hand)
	{
		CommonHooks.onEmptyClick(player, hand);
	}
	public static ItemStack callGetPickItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player)
	{
		return state.getBlock().getCloneItemStack(state, target, level, pos, player);
	}
	public static boolean callCanHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player)
	{
		return state.getBlock().canHarvestBlock(state, level, pos, player);
	}
}
