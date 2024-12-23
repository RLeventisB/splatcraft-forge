package net.splatcraft.items.remotes;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.saveinfo.SaveInfoCapability;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class RemoteItem extends Item implements CommandOutput
{
	public static final List<RemoteItem> remotes = new ArrayList<>();
	public static final Collection<ServerPlayerEntity> ALL_TARGETS = new ArrayList<>();
	protected static final Style TARGETS_STYLE = Style.EMPTY.withColor(Formatting.DARK_BLUE).withItalic(true);
	protected final int totalModes;
	public RemoteItem(Settings settings)
	{
		this(settings, 1);
	}
	public RemoteItem(Settings settings, int totalModes)
	{
		super(settings.component(SplatcraftComponents.REMOTE_INFO, SplatcraftComponents.RemoteInfo.DEFAULT));
		remotes.add(this);
		
		this.totalModes = totalModes;
	}
	public static SplatcraftComponents.RemoteInfo getInfo(ItemStack stack)
	{
		return stack.get(SplatcraftComponents.REMOTE_INFO);
	}
	public static int getRemoteMode(ItemStack stack)
	{
		return getInfo(stack).modeIndex();
	}
	public static void setRemoteMode(ItemStack stack, int mode)
	{
		SplatcraftComponents.RemoteInfo info = getInfo(stack);
		info.setModeIndex(mode);
		stack.set(SplatcraftComponents.REMOTE_INFO, info);
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
	public static Pair<BlockPos, BlockPos> getCoordSet(ItemStack stack)
	{
		if (!hasCoordSet(stack))
			return null;
		SplatcraftComponents.RemoteInfo info = getInfo(stack);
		
		if (info.stageId().isPresent())
		{
			Stage stage = Platform.getEnv() == EnvType.CLIENT ?
				ClientUtils.clientStages.get(info.stageId().get()) : SaveInfoCapability.get().getStages().get(info.stageId().get());
			if (stage == null)
				return null;
			
			return new Pair<>(stage.cornerA, stage.cornerB);
		}
		
		return new Pair<>(info.pointA().get(), info.pointB().get());
	}
	public static boolean addCoords(World world, ItemStack stack, BlockPos pos)
	{
		if (hasCoordSet(stack))
			return false;
		
		SplatcraftComponents.RemoteInfo info = getInfo(stack);
		
		if (info.dimensionId().isEmpty())
			info.setDimensionId(world.getDimension().effects().toString());
		else if (!world.equals(getLevel(world, stack)))
			return false;
		
		if (info.pointA().isPresent())
		{
			info.setPointB(pos);
		}
		else
		{
			info.setPointA(pos);
		}
		
		return true;
	}
	public static World getLevel(World world, ItemStack stack)
	{
		SplatcraftComponents.RemoteInfo info = getInfo(stack);
		
		World result = world.getServer().getWorld(RegistryKeys.toWorldKey(RegistryKey.of(RegistryKeys.DIMENSION, info.stageId().isPresent() ?
			(world.isClient() ? ClientUtils.clientStages.get(info.stageId().get()) : SaveInfoCapability.get().getStages().get(info.stageId().get())).dimID
			: Identifier.of(info.dimensionId().get()))));
		
		return result == null ? world : result;
	}
	public static RemoteResult createResult(boolean success, Text output)
	{
		return new RemoteResult(success, output);
	}
	public ClampedModelPredicateProvider getActiveProperty()
	{
		return (stack, level, entity, seed) -> hasCoordSet(stack) ? 1 : 0;
	}
	public ClampedModelPredicateProvider getModeProperty()
	{
		return (stack, level, entity, seed) -> getRemoteMode(stack);
	}
	@Override
	public void appendTooltip(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Text> tooltip, @NotNull TooltipType type)
	{
		super.appendTooltip(stack, context, tooltip, type);
		
		SplatcraftComponents.RemoteInfo info = getInfo(stack);
		
		if (info.stageId().isEmpty() || ClientUtils.clientStages.containsKey(info.stageId().get()))
		{
			if (hasCoordSet(stack))
			{
				Pair<BlockPos, BlockPos> set = getCoordSet(stack);
				tooltip.add(Text.translatable("item.remote.coords.b", set.getLeft().getX(), set.getLeft().getY(), set.getLeft().getZ(),
					set.getRight().getX(), set.getRight().getY(), set.getRight().getZ()));
			}
			else if (info.pointA().isPresent())
			{
				BlockPos pos = info.pointA().get();
				tooltip.add(Text.translatable("item.remote.coords.a", pos.getX(), pos.getY(), pos.getZ()));
			}
		}
		else
			tooltip.add(Text.translatable("item.remote.coords.invalid").setStyle(Style.EMPTY.withColor(Formatting.RED).withItalic(true)));
		
		if (info.targets().isPresent() && !info.targets().get().isEmpty())
			tooltip.add(Texts.setStyleIfAbsent(Text.literal(info.targets().get()), TARGETS_STYLE));
	}
	@Override
	public ActionResult useOnBlock(ItemUsageContext context)
	{
		if (context.getWorld().isClient)
		{
			return hasCoordSet(context.getStack()) ? ActionResult.PASS : ActionResult.SUCCESS;
		}
		
		if (addCoords(context.getWorld(), context.getStack(), context.getBlockPos()))
		{
			SplatcraftComponents.RemoteInfo info = getInfo(context.getStack());
			String key = info.pointB().isPresent() ? "b" : "a";
			BlockPos pos = context.getBlockPos();
			
			context.getPlayer().sendMessage(Text.translatable("status.coord_set." + key, pos.getX(), pos.getY(), pos.getZ()), true);
			return ActionResult.SUCCESS;
		}
		return ActionResult.PASS;
	}
	@Override
	public TypedActionResult<ItemStack> use(@NotNull World levelIn, PlayerEntity playerIn, @NotNull Hand handIn)
	{
		ItemStack stack = playerIn.getStackInHand(handIn);
		int mode = getRemoteMode(stack);
		
		if (playerIn.isSneaking() && totalModes > 1)
		{
			mode = cycleRemoteMode(stack);
			String statusMsg = getTranslationKey() + ".mode." + mode;
			
			if (levelIn.isClient && I18n.hasTranslation(statusMsg))
			{
				playerIn.sendMessage(Text.translatable("status.remote_mode", Text.translatable(statusMsg)), true);
			}
		}
		else if (hasCoordSet(stack) && !levelIn.isClient)
		{
			RemoteResult remoteResult = onRemoteUse(levelIn, stack, ColorUtils.getEntityColor(playerIn), playerIn.getPos(), playerIn);
			
			if (remoteResult.getOutput() != null)
			{
				playerIn.sendMessage(remoteResult.getOutput(), true);
			}
			levelIn.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), SplatcraftSounds.remoteUse, SoundCategory.BLOCKS, 0.8f, 1);
			return new TypedActionResult<>(remoteResult.wasSuccessful() ? ActionResult.SUCCESS : ActionResult.FAIL, stack);
		}
		
		return super.use(levelIn, playerIn, handIn);
	}
	public abstract RemoteResult onRemoteUse(World usedOnWorld, BlockPos posA, BlockPos posB, ItemStack stack, InkColor colorIn, int mode, Collection<ServerPlayerEntity> targets);
	public RemoteResult onRemoteUse(World usedOnWorld, ItemStack stack, InkColor colorIn, Vec3d pos, Entity user)
	{
		SplatcraftComponents.RemoteInfo info = getInfo(stack);
		Pair<BlockPos, BlockPos> coordSet = getCoordSet(stack);
		
		if (coordSet == null)
			return new RemoteResult(false, Text.translatable("status.remote.undefined_area"));
		
		Collection<ServerPlayerEntity> targets = ALL_TARGETS;
		
		if (info.targets().isPresent() && !info.targets().get().isEmpty())
			try
			{
				targets = EntityArgumentType.players().parse(new StringReader(info.targets().get())).getPlayers(createCommandSourceStack(stack, (ServerWorld) usedOnWorld, pos, user));
			}
			catch (CommandSyntaxException e)
			{
				return new RemoteResult(false, Text.literal(e.getMessage()));
			}
		
		return onRemoteUse(usedOnWorld, coordSet.getLeft(), coordSet.getRight(), stack, colorIn, getRemoteMode(stack), targets);
	}
	public ServerCommandSource createCommandSourceStack(ItemStack stack, ServerWorld level, Vec3d pos, Entity user)
	{
		return new ServerCommandSource(this, pos, Vec2f.ZERO, level, 2, getName(stack).toString(), getName(stack), level.getServer(), user);
	}
	@Override
	public void sendMessage(@NotNull Text p_145747_1_)
	{
	
	}
	@Override
	public boolean shouldReceiveFeedback()
	{
		return false;
	}
	@Override
	public boolean shouldTrackOutput()
	{
		return false;
	}
	@Override
	public boolean shouldBroadcastConsoleToOps()
	{
		return false;
	}
	public static class RemoteResult
	{
		boolean success;
		Text output;
		int commandResult = 0;
		int comparatorResult = 0;
		public RemoteResult(boolean success, Text output)
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
		public Text getOutput()
		{
			return output;
		}
	}
}
