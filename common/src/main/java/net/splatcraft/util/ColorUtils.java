package net.splatcraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.blocks.IColoredBlock;
import net.splatcraft.client.particles.InkSplashParticleData;
import net.splatcraft.client.particles.InkTerrainParticleData;
import net.splatcraft.data.InkColorGroup;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.data.capabilities.structs.SquidInfo;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerColorPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.ModSide;
import net.splatcraft.platform.Services;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftStats;
import net.splatcraft.tileentities.InkColorTileEntity;
import net.splatcraft.util.structs.InkColor;
import net.splatcraft.util.structs.InkColorTranslatableContents;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class ColorUtils
{
	// actual heresy but i am a c# dev so public static!!!!!
	public static final Random random = new Random();
	public static boolean doesStackHaveColorData(ItemStack stack)
	{
		return stack.has(SplatcraftComponents.ITEM_COLOR_DATA);
	}
	public static <T> T applyColorDataPredicate(ItemStack stack, Function<SplatcraftComponents.ItemColorData, T> getter, T fallback)
	{
		if (stack.has(SplatcraftComponents.ITEM_COLOR_DATA))
			return getter.apply(stack.get(SplatcraftComponents.ITEM_COLOR_DATA));
		return fallback;
	}
	public static @NotNull InkColor getEntityColor(Entity entity)
	{
		if (entity instanceof LivingEntity living)
		{
			if (Components.SQUID_INFO.has(living))
				return Components.SQUID_INFO.get(living).color();
		}
		if (entity instanceof IColoredEntity coloredEntity)
			return coloredEntity.getColor();
		return InkColor.INVALID;
	}
	public static InkColor getPlayerColor(UUID playerUuid, Level level)
	{
		return getEntityColor(level.getPlayerByUUID(playerUuid));
	}
	public static void setPlayerColor(Player player, InkColor color, boolean updateClient)
	{
		boolean didntHave = !Components.ENTITY_INFO.has(player);
		SquidInfo info = Components.SQUID_INFO.getOrCreate(player, () -> new SquidInfo(color));
		if (info.color() != color || didntHave)
		{
			if (player instanceof ServerPlayer serverPlayer)
				SplatcraftStats.CHANGE_INK_COLOR_TRIGGER.value().trigger(serverPlayer);
			
			Components.SQUID_INFO.set(player, info.withColor(color));
			ScoreboardHandler.updatePlayerScore(Stats.CUSTOM.get(ScoreboardHandler.COLOR), player, color);
		}
		
		Level world = player.level();
		if (!world.isClientSide() && updateClient)
		{
			SplatcraftPacketHandler.sendToTrackersAndSelf(new PlayerColorPacket(player, color), player);
		}
	}
	public static void setPlayerColor(Player player, InkColor color)
	{
		setPlayerColor(player, color, true);
	}
	public static boolean isInverted(ItemStack stack)
	{
		return applyColorDataPredicate(stack, SplatcraftComponents.ItemColorData::hasInvertedColor, false);
	}
	public static ItemStack withInvertedColor(ItemStack stack, boolean inverted)
	{
		stack.update(
			SplatcraftComponents.ITEM_COLOR_DATA,
			SplatcraftComponents.ItemColorData.DEFAULT,
			v -> v.withInvertedColor(inverted)
		);
		return stack;
	}
	/**
	 * Gets the direct {@link InkColor} stored in the specified {@link ItemStack}.
	 *
	 * @param stack The stack to check from.
	 * @return The {@link InkColor} that was stored in this {@link ItemStack}'s {@link net.splatcraft.registries.SplatcraftComponents.ItemColorData} component, if present, otherwise {@code InkColor.INVALID}.
	 */
	public static @NotNull InkColor getInkColor(ItemStack stack)
	{
		return applyColorDataPredicate(stack, SplatcraftComponents.ItemColorData::color, InkColor.INVALID);
	}
	/**
	 * Gets the effective {@link InkColor} stored in the specified {@link ItemStack}.
	 * This means that if the {@link ItemStack} has the inverted boolean set as true, it returns the inverted color of the stored original color.
	 *
	 * @param stack The stack to check from.
	 * @return The {@link InkColor} that was stored in this {@link ItemStack}'s {@link net.splatcraft.registries.SplatcraftComponents.ItemColorData} component with the specified convertions, if present, otherwise {@code InkColor.INVALID}.
	 */
	public static @NotNull InkColor getEffectiveColor(ItemStack stack)
	{
		return applyColorDataPredicate(stack, SplatcraftComponents.ItemColorData::getEffectiveColor, InkColor.INVALID);
	}
	/**
	 * Gets the effective {@link InkColor} stored in the specified {@link ItemStack}.
	 * This means that if the {@link ItemStack} has the inverted boolean set as true, it returns the inverted color of the stored original color. And if the color is {@code inkColor.INVALID}, it returns the {@link InkColor} of the specified {@link Entity}.
	 *
	 * @param stack  The stack to check from.
	 * @param entity The entity to retrieve the color from, if the {@link ItemStack}'s color is {@code inkColor.INVALID}
	 * @return The {@link InkColor} that was stored in this {@link ItemStack}'s {@link net.splatcraft.registries.SplatcraftComponents.ItemColorData} component with the specified convertions, if present, otherwise {@code InkColor.INVALID}.
	 */
	public static @NotNull InkColor getEffectiveColor(ItemStack stack, Entity entity)
	{
		if (entity == null)
			return getEffectiveColor(stack);
		return applyColorDataPredicate(stack, data -> data.getEffectiveColor(entity), InkColor.INVALID);
	}
	public static ItemStack withInkColor(ItemStack stack, InkColor color)
	{
		InkColor finalColor = color == null ? InkColor.INVALID : color;
		stack.update(
			SplatcraftComponents.ITEM_COLOR_DATA,
			SplatcraftComponents.ItemColorData.DEFAULT,
			v -> v.withInkColor(finalColor)
		);
		return stack;
	}
	public static InkColor getInkColor(BlockEntity te)
	{
		return getInkColor(te.getLevel(), te.getBlockPos());
	}
	public static InkColor getInkColor(Level world, BlockPos pos)
	{
		if (world.getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock)
			return coloredBlock.getColor(world, pos);
		
		return getDefaultColor();
	}
	public static InkColor getEffectiveColor(Level world, BlockPos pos)
	{
		InkColor color = getInkColor(world, pos);
		return InkColor.getIfInversed(color, isInverted(world, pos));
	}
	public static boolean isInverted(Level world, BlockPos pos)
	{
		return world.getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock && coloredBlock.isInverted(world, pos);
	}
	public static void withInvertedColor(Level world, BlockPos pos, boolean inverted)
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
		if (te.getBlockState().getBlock() instanceof IColoredBlock block)
		{
			return block.setColor(te.getLevel(), te.getBlockPos(), color);
		}
		return false;
	}
	public static List<ItemStack> getColorVariantsForItem(ItemLike item, boolean matching, boolean inverted, boolean starter)
	{
		List<ItemStack> items = new ArrayList<>();
		
		if (matching)
			items.add(withInkColor(item.asItem().getDefaultInstance(), null));
		if (inverted)
			items.add(withInvertedColor(withColorLocked(item.asItem().getDefaultInstance(), false), true));
		
		if (starter)
			for (InkColor color : getStarterColors())
				items.add(withColorLocked(withInkColor(item.asItem().getDefaultInstance(), color), true));
		
		return items;
	}
	@OnlyIn(Dist.CLIENT)
	public static boolean isColorLocked()
	{
		return SplatcraftConfig.get("splatcraft.colorLock");
	}
	public static @NotNull InkColor getColorLockedIfConfig(InkColor color)
	{
		return Services.PLATFORM.getModSide().equals(ModSide.CLIENT) && isColorLocked() ? getLockedColor(color) : color;
	}
	@OnlyIn(Dist.CLIENT)
	public static @NotNull InkColor getLockedColor(InkColor color)
	{
		return ClientUtils.getClientPlayer() != null
			? getEntityColor(ClientUtils.getClientPlayer()) == color
			? getColorLockFriendly()
			: getColorLockHostile()
			: InkColor.INVALID;
	}
	public static void forEachColoredBlockInBounds(Level world, final AABB bounds, ColoredBlockConsumer action)
	{
		int chunkMinX = (int) bounds.minX >> 4;
		int chunkMinZ = (int) bounds.minZ >> 4;
		int chunkmaxX = (int) bounds.maxX >> 4;
		int chunkmaxZ = (int) bounds.maxZ >> 4;
		for (int x = chunkMinX; x <= chunkmaxX; x++)
			for (int z = chunkMinZ; z <= chunkmaxZ; z++)
			{
				world.getChunk(x, z).getBlockEntities().entrySet().stream().filter(entry -> entry.getValue().getBlockState().getBlock() instanceof IColoredBlock && bounds.contains(entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()))
					.forEach(entry -> action.accept(entry.getKey(), (IColoredBlock) entry.getValue().getBlockState().getBlock(), entry.getValue()));
			}
		{
		}
	}
	public static MutableComponent getColorName(InkColor color)
	{
		return MutableComponent.create(new InkColorTranslatableContents(color));//Text.literal("#" + String.format("%06X", color).toUpperCase());
	}
	public static MutableComponent getFormatedColorName(InkColor color, boolean colorless)
	{
		MutableComponent colorName = getColorName(color);
		if (color == getDefaultColor())
			return Component.literal((colorless ? ChatFormatting.GRAY : "") + colorName.getString());
		return colorName.withColor(color.getColor());
	}
	public static boolean colorEquals(Level world, BlockPos pos, InkColor colorA, InkColor colorB)
	{
		return SplatcraftGameRules.getLocalizedRule(world, pos, SplatcraftGameRules.UNIVERSAL_INK) || colorA.getColor() == colorB.getColor();
	}
	public static boolean colorValueEquals(Level world, BlockPos pos, int colorA, int colorB)
	{
		return SplatcraftGameRules.getLocalizedRule(world, pos, SplatcraftGameRules.UNIVERSAL_INK) || colorA == colorB;
	}
	public static boolean colorEquals(Level world, BlockPos pos, InkColor otherColor)
	{
		return SplatcraftGameRules.getLocalizedRule(world, pos, SplatcraftGameRules.UNIVERSAL_INK) || getInkColor(world, pos) == otherColor;
	}
	public static boolean colorEquals(Entity entity, BlockEntity te)
	{
		if (entity == null || te == null)
			return false;
		
		InkColor entityColor = getEntityColor(entity);
		InkColor inkColor = getEffectiveColor(te.getLevel(), te.getBlockPos());
		
		if (!entityColor.isValid() || !inkColor.isValid())
			return false;
		return colorEquals(entity.level(), te.getBlockPos(), entityColor, inkColor);
	}
	public static boolean colorEquals(LivingEntity entity, ItemStack stack)
	{
		InkColor entityColor = getEntityColor(entity);
		InkColor inkColor = getInkColor(stack);
		
		if (!entityColor.isValid() || !inkColor.isValid())
			return false;
		return colorEquals(entity.level(), entity.blockPosition(), entityColor, inkColor);
	}
	public static ItemStack withColorLocked(ItemStack stack, boolean isLocked)
	{
		stack.update(
			SplatcraftComponents.ITEM_COLOR_DATA,
			SplatcraftComponents.ItemColorData.DEFAULT,
			v -> v.withColorLocked(isLocked)
		);
		return stack;
	}
	public static boolean isColorLocked(ItemStack stack)
	{
		return applyColorDataPredicate(stack, SplatcraftComponents.ItemColorData::colorLocked, false);
	}
	public static int makeBrighter(InkColor color)
	{
		return makeBrighter(color, 0.5f, 0.9f);
	}
	public static int makeBrighter(InkColor color, float desaturationDelta, float brightnessDelta)
	{
		return makeBrighter(color.getColorWithAlpha(255), desaturationDelta, brightnessDelta);
	}
	public static int makeBrighter(int color, float desaturationDelta, float brightnessDelta)
	{
		return applyHSBOperators(color,
			v -> v,
			v -> Mth.clamp(Mth.lerp(desaturationDelta, v, 0), 0, 1),
			v -> Mth.clamp(Mth.lerp(brightnessDelta, v, 1), 0, 1)
		);
	}
	public static int applyHSBOperators(InkColor color,
	                                    UnaryOperator<Float> hueOperator,
	                                    UnaryOperator<Float> saturationOperator,
	                                    UnaryOperator<Float> brightnessOperator
	)
	{
		return applyHSBOperators(color.getColor(), hueOperator, saturationOperator, brightnessOperator);
	}
	public static int applyHSBOperators(int color,
	                                    UnaryOperator<Float> hueOperator,
	                                    UnaryOperator<Float> saturationOperator,
	                                    UnaryOperator<Float> brightnessOperator
	)
	{
		float[] hslValues = new float[3];
		Color.RGBtoHSB(FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color), hslValues);
		hslValues[0] = hueOperator.apply(hslValues[0]);
		hslValues[1] = saturationOperator.apply(hslValues[1]);
		hslValues[2] = brightnessOperator.apply(hslValues[2]);
		return FastColor.ARGB32.color(FastColor.ARGB32.alpha(color), Color.HSBtoRGB(hslValues[0], hslValues[1], hslValues[2]));
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
		return InkColorGroup.getGroup(InkColorGroup.STARTER_COLORS).get().getRandomColor(random);
	}
	public static void addInkSplashParticle(Level world, LivingEntity source, float size)
	{
		InkColor color = getDefaultColor();
		if (Components.SQUID_INFO.has(source))
		{
			color = Components.SQUID_INFO.get(source).color();
		}
		
		addInkSplashParticle(world, color, source.getX(), source.getY(world.getRandom().nextFloat() * 0.3f), source.getZ(), size + (world.getRandom().nextFloat() * 0.2f - 0.1f));
	}
	public static void addInkSplashParticle(ServerLevel level, LivingEntity source, float size)
	{
		InkColor color = getDefaultColor();
		if (Components.SQUID_INFO.has(source))
		{
			color = Components.SQUID_INFO.get(source).color();
		}
		addInkSplashParticle(level, color, source.getX(), source.getY(level.getRandom().nextFloat() * 0.3f), source.getZ(), size + (level.getRandom().nextFloat() * 0.2f - 0.1f));
	}
	public static void addStandingInkSplashParticle(Level world, LivingEntity entity, float size)
	{
		Optional<BlockPos> posOptional = InkBlockUtils.getBlockBelowPos(entity);
		posOptional.ifPresent(pos ->
		{
			InkColor color = InkColor.INVALID;
			if (InkBlockUtils.isInked(world, pos, Direction.UP))
				color = InkBlockUtils.getInkInFace(world, pos, Direction.UP).color();
			else if (entity.level().getBlockState(pos).getBlock() instanceof IColoredBlock block)
				color = block.getColor(world, pos);
			addInkSplashParticle(world, color, entity.getX() + (world.getRandom().nextFloat() * 0.8 - 0.4), entity.getY(world.getRandom().nextFloat() * 0.3f), entity.getZ() + (world.getRandom().nextFloat() * 0.8 - 0.4), size + (world.getRandom().nextFloat() * 0.2f - 0.1f));
		});
	}
	public static void addInkSplashParticle(Level world, InkColor color, double x, double y, double z, float size)
	{
		float[] rgb = color.getRGB();
		if (world instanceof ServerLevel serverLevel)
			serverLevel.sendParticles(new InkSplashParticleData(rgb[0], rgb[1], rgb[2], size), x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0F);
		else
			world.addParticle(new InkSplashParticleData(rgb[0], rgb[1], rgb[2], size), x, y, z, 0.0D, 0.0D, 0.0D);
	}
	public static void addInkTerrainParticle(Level world, InkColor color, double x, double y, double z, double dx, double dy, double dz, float maxSpeed)
	{
		if (world instanceof ServerLevel serverLevel)
			serverLevel.sendParticles(new InkTerrainParticleData(color), x, y, z, 1, dx, dy, dz, maxSpeed);
		else
			world.addParticle(new InkTerrainParticleData(color), x, y, z, 0.0D, 0.0D, 0.0D);
	}
	public static void addInkDestroyParticle(Level world, BlockPos pos, InkColor color)
	{
		BlockState state = world.getBlockState(pos);
		VoxelShape voxelshape = state.getOcclusionShape(world, pos);
		
		if (voxelshape.isEmpty())
			voxelshape = Shapes.block();
		
		voxelshape.forAllBoxes((p_172273_, p_172274_, p_172275_, p_172276_, p_172277_, p_172278_) ->
		{
			double d1 = Math.min(1.0D, p_172276_ - p_172273_);
			double d2 = Math.min(1.0D, p_172277_ - p_172274_);
			double d3 = Math.min(1.0D, p_172278_ - p_172275_);
			int i = Math.max(2, Mth.ceil(d1 / 0.25D));
			int j = Math.max(2, Mth.ceil(d2 / 0.25D));
			int k = Math.max(2, Mth.ceil(d3 / 0.25D));
			
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
		return InkColorRegistry.getColorByAlias(Splatcraft.identifierOf("orange")).get();
	}
	public static InkColor getBlue()
	{
		return InkColorRegistry.getColorByAlias(Splatcraft.identifierOf("blue")).get();
	}
	public static InkColor getGreen()
	{
		return InkColorRegistry.getColorByAlias(Splatcraft.identifierOf("green")).get();
	}
	public static InkColor getPink()
	{
		return InkColorRegistry.getColorByAlias(Splatcraft.identifierOf("pink")).get();
	}
	public static InkColor getDefaultColor()
	{
		if (InkColorRegistry.REGISTRY.isEmpty())
			return new InkColor(0x1F1F2D);
		
		return InkColorRegistry.getColorByAlias(Splatcraft.identifierOf("default")).get();
	}
	public static InkColor getColorLockFriendly()
	{
		return InkColorRegistry.getColorByAlias(Splatcraft.identifierOf("color_lock_friendly")).get();
	}
	public static InkColor getColorLockHostile()
	{
		return InkColorRegistry.getColorByAlias(Splatcraft.identifierOf("color_lock_hostile")).get();
	}
	public static Collection<InkColor> getStarterColors()
	{
		return InkColorGroup.getGroup(InkColorGroup.STARTER_COLORS).get().getColors();
	}
	public interface ColoredBlockConsumer
	{
		void accept(BlockPos pos, IColoredBlock coloredBlock, BlockEntity blockEntity);
	}
}