package net.splatcraft.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.blocks.IColoredBlock;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.client.particles.InkTerrainParticleData;
import net.splatcraft.data.InkColorGroups;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerColorPacket;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftStats;
import net.splatcraft.tileentities.InkColorTileEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class ColorUtils
{
	// actual heresy but i am a c# dev so public static!!!!!
	public static final Random random = new Random();
	public static boolean doesStackHaveColorData(ItemStack stack)
	{
		return stack.contains(SplatcraftComponents.ITEM_COLOR_DATA);
	}
	public static <T> T applyColorDataPredicate(ItemStack stack, Function<SplatcraftComponents.ItemColorData, T> getter, T fallback)
	{
		if (stack.contains(SplatcraftComponents.ITEM_COLOR_DATA))
			return getter.apply(stack.get(SplatcraftComponents.ITEM_COLOR_DATA));
		return fallback;
	}
	public static @NotNull InkColor getEntityColor(Entity entity)
	{
		if (entity instanceof LivingEntity living)
		{
			if (living == ClientUtils.getClientPlayer())
				return ClientUtils.getClientPlayerColor(living.getUuid());
			if (EntityInfoCapability.hasCapability(living))
				return EntityInfoCapability.get(living).getColor();
		}
		if (entity instanceof IColoredEntity coloredEntity)
			return coloredEntity.getColor();
		return InkColor.INVALID;
	}
	public static void setPlayerColor(PlayerEntity player, InkColor color, boolean updateClient)
	{
		if (EntityInfoCapability.hasCapability(player) && EntityInfoCapability.get(player).getColor() != color)
		{
			if (player instanceof ServerPlayerEntity serverPlayer)
				SplatcraftStats.CHANGE_INK_COLOR_TRIGGER.get().trigger(serverPlayer);
			
			EntityInfoCapability.get(player).setColor(color);
			ScoreboardHandler.updatePlayerScore(Stats.CUSTOM.getOrCreateStat(ScoreboardHandler.COLOR), player, color);
		}
		
		World world = player.getWorld();
		if (!world.isClient() && updateClient)
		{
			SplatcraftPacketHandler.sendToTrackersAndSelf(new PlayerColorPacket(player, color), player);
		}
	}
	public static void setPlayerColor(PlayerEntity player, InkColor color)
	{
		setPlayerColor(player, color, true);
	}
	public static boolean isInverted(ItemStack stack)
	{
		return applyColorDataPredicate(stack, SplatcraftComponents.ItemColorData::hasInvertedColor, false);
	}
	public static ItemStack withInvertedColor(ItemStack stack, boolean inverted)
	{
		stack.apply(
			SplatcraftComponents.ITEM_COLOR_DATA,
			SplatcraftComponents.ItemColorData.DEFAULT.get(),
			v -> v.withInvertedColor(inverted)
		);
		return stack;
	}
	public static @NotNull InkColor getInkColor(ItemStack stack)
	{
		return applyColorDataPredicate(stack, SplatcraftComponents.ItemColorData::color, InkColor.INVALID);
	}
	public static @NotNull InkColor getEffectiveColor(ItemStack stack)
	{
		return applyColorDataPredicate(stack, SplatcraftComponents.ItemColorData::getEffectiveColor, InkColor.INVALID);
	}
	public static @NotNull InkColor getEffectiveColor(ItemStack stack, Entity entity)
	{
		if (entity == null)
			return getEffectiveColor(stack);
		return applyColorDataPredicate(stack, data -> data.getEffectiveColor(entity), InkColor.INVALID);
	}
	public static ItemStack withInkColor(ItemStack stack, InkColor color)
	{
		InkColor finalColor = color == null ? InkColor.INVALID : color;
		stack.apply(
			SplatcraftComponents.ITEM_COLOR_DATA,
			SplatcraftComponents.ItemColorData.DEFAULT.get(),
			v -> v.withInkColor(finalColor)
		);
		return stack;
	}
	public static InkColor getInkColor(BlockEntity te)
	{
		return getInkColor(te.getWorld(), te.getPos());
	}
	public static InkColor getInkColor(World world, BlockPos pos)
	{
		if (world.getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock)
			return coloredBlock.getColor(world, pos);
		
		return getDefaultColor();
	}
	public static InkColor getEffectiveColor(World world, BlockPos pos)
	{
		InkColor color = getInkColor(world, pos);
		return InkColor.getIfInversed(color, isInverted(world, pos));
	}
	public static boolean isInverted(World world, BlockPos pos)
	{
		return world.getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock && coloredBlock.isInverted(world, pos);
	}
	public static void withInvertedColor(World world, BlockPos pos, boolean inverted)
	{
		if (world.getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock)
			coloredBlock.setInverted(world, pos, inverted);
	}
	public static boolean withInkColor(BlockEntity te, InkColor color)
	{
		if (te instanceof InkColorTileEntity te1)
		{
			te1.setColor(color);
			return true;
		}
		if (te.getCachedState().getBlock() instanceof IColoredBlock block)
		{
			return block.setColor(te.getWorld(), te.getPos(), color);
		}
		return false;
	}
	public static List<ItemStack> getColorVariantsForItem(ItemConvertible item, boolean matching, boolean inverted, boolean starter)
	{
		List<ItemStack> items = new ArrayList<>();
		
		if (matching)
			items.add(withInkColor(item.asItem().getDefaultStack(), null));
		if (inverted)
			items.add(withInvertedColor(withColorLocked(item.asItem().getDefaultStack(), false), true));
		
		if (starter)
			for (InkColor color : getGetStarterColors())
				items.add(withColorLocked(withInkColor(item.asItem().getDefaultStack(), color), true));
		
		return items;
	}
	@Environment(EnvType.CLIENT)
	public static boolean isColorLocked()
	{
		return SplatcraftConfig.get("splatcraft.colorLock");
	}
	@Environment(EnvType.CLIENT)
	public static @NotNull InkColor getColorLockedIfConfig(InkColor color)
	{
		return isColorLocked() ? getLockedColor(color) : color;
	}
	public static @NotNull InkColor getLockedColor(InkColor color)
	{
		return ClientUtils.getClientPlayer() != null
			? getEntityColor(ClientUtils.getClientPlayer()) == color
			? getColorLockFriendly()
			: getColorLockHostile()
			: InkColor.INVALID;
	}
	public static void forEachColoredBlockInBounds(World world, Box bounds, ColoredBlockConsumer action)
	{
		final Box expandedBounds = bounds.stretch(1, 1, 1);
		int chunkMinX = (int) bounds.minX >> 4;
		int chunkMinZ = (int) bounds.minZ >> 4;
		int chunkmaxX = (int) bounds.maxX >> 4;
		int chunkmaxZ = (int) bounds.maxZ >> 4;
		for (int x = chunkMinX; x <= chunkmaxX; x++)
			for (int z = chunkMinZ; z <= chunkmaxZ; z++)
			{
				world.getChunk(x, z).getBlockEntities().entrySet().stream().filter(entry -> entry.getValue().getCachedState().getBlock() instanceof IColoredBlock && expandedBounds.contains(entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()))
					.forEach(entry -> action.accept(entry.getKey(), (IColoredBlock) entry.getValue().getCachedState().getBlock(), entry.getValue()));
			}
		{
		}
	}
	public static MutableText getColorName(InkColor color)
	{
		return MutableText.of(new InkColorTranslatableContents(color));//Text.literal("#" + String.format("%06X", color).toUpperCase());
	}
	public static MutableText getFormatedColorName(InkColor color, boolean colorless)
	{
		MutableText colorName = getColorName(color);
		if (color == getDefaultColor())
			return Text.literal((colorless ? Formatting.GRAY : "") + colorName.getString());
		return colorName.withColor(color.getColor());
	}
	public static boolean colorEquals(World world, BlockPos pos, InkColor colorA, InkColor colorB)
	{
		return SplatcraftGameRules.getLocalizedRule(world, pos, SplatcraftGameRules.UNIVERSAL_INK) || colorA.getColor() == colorB.getColor();
	}
	public static boolean colorValueEquals(World world, BlockPos pos, int colorA, int colorB)
	{
		return SplatcraftGameRules.getLocalizedRule(world, pos, SplatcraftGameRules.UNIVERSAL_INK) || colorA == colorB;
	}
	public static boolean colorEquals(World world, BlockPos pos, InkColor otherColor)
	{
		return SplatcraftGameRules.getLocalizedRule(world, pos, SplatcraftGameRules.UNIVERSAL_INK) || getInkColor(world, pos) == otherColor;
	}
	public static boolean colorEquals(Entity entity, BlockEntity te)
	{
		if (entity == null || te == null)
			return false;
		
		InkColor entityColor = getEntityColor(entity);
		InkColor inkColor = getEffectiveColor(te.getWorld(), te.getPos());
		
		if (!entityColor.isValid() || !inkColor.isValid())
			return false;
		return colorEquals(entity.getWorld(), te.getPos(), entityColor, inkColor);
	}
	public static boolean colorEquals(LivingEntity entity, ItemStack stack)
	{
		InkColor entityColor = getEntityColor(entity);
		InkColor inkColor = getInkColor(stack);
		
		if (!entityColor.isValid() || !inkColor.isValid())
			return false;
		return colorEquals(entity.getWorld(), entity.getBlockPos(), entityColor, inkColor);
	}
	public static ItemStack withColorLocked(ItemStack stack, boolean isLocked)
	{
		stack.apply(
			SplatcraftComponents.ITEM_COLOR_DATA,
			SplatcraftComponents.ItemColorData.DEFAULT.get(),
			v -> v.withColorLocked(isLocked)
		);
		return stack;
	}
	public static boolean isColorLocked(ItemStack stack)
	{
		return applyColorDataPredicate(stack, SplatcraftComponents.ItemColorData::colorLocked, false);
	}
	public static float[] hexToRGB(int color)
	{
		float r = ((color & 0x00FF0000) >> 16) / 255.0f;
		float g = ((color & 0x0000FF00) >> 8) / 255.0f;
		float b = (color & 0x000000FF) / 255.0f;
		
		return new float[] {r, g, b};
	}
	public static int RGBtoHex(float[] color)
	{
		return (int) (color[0] * 255f) << 16 | (int) (color[1] * 255f) << 8 | (int) (color[2] * 255f);
	}
	public static InkColor getRandomStarterColor()
	{
		return InkColorGroups.STARTER_COLORS.getRandom(random);
	}
	public static void addInkSplashParticle(World world, LivingEntity source, float size)
	{
		InkColor color = getDefaultColor();
		if (EntityInfoCapability.hasCapability(source))
		{
			color = EntityInfoCapability.get(source).getColor();
		}
		
		addInkSplashParticle(world, color, source.getX(), source.getBodyY((world.getRandom().nextFloat() * 0.3f)), source.getZ(), size + (world.getRandom().nextFloat() * 0.2f - 0.1f));
	}
	public static void addInkSplashParticle(ServerWorld level, LivingEntity source, float size)
	{
		InkColor color = getDefaultColor();
		if (EntityInfoCapability.hasCapability(source))
		{
			color = EntityInfoCapability.get(source).getColor();
		}
		addInkSplashParticle(level, color, source.getX(), source.getCameraPosVec(level.getRandom().nextFloat() * 0.3f), source.getZ(), size + (level.getRandom().nextFloat() * 0.2f - 0.1f));
	}
	public static void addStandingInkSplashParticle(World world, LivingEntity entity, float size)
	{
		InkColor color = InkColor.INVALID;
		BlockPos pos = InkBlockUtils.getBlockStandingOnPos(entity);
		if (InkBlockUtils.isInked(world, pos, Direction.UP))
			color = InkBlockUtils.getInkInFace(world, pos, Direction.UP).color();
		else if (entity.getWorld().getBlockState(pos).getBlock() instanceof IColoredBlock block)
			color = block.getColor(world, pos);
		addInkSplashParticle(world, color, entity.getX() + (world.getRandom().nextFloat() * 0.8 - 0.4), entity.getCameraPosVec(world.getRandom().nextFloat() * 0.3f), entity.getZ() + (world.getRandom().nextFloat() * 0.8 - 0.4), size + (world.getRandom().nextFloat() * 0.2f - 0.1f));
	}
	public static void addInkSplashParticle(World world, InkColor color, double x, double y, double z, float size)
	{
		float[] rgb = color.getRGB();
		if (world instanceof ServerWorld serverLevel)
			serverLevel.spawnParticles(new InkSplashParticleData(rgb[0], rgb[1], rgb[2], size), x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0F);
		else
			world.addParticle(new InkSplashParticleData(rgb[0], rgb[1], rgb[2], size), x, y, z, 0.0D, 0.0D, 0.0D);
	}
	public static void addInkSplashParticle(World world, InkColor color, double x, Vec3d y, double z, float size)
	{
		float[] rgb = color.getRGB();
		world.addParticle(new InkSplashParticleData(rgb[0], rgb[1], rgb[2], size), x, y.y, z, 0.0D, 0.0D, 0.0D);
	}
	public static void addInkTerrainParticle(World world, InkColor color, double x, double y, double z, double dx, double dy, double dz, float maxSpeed)
	{
		if (world instanceof ServerWorld serverLevel)
			serverLevel.spawnParticles(new InkTerrainParticleData(color), x, y, z, 1, dx, dy, dz, maxSpeed);
		else
			world.addParticle(new InkTerrainParticleData(color), x, y, z, 0.0D, 0.0D, 0.0D);
	}
	public static void addInkDestroyParticle(World world, BlockPos pos, InkColor color)
	{
		BlockState state = world.getBlockState(pos);
		VoxelShape voxelshape = state.getCullingShape(world, pos);
		
		if (voxelshape.isEmpty())
			voxelshape = VoxelShapes.fullCube();
		
		voxelshape.forEachBox((p_172273_, p_172274_, p_172275_, p_172276_, p_172277_, p_172278_) ->
		{
			double d1 = Math.min(1.0D, p_172276_ - p_172273_);
			double d2 = Math.min(1.0D, p_172277_ - p_172274_);
			double d3 = Math.min(1.0D, p_172278_ - p_172275_);
			int i = Math.max(2, MathHelper.ceil(d1 / 0.25D));
			int j = Math.max(2, MathHelper.ceil(d2 / 0.25D));
			int k = Math.max(2, MathHelper.ceil(d3 / 0.25D));
			
			for (int x = 0; x < i; ++x)
			{
				for (int y = 0; y < j; ++y)
				{
					for (int z = 0; z < k; ++z)
					{
						double d4 = (x + 0.5D) / i;
						double d5 = (y + 0.5D) / j;
						double d6 = (z + 0.5D) / k;
						double d7 = d4 * d1 + p_172273_;
						double d8 = d5 * d2 + p_172274_;
						double d9 = d6 * d3 + p_172275_;
						
						addInkTerrainParticle(world, color, (double) pos.getX() + d7, (double) pos.getY() + d8, (double) pos.getZ() + d9, d4 - 0.5D, d5 - 0.5D, d6 - 0.5D, 1);
					}
				}
			}
		});
	}
	public static InkColor getOrange()
	{
		return InkColorRegistry.getInkColorByAlias(Splatcraft.identifierOf("orange"));
	}
	public static InkColor getBlue()
	{
		return InkColorRegistry.getInkColorByAlias(Splatcraft.identifierOf("blue"));
	}
	public static InkColor getGreen()
	{
		return InkColorRegistry.getInkColorByAlias(Splatcraft.identifierOf("green"));
	}
	public static InkColor getPink()
	{
		return InkColorRegistry.getInkColorByAlias(Splatcraft.identifierOf("pink"));
	}
	public static InkColor getDefaultColor()
	{
		if (InkColorRegistry.REGISTRY.isEmpty())
			return new InkColor(0x1F1F2D);
		
		return InkColorRegistry.getInkColorByAlias(Splatcraft.identifierOf("default"));
	}
	public static InkColor getColorLockFriendly()
	{
		return InkColorRegistry.getInkColorByAlias(Splatcraft.identifierOf("color_lock_friendly"));
	}
	public static InkColor getColorLockHostile()
	{
		return InkColorRegistry.getInkColorByAlias(Splatcraft.identifierOf("color_lock_hostile"));
	}
	public static Collection<InkColor> getGetStarterColors()
	{
		return InkColorGroups.STARTER_COLORS.getAll();
	}
	public interface ColoredBlockConsumer
	{
		void accept(BlockPos pos, IColoredBlock coloredBlock, BlockEntity blockEntity);
	}
}