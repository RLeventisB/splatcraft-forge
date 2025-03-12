package net.splatcraft.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.dummys.ISplatcraftForgeItemDummy;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendJumpLureDataPacket;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JumpLureItem extends Item implements IColoredItem, ISplatcraftForgeItemDummy
{
	public JumpLureItem()
	{
		super(new Item.Properties().stacksTo(1));
		SplatcraftItems.inkColoredItems.add(this);
	}
	public static void activate(ServerPlayer player, UUID targetUUID, InkColor color)
	{
		Vec3 target;
		if (targetUUID == null)
		{
			BlockPos spawnPos = SuperJumpCommand.getSpawnPadPos(player);
			if (spawnPos != null)
			{
				target = new Vec3(spawnPos.getCenter().x(), spawnPos.getY() + SuperJumpCommand.blockHeight(spawnPos, player.level()), spawnPos.getCenter().z());
				if (!SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.GLOBAL_SUPERJUMPING) && !SuperJumpCommand.canSuperJumpTo(player, target))
				{
					player.sendSystemMessage(Component.literal("Spawn Pad outside of stage bounds!")); //TODO better feedback
					return;
				}
			}
			else
			{
				player.sendSystemMessage(Component.literal("No valid Spawn Pad was found!")); //TODO better feedback
				return;
			}
		}
		else
		{
			Player targetPlayer = player.level().getPlayerByUUID(targetUUID);
			
			if (targetPlayer == null || !hasMatchingLure(targetPlayer, color) || (!SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.GLOBAL_SUPERJUMPING)
				&& !SuperJumpCommand.canSuperJumpTo(player, targetPlayer.position())))
			{
				player.sendSystemMessage(Component.literal("A communication error has occurred.")); //TODO better feedback
				// this error message is funny af
				return;
			}
			else
				target = targetPlayer.position();
		}
		SuperJumpCommand.superJump(player, target);
	}
	public static boolean hasMatchingLure(Player targetPlayer, InkColor color)
	{
		for (int i = 0; i < targetPlayer.getInventory().getContainerSize(); i++)
			if (targetPlayer.getInventory().getItem(i).getItem() instanceof JumpLureItem &&
				ColorUtils.colorEquals(targetPlayer.level(), targetPlayer.blockPosition(), color, ColorUtils.getEffectiveColor(targetPlayer.getInventory().getItem(i))))
				return true;
		return false;
	}
	public static List<? extends Player> getAvailableCandidates(Player player, InkColor color)
	{
		ArrayList<Player> players = new ArrayList<>();
		if (SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.GLOBAL_SUPERJUMPING))
		{
			players.addAll(player.level().players());
		}
		else
		{
			ArrayList<Stage> stages = Stage.getStagesForPosition(player.level(), player.position());
			for (Stage stage : stages)
				players.addAll(player.level().getEntitiesOfClass(Player.class, stage.getBounds(), v -> true));
		}
		players.removeIf(target ->
			player.equals(target) || !hasMatchingLure(target, color)
				&& !SuperJumpCommand.canSuperJumpTo(player, target.position()));
		return players;
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag type)
	{
		super.appendHoverText(stack, context, tooltip, type);
		if (I18n.exists(getDescriptionId() + ".tooltip"))
			tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
		boolean inverted = ColorUtils.isInverted(stack);
		if (ColorUtils.isColorLocked(stack))
		{
			tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
			if (inverted)
				tooltip.add(Component.translatable("item.splatcraft.tooltip.inverted").setStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.DARK_PURPLE)));
		}
		else
			tooltip.add(Component.translatable("item.splatcraft.tooltip.matches_color" + (inverted ? ".inverted" : "")).withStyle(ChatFormatting.GRAY));
	}
	@Override
	public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity user)
	{
		return 72000;
	}
	@OnlyIn(Dist.CLIENT)
	private void releaseLure(LivingEntity entity)
	{
		if (entity.equals(ClientUtils.getClientPlayer()))
			JumpLureHudHandler.releaseLure();
	}
	@Override
	public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack)
	{
		return UseAnim.SPEAR;
	}
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand)
	{
		if (world.isClientSide())
		{
			JumpLureHudHandler.clickedThisFrame = false;
			return super.use(world, player, hand);
		}
		
		InkColor color = ColorUtils.getEffectiveColor(player.getItemInHand(hand));
		ArrayList<UUID> players = new ArrayList<>(getAvailableCandidates(player, color).stream().map(Entity::getUUID).toList());
		
		ServerPlayer serverPlayer = (ServerPlayer) player;
		
		BlockPos spawnPadPos = SuperJumpCommand.getSpawnPadPos(serverPlayer);
		
		if (!SplatcraftGameRules.getLocalizedRule(world, player.blockPosition(), SplatcraftGameRules.GLOBAL_SUPERJUMPING) && !SuperJumpCommand.canSuperJumpTo(player, new Vec3(spawnPadPos.getX(), spawnPadPos.getY(), spawnPadPos.getZ())))
			spawnPadPos = null;
		
		if (spawnPadPos == null && players.isEmpty())
		{
			serverPlayer.sendSystemMessage(Component.translatable("status.no_superjump_targets").withStyle(ChatFormatting.RED), true);
			return super.use(world, player, hand);
		}
		SplatcraftPacketHandler.sendToPlayer(new SendJumpLureDataPacket(color, spawnPadPos != null,
			players, spawnPadPos), serverPlayer);
		
		player.startUsingItem(hand);
		return super.use(world, player, hand);
	}
	@Override
	public void releaseUsing(@NotNull ItemStack stack, @NotNull Level world, @NotNull LivingEntity entity, int useTime)
	{
		super.releaseUsing(stack, world, entity, useTime);
		if (world.isClientSide())
			releaseLure(entity);
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull Level world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
		
		if (entity instanceof Player player)
		{
			if (!ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getEntityColor(player)
				&& EntityInfoCapability.hasCapability(player))
				ColorUtils.withInkColor(stack, ColorUtils.getEntityColor(player));
		}
	}
	@Override
	public boolean phOnEntityItemUpdate(ItemStack stack, ItemEntity entity)
	{
		BlockPos pos = entity.blockPosition().below();
		
		if (entity.level().getBlockState(pos).getBlock() instanceof InkwellBlock)
		{
			if (ColorUtils.getInkColor(stack) != ColorUtils.getEffectiveColor(entity.level(), pos))
			{
				ColorUtils.withInkColor(entity.getItem(), ColorUtils.getEffectiveColor(entity.level(), pos));
				ColorUtils.withColorLocked(entity.getItem(), true);
			}
		}
		else if ((!(stack.getItem() instanceof SubWeaponItem) || !SubWeaponItem.singleUse(stack))
			&& InkedBlock.causesClear(entity.level(), pos, entity.level().getBlockState(pos)) && ColorUtils.getInkColor(stack).getColor() != 0xFFFFFF)
		{
			ColorUtils.withInkColor(stack, InkColor.constructOrReuse(0xFFFFFF));
			ColorUtils.withColorLocked(stack, false);
		}
		return false;
	}
}