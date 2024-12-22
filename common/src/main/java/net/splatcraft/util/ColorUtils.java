package net.splatcraft.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
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
import net.minecraft.text.TextColor;
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
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.handlers.ScoreboardHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerColorPacket;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftStats;
import net.splatcraft.tileentities.InkColorTileEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class ColorUtils
{
	// actual heresy but i am a c# dev so public static!!!!!
	public static final Random random = new Random();
	private static final int ORANGE = 0xDF641A;
	private static final int BLUE = 0x26229F;
	private static final int GREEN = 0x409d3b;
	private static final int PINK = 0xc83d79;
	private static final int DEFAULT = 0x1F1F2D;
	private static final int COLOR_LOCK_FRIENDLY = 0xDEA801;
	private static final int COLOR_LOCK_HOSTILE = 0x4717A9;
	private static final Collection<InkColor> getStarterColors = InkColorGroups.STARTER_COLORS.getAll();
	public static @Nullable SplatcraftComponents.ItemColorData getColorDataFromStack(ItemStack stack)
	{
		return stack.getComponents().getOrDefault(SplatcraftComponents.ITEM_COLOR_DATA, null);
	}
	public static boolean doesStackHaveColorData(ItemStack stack)
	{
		return stack.getComponents().contains(SplatcraftComponents.ITEM_COLOR_DATA);
	}
	public static @Nullable SplatcraftComponents.ItemColorData getColorDataFromStack(ItemStack stack, Supplier<SplatcraftComponents.ItemColorData> fallback)
	{
		ComponentMap components = stack.getComponents();
		if (components.contains(SplatcraftComponents.ITEM_COLOR_DATA))
			return components.get(SplatcraftComponents.ITEM_COLOR_DATA);
		return fallback.get();
	}
	public static @NotNull InkColor getEntityColor(Entity entity)
	{
		if (entity instanceof PlayerEntity player)
		{
			if (player == ClientUtils.getClientPlayer())
				return ClientUtils.getClientPlayerColor(player.getUuid());
			if (EntityInfoCapability.hasCapability(player))
				return EntityInfoCapability.get(player).getColor();
			return InkColor.INVALID;
		}
		else if (entity instanceof IColoredEntity coloredEntity)
			return coloredEntity.getColor();
		else return InkColor.INVALID;
	}
	public static @NotNull InkColor getEntityColor(LivingEntity player)
	{
		if (player == ClientUtils.getClientPlayer())
			return ClientUtils.getClientPlayerColor(player.getUuid());
		if (EntityInfoCapability.hasCapability(player))
			return EntityInfoCapability.get(player).getColor();
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
		return getColorDataFromStack(stack).inverted();
	}
	public static ItemStack setInverted(ItemStack stack, boolean inverted)
	{
		SplatcraftComponents.ItemColorData colorData = getColorDataFromStack(stack, SplatcraftComponents.ItemColorData.DEFAULT);
		colorData.setInverted(inverted);
		return stack;
	}
	public static @NotNull InkColor getInkColor(ItemStack stack)
	{
		SplatcraftComponents.ItemColorData colorData = getColorDataFromStack(stack);
		return doesStackHaveColorData(stack) ? colorData.inkColor() : InkColor.INVALID;
	}
	public static @NotNull InkColor getInkColorOrInverted(ItemStack stack)
	{
		return getColorDataFromStack(stack).getEffectiveColor();
	}
	public static ItemStack setInkColor(ItemStack stack, InkColor color)
	{
		SplatcraftComponents.ItemColorData colorData = getColorDataFromStack(stack);
		if (color == null)
			colorData.setInkColor(InkColor.DEFAULT);
		else
			colorData.setInkColor(color);
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
		
		return InkColor.DEFAULT;
	}
	public static InkColor getInkColorOrInverted(World world, BlockPos pos)
	{
		InkColor color = getInkColor(world, pos);
		return InkColor.getIfInversed(color, isInverted(world, pos));
	}
	public static boolean isInverted(World world, BlockPos pos)
	{
		return world.getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock && coloredBlock.isInverted(world, pos);
	}
	public static void setInverted(World world, BlockPos pos, boolean inverted)
	{
		if (world.getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock)
			coloredBlock.setInverted(world, pos, inverted);
	}
	public static boolean setInkColor(BlockEntity te, InkColor color)
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
			items.add(setInkColor(item.asItem().getDefaultStack(), null));
		if (inverted)
			items.add(setInverted(setColorLocked(item.asItem().getDefaultStack(), false), true));
		
		if (starter)
			for (InkColor color : getGetStarterColors())
				items.add(setColorLocked(setInkColor(item.asItem().getDefaultStack(), color), true));
		
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
		for (BlockPos.Mutable chunkPos = new BlockPos.Mutable(bounds.minX, bounds.minY, bounds.minZ);
		     chunkPos.getX() <= bounds.maxX && chunkPos.getY() <= bounds.maxY && chunkPos.getZ() <= bounds.maxZ; chunkPos.move(16, 16, 16))
		{
			world.getWorldChunk(chunkPos).getBlockEntities().entrySet().stream().filter(entry -> entry.getValue().getCachedState().getBlock() instanceof IColoredBlock && expandedBounds.contains(entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()))
				.forEach(entry -> action.accept(entry.getKey(), (IColoredBlock) entry.getValue().getCachedState().getBlock(), entry.getValue()));
		}
	}
	public static MutableText getColorName(InkColor color)
	{
		
		return MutableText.of(new InkColorTranslatableContents(color));//Text.literal("#" + String.format("%06X", color).toUpperCase());
	}
	public static MutableText getFormatedColorName(InkColor color, boolean colorless)
	{
		return color == getDefaultColor()
			? Text.literal((colorless ? Formatting.GRAY : "") + getColorName(color).getString())
			: getColorName(color).setStyle(getColorName(color).getStyle().withColor(TextColor.fromRgb(color.getColor())));
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
		InkColor inkColor = getInkColorOrInverted(te.getWorld(), te.getPos());
		
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
	public static ItemStack setColorLocked(ItemStack stack, boolean isLocked)
	{
		getColorDataFromStack(stack).setColorImmutable(isLocked);
		return stack;
	}
	public static boolean isColorLocked(ItemStack stack)
	{
		SplatcraftComponents.ItemColorData colorData = getColorDataFromStack(stack);
		return colorData != null && colorData.isColorImmutable();
	}
	public static float[] hexToRGB(int color)
	{
		float r = ((color & 0x00FF0000) >> 16) / 255.0f;
		float g = ((color & 0x0000FF00) >> 8) / 255.0f;
		float b = (color & 0x000000FF) / 255.0f;
		
		return new float[] {r, g, b};
	}
	public static InkColor RGBtoHex(float[] color)
	{
		return InkColor.constructOrReuse((((int) (color[0] * 255f)) << 16) | (((int) (color[1] * 255f)) << 8) | (((int) (color[2] * 255f))));
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