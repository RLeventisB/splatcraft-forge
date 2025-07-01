package net.splatcraft.util;

import com.google.common.base.Supplier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.client.renderer.InkSquidRenderer;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.platform.ModSide;
import net.splatcraft.platform.Services;
import net.splatcraft.util.action.EntityAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class CommonUtils
{
	public static final EntityDataSerializer<Vector2f> VEC2_DATA_HANDLER = new EntityDataSerializer<>()
	{
		public static final StreamCodec<RegistryFriendlyByteBuf, Vector2f> PACKET_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, Vector2f::x,
			ByteBufCodecs.FLOAT, Vector2f::y,
			Vector2f::new);
		@Override
		public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, Vector2f> codec()
		{
			return PACKET_CODEC;
		}
		@Override
		public @NotNull Vector2f copy(@NotNull Vector2f vec2)
		{
			return new Vector2f(vec2.x, vec2.y);
		}
	};
	public static final EntityDataSerializer<InkColor> INKCOLOR_DATA_HANDLER = EntityDataSerializer.forValueType(InkColor.PACKET_CODEC);
	public static final EntityDataSerializer<UUID> UUID_DATA_HANDLER = EntityDataSerializer.forValueType(UUIDUtil.STREAM_CODEC);
	public static final EntityDataSerializer<Vec3> VEC3_DATA_HANDLER = EntityDataSerializer.forValueType(CodecUtils.Codecs.VEC_3_PACKET_CODEC);
	public static final EntityDataSerializer<Optional<Vec3>> OPTIONAL_VEC3_DATA_HANDLER = EntityDataSerializer.forValueType(ByteBufCodecs.optional(CodecUtils.Codecs.VEC_3_PACKET_CODEC));
	public static CustomPacketPayload.Type<?> createIdFromClass(Class<?> clazz)
	{
		return new CustomPacketPayload.Type<>(Splatcraft.identifierOf(makeStringIdentifierValid(clazz.getSimpleName())));
	}
	public static String makeStringIdentifierValid(String text)
	{
		StringBuilder builder = new StringBuilder();
		for (char chr : text.toCharArray())
		{
			if (chr == '-' || chr == '_' || chr == '.' || Character.isLetterOrDigit(chr))
			{
				if (Character.isUpperCase(chr))
				{
					builder.append('_');
					builder.append(Character.toLowerCase(chr));
				}
				else builder.append(chr);
			}
		}
		return builder.toString();
	}
	public static void spawnTestParticle(Vec3 pos)
	{
		spawnTestParticle(getCurrentWorld(), new DustParticleOptions(new Vector3f(1, 0, 0), 1), pos);
	}
	public static TimedTextDisplayEntity spawnTestText(Level world, Vec3 pos, String text, int durationTicks)
	{
		return spawnTestText(world, pos, Component.literal(text), durationTicks);
	}
	public static TimedTextDisplayEntity spawnTestText(Level world, Vec3 pos, Component text, int durationTicks)
	{
		TimedTextDisplayEntity entity = null;
		
		if (world != null)
		{
			entity = new TimedTextDisplayEntity(EntityType.TEXT_DISPLAY, world, durationTicks);
			entity.setPos(pos);
			entity.setText(text);
			world.addFreshEntity(entity);
		}
		
		return entity;
	}
	public static void spawnTestParticle(Vec3 pos, Color color)
	{
		float[] rgb = color.getRGBColorComponents(null);
		
		spawnTestParticle(getCurrentWorld(), new DustParticleOptions(new Vector3f(rgb[0], rgb[1], rgb[2]), 3), pos);
	}
	public static void spawnTestBlockParticle(Vec3 pos, BlockState state)
	{
		spawnTestParticle(getCurrentWorld(), new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state), pos);
	}
	@OnlyIn(Dist.CLIENT)
	public static Level getCurrentWorld()
	{
		return Minecraft.getInstance().level;
	}
	public static void spawnTestParticle(Level world, ParticleOptions options, Vec3 pos)
	{
		if (world != null)
		{
			if (world instanceof ServerLevel serverLevel)
			{
				serverLevel.sendParticles(options, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
				return;
			}
			world.addParticle(options, true, pos.x, pos.y, pos.z, 0, 0, 0);
		}
	}
	public static void showBoundingBoxCorners(Level level, AABB aabb)
	{
		for (int x = 0; x < 2; x++)
		{
			for (int y = 0; y < 2; y++)
			{
				for (int z = 0; z < 2; z++)
				{
					spawnTestParticle(level,
						ParticleTypes.BUBBLE, new Vec3(
							x == 0 ? aabb.min(Direction.Axis.X) : aabb.max(Direction.Axis.X),
							y == 0 ? aabb.min(Direction.Axis.Y) : aabb.max(Direction.Axis.Y),
							z == 0 ? aabb.min(Direction.Axis.Z) : aabb.max(Direction.Axis.Z)));
				}
			}
		}
	}
	public static float nextFloat(RandomSource random, float min, float max)
	{
		return min + (max - min) * random.nextFloat();
	}
	public static double nextDouble(RandomSource random, double min, double max)
	{
		return min + (max - min) * random.nextDouble();
	}
	public static void blockDrop(Level level, BlockPos pos, ItemStack stack)
	{
		if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) /*&& !world.captureBlockSnapshots*/)
			spawnItem(level, pos, stack);
	}
	public static void spawnItem(Level level, BlockPos pos, ItemStack stack)
	{
		if (level.isClientSide() || stack.isEmpty())
			return;
		
		double d0 = (double) (level.random.nextFloat() * 0.5F) + 0.25D;
		double d1 = (double) (level.random.nextFloat() * 0.5F) + 0.25D;
		double d2 = (double) (level.random.nextFloat() * 0.5F) + 0.25D;
		ItemEntity itementity = new ItemEntity(level, (double) pos.getX() + d0, (double) pos.getY() + d1, (double) pos.getZ() + d2, stack);
		itementity.setDefaultPickUpDelay();
		level.addFreshEntity(itementity);
	}
	public static ItemStack getItemInInventory(Player entity, Predicate<ItemStack> predicate)
	{
		ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(entity, predicate);
		if (!itemstack.isEmpty())
			return itemstack;
		
		for (int i = 0; i < entity.getInventory().getContainerSize(); ++i)
		{
			ItemStack itemstack1 = entity.getInventory().getItem(i);
			if (predicate.test(itemstack1))
				return itemstack1;
		}
		
		return ItemStack.EMPTY;
	}
	// horrible redaction incoming
	/**
	 * Finds and returns an specific {@link ItemStack} and their respective index that are from the entity's inventory.
	 * The index may be negative if the {@link LivingEntity} passed is not a player, or if the respective stack was not found.
	 * If the entity is not a player, the index may be -2 or -3 if the stack is found in the main hand or offhand, respectively.
	 * If the entity is a player, and the stack is in the main hand, it returns the selected slot, if the item is in
	 * the offhand, it returns {@code PlayerInventory.OFF_HAND_SLOT}, otherwise if the stack is found in another slot from
	 * the player's inventory it returns the index that points to the inventory's slot from which you can find the same stack.
	 * If the stack is not found, the index is -1, and the stack is {@code ItemStack.EMPTY}.
	 *
	 * @param entity    The entity from which their entire inventory will be tested.
	 * @param predicate The predicate to test if an {@link ItemStack} is valid.
	 * @return An {@link Pair} which consists of the found item, if any, or else {@code ItemStack.EMPTY}, and the index
	 * of the found item.
	 */
	public static Pair<ItemStack, Integer> getStackAndIndexInInventory(LivingEntity entity, Predicate<ItemStack> predicate)
	{
		Pair<ItemStack, Integer> dataPair = getHeldProjectileAndIndex(entity, predicate);
		if (dataPair.getSecond() != -1)
			return dataPair;
		
		if (entity instanceof Player player)
		{
			Inventory inventory = player.getInventory();
			for (int i = 0; i < inventory.getContainerSize(); ++i)
			{
				ItemStack stack = inventory.getItem(i);
				if (predicate.test(stack))
					return Pair.of(stack, i);
			}
		}
		
		return Pair.of(ItemStack.EMPTY, -1);
	}
	/**
	 * Checks both hands and returns an {@link ItemStack} and their respective index, if the stack passes the given predicate.
	 * The index may be negative if the {@link LivingEntity} passed is not a player, or if the respective stack was not found.
	 * If the entity is not a player, the index may be -2 or -3 if the stack is found in the main hand or offhand, respectively.
	 * If the entity is a player, and the stack is in the main hand, it returns the selected slot, if the item is in
	 * the offhand, it returns {@code PlayerInventory.OFF_HAND_SLOT}.
	 * If the stack is not found, the index is -1, and the stack is {@code ItemStack.EMPTY}.
	 *
	 * @param entity    The entity from which their entire inventory will be tested.
	 * @param predicate The predicate to test if an {@link ItemStack} is valid.
	 * @return An {@link Pair} which consists of the found item, if any, or else {@code ItemStack.EMPTY}, and the index
	 * of the found item.
	 */
	public static Pair<ItemStack, Integer> getHeldProjectileAndIndex(LivingEntity entity, Predicate<ItemStack> predicate)
	{
		if (predicate.test(entity.getItemInHand(InteractionHand.OFF_HAND)))
		{
			return Pair.of(entity.getItemInHand(InteractionHand.OFF_HAND), entity instanceof Player ? Inventory.SLOT_OFFHAND : -3);
		}
		else
		{
			return predicate.test(entity.getItemInHand(InteractionHand.MAIN_HAND)) ? Pair.of(entity.getItemInHand(InteractionHand.MAIN_HAND), entity instanceof Player player ? player.getInventory().selected : -2) : Pair.of(ItemStack.EMPTY, -1);
		}
	}
	public static boolean anyWeaponOnCooldown(LivingEntity entity)
	{
		if (entity instanceof Player player)
			return anyWeaponOnCooldown(player);
		boolean isMainOnCooldown = entity.getMainHandItem().getItem() instanceof WeaponBaseItem;
		boolean isOffOnCooldown = entity.getOffhandItem().getItem() instanceof WeaponBaseItem;
		return isMainOnCooldown || isOffOnCooldown;
	}
	public static boolean anyWeaponOnCooldown(Player player)
	{
		boolean isMainOnCooldown = player.getMainHandItem().getItem() instanceof WeaponBaseItem weapon && player.getCooldowns().isOnCooldown(weapon);
		boolean isOffOnCooldown = player.getOffhandItem().getItem() instanceof WeaponBaseItem weapon && player.getCooldowns().isOnCooldown(weapon);
		return isMainOnCooldown || isOffOnCooldown;
	}
	public static @NotNull Result tickValue(float delay, float value, float decrease, float minValue, float timeDelta)
	{
		if (delay > 0)
		{
			delay -= timeDelta;
			if (delay < 0)
			{
				if (Float.isInfinite(decrease) || Float.isNaN(decrease))
					value = 0;
				else
					value -= decrease * -delay;
				delay = 0;
			}
		}
		else
		{
			if (decrease == 0)
				value = 0;
			else
			{
				if (value > minValue)
					value -= decrease * timeDelta;
				if (value < minValue)
					value = minValue;
			}
		}
		return new Result(delay, value);
	}
	public static @NotNull Result tickValueToMax(float delay, float value, float increase, float maxValue, float timeDelta)
	{
		if (delay > 0)
		{
			delay -= timeDelta;
			if (delay < 0)
			{
				if (Float.isInfinite(increase) || Float.isNaN(increase))
					value = 0;
				else
					value += increase * -delay;
				delay = 0;
			}
		}
		else
		{
			if (increase == 0)
				value = 0;
			else
			{
				if (value < maxValue)
					value += increase * timeDelta;
				if (value > maxValue)
					value = maxValue;
			}
		}
		return new Result(delay, value);
	}
	public static <T> T returnValueDependantOnSquidCancel(LivingEntity entity, T withCancel, T withoutCancel)
	{
		AtomicBoolean didCancel = new AtomicBoolean(false);
		EntityInfoCapability.getOptional(entity).ifPresent(info ->
		{
			didCancel.set(info.hasHigherStartup());
			info.resetHigherStartup();
		});
		return didCancel.get() ? withCancel : withoutCancel;
	}
	public static float startupSquidSwitch(LivingEntity entity, CommonRecords.ShotDataRecord shotData)
	{
		return returnValueDependantOnSquidCancel(entity, shotData.squidStartupTicks(), shotData.startupTicks());
	}
	public static float nextTriangular(RandomSource random, float mode, float deviation)
	{
		return mode + deviation * (random.nextFloat() - random.nextFloat());
	}
	public static boolean isRolling(LivingEntity entity)
	{
		return EntityAction.hasSpecificEntityAction(entity, DualieItem.DodgeRollAction.class);
	}
	public static InteractionHand otherHand(InteractionHand hand)
	{
		return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}
	public static <T> int @Nullable [] findMatches(List<T> inputs, java.util.List<? extends Predicate<T>> tests)
	{
		// maybe its better to just copy the neoforge code but idk if java folds same code methods
		return Services.PLATFORM.findItemMatches(inputs, tests);
	}
	public static Holder.Reference<Enchantment> getEnchantmentEntry(LevelReader world, ResourceKey<Enchantment> enchantment)
	{
		return world.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(enchantment).get();
	}
	public static void doRenderLivingAfterEvent(AbstractClientPlayer player, InkSquidRenderer squidRenderer, float g, PoseStack matrixStack, MultiBufferSource consumerProvider, int i)
	{
		Services.PLATFORM.postConsumerEvent("net.neoforged.neoforge.client.event.RenderLivingEvent.Post", player, squidRenderer, g, matrixStack, consumerProvider, i);
	}
	public static InteractionEventResultDummy doPlayerUseItemForgeEvent(int i, KeyMapping useKey, InteractionHand hand)
	{
		return new InteractionEventResultDummy(true, false);
	}
	public static void doForgeEmptyClickEvent(LocalPlayer player, InteractionHand hand)
	{
	
	}
	public static ItemStack callGetPickItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player)
	{
		return null;
	}
	public static boolean callCanHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player)
	{
		return false;
	}
	public static <T> T getDistSpecificValue(Supplier<T> clientSupplier, Supplier<T> serverSupplier)
	{
		return (Services.PLATFORM.getModSide().equals(ModSide.CLIENT) ? clientSupplier : serverSupplier).get();
	}
	public static <I extends RecipeInput, T extends Recipe<I>> ResourceLocation getRecipeId(T recipe)
	{
		RecipeManager recipeManager = getDistSpecificValue(() -> ClientUtils.getClient().level.getRecipeManager(), () -> Services.PLATFORM.getServerInstance().getRecipeManager());
		for (RecipeHolder<?> recipeEntry : recipeManager.getAllRecipesFor((RecipeType<T>) recipe.getType()))
		{
			if (recipeEntry.value() == recipe)
				return recipeEntry.id();
		}
		return null;
	}
	// this only accepts a fallback in cases of some coordinate not being finite / startPos being equal to endPos
	public static double getDeltaBetweenVectors(Vec3 pos, Vec3 startPos, Vec3 endPos, double fallback)
	{
		Double[] progresses = new Double[]
			{
				Mth.inverseLerp(pos.x, startPos.x, endPos.x),
				Mth.inverseLerp(pos.y, startPos.y, endPos.y),
				Mth.inverseLerp(pos.z, startPos.z, endPos.z)
			};
		return Arrays.stream(progresses).filter(Double::isFinite).mapToDouble(v -> v).average().orElse(fallback);
	}
	public static float calculateStep(float width, float minStep)
	{
		return width / Mth.ceil(width / minStep);
	}
	public static void writeBooleansCompact(ByteBuf buffer, boolean... booleans)
	{
		if (booleans.length == 0)
			return;
		
		byte currentByte = 0;
		for (int index = 0; index < booleans.length; index++)
		{
			int bit = index % 8;
			if (booleans[index])
				currentByte |= (byte) (1 << bit);
			if (bit == 7)
			{
				buffer.writeByte(currentByte);
				currentByte = 0;
			}
		}
		buffer.writeByte(currentByte);
	}
	public static void readBooleansCompact(ByteBuf buffer, Boolean... booleans)
	{
		int count = booleans.length;
		if (count == 0)
			return;
		byte[] buf = new byte[count >> 3];
		buffer.readBytes(buf);
		for (int index = 0; index < count; index++)
		{
			int bit = index % 8;
			byte currentByte = buf[index >> 3];
			booleans[index] = (currentByte >> bit & 1) == 1;
		}
	}
	public static boolean[] readBooleansCompact(ByteBuf buffer, int count)
	{
		if (count <= 0)
			return new boolean[0];
		byte[] buf = new byte[count >> 3];
		boolean[] booleans = new boolean[count];
		buffer.readBytes(buf);
		for (int index = 0; index < count; index++)
		{
			int bit = index % 8;
			byte currentByte = buf[index >> 3];
			booleans[index] = (currentByte >> bit & 1) == 1;
		}
		return booleans;
	}
	public static void setSquidDelay(LivingEntity entity, float delay)
	{
		if (entity instanceof Player player)
		{
			if (player.isLocalPlayer())
				SplatcraftKeyHandler.setSquidDelayInternal(delay);
		}
	}
	public static Vec3 createVec3(float value)
	{
		return new Vec3(value, value, value);
	}
	public static Vec3 createVec3(double value)
	{
		return new Vec3(value, value, value);
	}
	public record Result(float delay, float value)
	{
	}
	public static class InteractionEventResultDummy
	{
		private final boolean canceled;
		private boolean handSwing;
		public InteractionEventResultDummy(boolean handSwing, boolean canceled)
		{
			this.handSwing = handSwing;
			this.canceled = canceled;
		}
		public boolean shouldSwingHand()
		{
			return handSwing;
		}
		public boolean isCanceled()
		{
			return canceled;
		}
	}
	public static class TimedTextDisplayEntity extends Display.TextDisplay
	{
		public int lifeSpan;
		public TimedTextDisplayEntity(EntityType<TextDisplay> entityType, Level world, int lifeSpan)
		{
			super(entityType, world);
			this.lifeSpan = lifeSpan;
		}
		@Override
		public void tick()
		{
			super.tick();
			if (lifeSpan == 0)
				discard();
			lifeSpan--;
		}
	}
}