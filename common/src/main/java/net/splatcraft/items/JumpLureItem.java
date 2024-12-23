package net.splatcraft.items;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.blocks.InkwellBlock;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.SubWeaponItem;
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
		super(new Item.Settings().maxCount(1));
		SplatcraftItems.inkColoredItems.add(this);
	}
	public static void activate(ServerPlayerEntity player, UUID targetUUID, InkColor color)
	{
		Vec3d target;
		if (targetUUID == null)
		{
			BlockPos spawnPos = SuperJumpCommand.getSpawnPadPos(player);
			if (spawnPos != null)
			{
				target = new Vec3d(spawnPos.toCenterPos().getX(), spawnPos.getY() + SuperJumpCommand.blockHeight(spawnPos, player.getWorld()), spawnPos.toCenterPos().getZ());
				if (!SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.getBlockPos(), SplatcraftGameRules.GLOBAL_SUPERJUMPING) && !SuperJumpCommand.canSuperJumpTo(player, target))
				{
					player.sendMessage(Text.literal("Spawn Pad outside of stage bounds!")); //TODO better feedback
					return;
				}
			}
			else
			{
				player.sendMessage(Text.literal("No valid Spawn Pad was found!")); //TODO better feedback
				return;
			}
		}
		else
		{
			PlayerEntity targetPlayer = player.getWorld().getPlayerByUuid(targetUUID);
			
			if (targetPlayer == null || !hasMatchingLure(targetPlayer, color) || (!SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.getBlockPos(), SplatcraftGameRules.GLOBAL_SUPERJUMPING)
				&& !SuperJumpCommand.canSuperJumpTo(player, targetPlayer.getPos())))
			{
				player.sendMessage(Text.literal("A communication error has occurred.")); //TODO better feedback
				// this error message is funny af
				return;
			}
			else
				target = targetPlayer.getPos();
		}
		SuperJumpCommand.superJump(player, target);
	}
	public static boolean hasMatchingLure(PlayerEntity targetPlayer, InkColor color)
	{
		for (int i = 0; i < targetPlayer.getInventory().size(); i++)
			if (targetPlayer.getInventory().getStack(i).getItem() instanceof JumpLureItem &&
				ColorUtils.colorEquals(targetPlayer.getWorld(), targetPlayer.getBlockPos(), color, ColorUtils.getInkColorOrInverted(targetPlayer.getInventory().getStack(i))))
				return true;
		return false;
	}
	public static List<? extends PlayerEntity> getAvailableCandidates(PlayerEntity player, InkColor color)
	{
		ArrayList<PlayerEntity> players = new ArrayList<>();
		if (SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.getBlockPos(), SplatcraftGameRules.GLOBAL_SUPERJUMPING))
		{
			players.addAll(player.getWorld().getPlayers());
		}
		else
		{
			ArrayList<Stage> stages = Stage.getStagesForPosition(player.getWorld(), player.getPos());
			for (Stage stage : stages)
				players.addAll(player.getWorld().getEntitiesByClass(PlayerEntity.class, stage.getBounds(), v -> true));
		}
		players.removeIf(target ->
			player.equals(target) || !hasMatchingLure(target, color)
				&& !SuperJumpCommand.canSuperJumpTo(player, target.getPos()));
		return players;
	}
	@Override
	public void appendTooltip(@NotNull ItemStack stack, @Nullable TooltipContext context, @NotNull List<Text> tooltip, @NotNull TooltipType type)
	{
		super.appendTooltip(stack, context, tooltip, type);
		if (I18n.hasTranslation(getTranslationKey() + ".tooltip"))
			tooltip.add(Text.translatable(getTranslationKey() + ".tooltip").formatted(Formatting.GRAY));
		boolean inverted = ColorUtils.isInverted(stack);
		if (ColorUtils.isColorLocked(stack))
		{
			tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));
			if (inverted)
				tooltip.add(Text.translatable("item.splatcraft.tooltip.inverted").setStyle(Style.EMPTY.withItalic(true).withColor(Formatting.DARK_PURPLE)));
		}
		else
			tooltip.add(Text.translatable("item.splatcraft.tooltip.matches_color" + (inverted ? ".inverted" : "")).formatted(Formatting.GRAY));
	}
	@Override
	public int getMaxUseTime(ItemStack stack, LivingEntity user)
	{
		return 72000;
	}
	@Environment(EnvType.CLIENT)
	private void releaseLure(LivingEntity entity)
	{
		if (entity.equals(ClientUtils.getClientPlayer()))
			JumpLureHudHandler.releaseLure();
	}
	@Override
	public UseAction getUseAction(ItemStack stack)
	{
		return UseAction.SPEAR;
	}
	@Override
	public @NotNull TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity player, @NotNull Hand hand)
	{
		if (world.isClient())
		{
			JumpLureHudHandler.clickedThisFrame = false;
			return super.use(world, player, hand);
		}
		
		InkColor color = ColorUtils.getInkColorOrInverted(player.getStackInHand(hand));
		ArrayList<UUID> players = new ArrayList<>(getAvailableCandidates(player, color).stream().map(Entity::getUuid).toList());
		
		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
		
		BlockPos spawnPadPos = SuperJumpCommand.getSpawnPadPos(serverPlayer);
		
		if (!SplatcraftGameRules.getLocalizedRule(world, player.getBlockPos(), SplatcraftGameRules.GLOBAL_SUPERJUMPING) && !SuperJumpCommand.canSuperJumpTo(player, new Vec3d(spawnPadPos.getX(), spawnPadPos.getY(), spawnPadPos.getZ())))
			spawnPadPos = null;
		
		if (spawnPadPos == null && players.isEmpty())
		{
			((ServerPlayerEntity) player).sendMessageToClient(Text.translatable("status.no_superjump_targets").formatted(Formatting.RED), true);
			return super.use(world, player, hand);
		}
		SplatcraftPacketHandler.sendToPlayer(new SendJumpLureDataPacket(color, spawnPadPos != null,
			players, spawnPadPos), serverPlayer);
		
		player.setCurrentHand(hand);
		return super.use(world, player, hand);
	}
	@Override
	public void onStoppedUsing(@NotNull ItemStack stack, @NotNull World world, @NotNull LivingEntity entity, int useTime)
	{
		super.onStoppedUsing(stack, world, entity, useTime);
		if (world.isClient())
			releaseLure(entity);
	}
	@Override
	public void inventoryTick(@NotNull ItemStack stack, @NotNull World world, @NotNull Entity entity, int itemSlot, boolean isSelected)
	{
		super.inventoryTick(stack, world, entity, itemSlot, isSelected);
		
		if (entity instanceof PlayerEntity player)
		{
			if (!ColorUtils.isColorLocked(stack) && ColorUtils.getInkColor(stack) != ColorUtils.getEntityColor(player)
				&& EntityInfoCapability.hasCapability(player))
				ColorUtils.withInkColor(stack, ColorUtils.getEntityColor(player));
		}
	}
	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity)
	{
		BlockPos pos = entity.getBlockPos().down();
		
		if (entity.getWorld().getBlockState(pos).getBlock() instanceof InkwellBlock)
		{
			if (ColorUtils.getInkColor(stack) != ColorUtils.getInkColorOrInverted(entity.getWorld(), pos))
			{
				ColorUtils.withInkColor(entity.getStack(), ColorUtils.getInkColorOrInverted(entity.getWorld(), pos));
				ColorUtils.withColorLocked(entity.getStack(), true);
			}
		}
		else if ((!(stack.getItem() instanceof SubWeaponItem) || !SubWeaponItem.singleUse(stack))
			&& InkedBlock.causesClear(entity.getWorld(), pos, entity.getWorld().getBlockState(pos)) && ColorUtils.getInkColor(stack).getColor() != 0xFFFFFF)
		{
			ColorUtils.withInkColor(stack, InkColor.constructOrReuse(0xFFFFFF));
			ColorUtils.withColorLocked(stack, false);
		}
		return false;
	}
}