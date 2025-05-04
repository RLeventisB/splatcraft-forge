package net.splatcraft.items.remotes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class RemoteItem extends Item implements CommandSource
{
	public static final List<RemoteItem> remotes = new ArrayList<>();
	public static final Collection<ServerPlayer> ALL_TARGETS = new ArrayList<>();
	protected static final Style TARGETS_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_BLUE).withItalic(true);
	protected final int totalModes;
	public RemoteItem(Properties settings)
	{
		this(settings, 1);
	}
	public RemoteItem(Properties settings, int totalModes)
	{
		super(settings.component(SplatcraftComponents.REMOTE_INFO, SplatcraftComponents.RemoteInfo.DEFAULT));
		remotes.add(this);

		this.totalModes = totalModes;
	}
	public static SplatcraftComponents.RemoteInfo getInfo(ItemStack stack)
	{
		return stack.get(SplatcraftComponents.REMOTE_INFO);
	}
	private static void setInfo(ItemStack stack, SplatcraftComponents.RemoteInfo info)
	{
		stack.set(SplatcraftComponents.REMOTE_INFO, info);
	}
	public static int getRemoteMode(ItemStack stack)
	{
		return getInfo(stack).modeIndex();
	}
	public static void setRemoteMode(ItemStack stack, int mode)
	{
		setInfo(stack, getInfo(stack).setModeIndex(mode));
	}
	public static int cycleRemoteMode(ItemStack stack)
	{
		int mode = getRemoteMode(stack) + 1;
		if (stack.getItem() instanceof RemoteItem item)
		{
			mode %= item.totalModes;
		}
		setRemoteMode(stack, mode);
		return mode;
	}
	public static boolean hasCoordSet(ItemStack stack)
	{
		SplatcraftComponents.RemoteInfo info = getInfo(stack);
		return info.stageId().isPresent() || (info.pointA().isPresent() && info.pointB().isPresent());
	}
	public static Tuple<BlockPos, BlockPos> getCoordSet(ItemStack stack)
	{
		if (!hasCoordSet(stack))
			return null;
		SplatcraftComponents.RemoteInfo info = getInfo(stack);

		if (info.stageId().isPresent())
		{
			Stage stage = SaveInfoCapability.get().stages().get(info.stageId().get());
			if (stage == null)
				return null;

			return new Tuple<>(stage.cornerA, stage.cornerB);
		}

		return new Tuple<>(info.pointA().get(), info.pointB().get());
	}
	public static boolean addCoords(Level world, ItemStack stack, BlockPos pos)
	{
		if (hasCoordSet(stack))
			return false;

		SplatcraftComponents.RemoteInfo info = getInfo(stack);

		if (info.worldKey().isEmpty())
			info = info.setWorldKey(world.dimension());
		else if (!world.equals(getLevel(world, stack)))
			return false;

		setInfo(stack, info.setPoint(pos));

		return true;
	}
	public static Level getLevel(Level world, ItemStack stack)
	{
		SplatcraftComponents.RemoteInfo info = getInfo(stack);

		Level result = world.getServer().getLevel(
			info.stageId().isPresent() ?
				SaveInfoCapability.get().stages().get(info.stageId().get()).worldKey :
				info.worldKey().get());

		return result == null ? world : result;
	}
	public static RemoteResult createResult(boolean success, Component output)
	{
		return new RemoteResult(success, output);
	}
	public ClampedItemPropertyFunction getActiveProperty()
	{
		return (stack, level, entity, seed) -> hasCoordSet(stack) ? 1 : 0;
	}
	public ClampedItemPropertyFunction getModeProperty()
	{
		return (stack, level, entity, seed) -> getRemoteMode(stack);
	}
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag type)
	{
		super.appendHoverText(stack, context, tooltip, type);

		SplatcraftComponents.RemoteInfo info = getInfo(stack);

		if (info.stageId().isEmpty() || SaveInfoCapability.get().stages().containsKey(info.stageId().get()))
		{
			if (hasCoordSet(stack))
			{
				Tuple<BlockPos, BlockPos> set = getCoordSet(stack);
				tooltip.add(Component.translatable("item.remote.coords.b", set.getA().getX(), set.getA().getY(), set.getA().getZ(),
					set.getB().getX(), set.getB().getY(), set.getB().getZ()));
			}
			else if (info.pointA().isPresent())
			{
				BlockPos pos = info.pointA().get();
				tooltip.add(Component.translatable("item.remote.coords.a", pos.getX(), pos.getY(), pos.getZ()));
			}
		}
		else
			tooltip.add(Component.translatable("item.remote.coords.invalid").setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withItalic(true)));

		if (info.targets().isPresent() && !info.targets().get().isEmpty())
			tooltip.add(ComponentUtils.mergeStyles(Component.literal(info.targets().get()), TARGETS_STYLE));
	}
	@Override
	public @NotNull InteractionResult useOn(UseOnContext context)
	{
		if (context.getLevel().isClientSide)
		{
			return hasCoordSet(context.getItemInHand()) ? InteractionResult.PASS : InteractionResult.SUCCESS;
		}

		if (addCoords(context.getLevel(), context.getItemInHand(), context.getClickedPos()))
		{
			SplatcraftComponents.RemoteInfo info = getInfo(context.getItemInHand());
			String key = info.pointB().isPresent() ? "b" : "a";
			BlockPos pos = context.getClickedPos();

			context.getPlayer().displayClientMessage(Component.translatable("status.coord_set." + key, pos.getX(), pos.getY(), pos.getZ()), true);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level levelIn, Player playerIn, @NotNull InteractionHand handIn)
	{
		ItemStack stack = playerIn.getItemInHand(handIn);
		int mode = getRemoteMode(stack);

		if (playerIn.isShiftKeyDown() && totalModes > 1)
		{
			mode = cycleRemoteMode(stack);
			String statusMsg = getDescriptionId() + ".mode." + mode;

			if (levelIn.isClientSide && I18n.exists(statusMsg))
			{
				playerIn.displayClientMessage(Component.translatable("status.remote_mode", Component.translatable(statusMsg)), true);
			}
		}
		else if (hasCoordSet(stack) && !levelIn.isClientSide)
		{
			RemoteResult remoteResult = onRemoteUse(levelIn, stack, ColorUtils.getEntityColor(playerIn), playerIn.position(), playerIn);

			if (remoteResult.getOutput() != null)
			{
				playerIn.displayClientMessage(remoteResult.getOutput(), true);
			}
			levelIn.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), SplatcraftSounds.remoteUse, SoundSource.BLOCKS, 0.8f, 1);
			return new InteractionResultHolder<>(remoteResult.wasSuccessful() ? InteractionResult.SUCCESS : InteractionResult.FAIL, stack);
		}

		return super.use(levelIn, playerIn, handIn);
	}
	public abstract RemoteResult onRemoteUse(Level usedOnWorld, BlockPos posA, BlockPos posB, ItemStack stack, InkColor colorIn, int mode, Collection<ServerPlayer> targets);
	public RemoteResult onRemoteUse(Level usedOnWorld, ItemStack stack, InkColor colorIn, Vec3 pos, Entity user)
	{
		SplatcraftComponents.RemoteInfo info = getInfo(stack);
		Tuple<BlockPos, BlockPos> coordSet = getCoordSet(stack);

		if (coordSet == null)
			return new RemoteResult(false, Component.translatable("status.remote.undefined_area"));

		Collection<ServerPlayer> targets = ALL_TARGETS;

		if (info.targets().isPresent() && !info.targets().get().isEmpty())
			try
			{
				targets = EntityArgument.players().parse(new StringReader(info.targets().get())).findPlayers(createCommandSourceStack(stack, (ServerLevel) usedOnWorld, pos, user));
			}
			catch (CommandSyntaxException e)
			{
				return new RemoteResult(false, Component.literal(e.getMessage()));
			}

		return onRemoteUse(usedOnWorld, coordSet.getA(), coordSet.getB(), stack, colorIn, getRemoteMode(stack), targets);
	}
	public CommandSourceStack createCommandSourceStack(ItemStack stack, ServerLevel level, Vec3 pos, Entity user)
	{
		return new CommandSourceStack(this, pos, Vec2.ZERO, level, 2, getName(stack).toString(), getName(stack), level.getServer(), user);
	}
	@Override
	public void sendSystemMessage(@NotNull Component p_145747_1_)
	{

	}
	@Override
	public boolean acceptsSuccess()
	{
		return false;
	}
	@Override
	public boolean acceptsFailure()
	{
		return false;
	}
	@Override
	public boolean shouldInformAdmins()
	{
		return false;
	}
	public static class RemoteResult
	{
		boolean success;
		Component output;
		int commandResult = 0;
		int comparatorResult = 0;
		public RemoteResult(boolean success, Component output)
		{
			this.success = success;
			this.output = output;
		}
		public RemoteResult setIntResults(int commandResult, int comparatorResult)
		{
			this.commandResult = commandResult;
			this.comparatorResult = comparatorResult;
			return this;
		}
		public int getCommandResult()
		{
			return commandResult;
		}
		public int getComparatorResult()
		{
			return comparatorResult;
		}
		public boolean wasSuccessful()
		{
			return success;
		}
		public Component getOutput()
		{
			return output;
		}
	}
}
