package net.splatcraft.util;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
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
import net.minecraft.world.phys.shapes.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.SplatcraftKeyHandler;
import net.splatcraft.client.renderer.InkSquidRenderer;
import net.splatcraft.data.capabilities.structs.SquidInfo;
import net.splatcraft.data.capabilities.structs.WeaponInfo;
import net.splatcraft.entities.InkSquidEntity;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.CommonRecords;
import net.splatcraft.mixin.accessors.VoxelShapeAccessor;
import net.splatcraft.platform.Components;
import net.splatcraft.platform.ModSide;
import net.splatcraft.platform.Services;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.structs.InkColor;
import org.apache.commons.lang3.math.IEEE754rUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import oshi.util.tuples.Triplet;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CommonUtils
{
	public static final EntityDataSerializer<Vector2f> VEC2_DATA_HANDLER = EntityDataSerializer.forValueType(CodecUtils.Codecs.VECTOR2F_STREAM_CODEC);
	public static final EntityDataSerializer<InkColor> INKCOLOR_DATA_HANDLER = EntityDataSerializer.forValueType(InkColor.STREAM_CODEC);
	public static final EntityDataSerializer<Vec3> VEC3_DATA_HANDLER = EntityDataSerializer.forValueType(CodecUtils.Codecs.VEC_3_STREAM_CODEC);
	public static final EntityDataSerializer<Optional<Vec3>> OPTIONAL_VEC3_DATA_HANDLER = EntityDataSerializer.forValueType(ByteBufCodecs.optional(CodecUtils.Codecs.VEC_3_STREAM_CODEC));
	public static final EntityDataSerializer<Optional<Direction>> OPTIONAL_DIRECTION_DATA_HANDLER = EntityDataSerializer.forValueType(ByteBufCodecs.optional(Direction.STREAM_CODEC));
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
		for (Vec3 corner : getBoundingBoxCorners(aabb))
		{
			spawnTestParticle(level, ParticleTypes.BUBBLE, corner);
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
	public static List<ItemStack> getItemsInInventory(LivingEntity entity, Predicate<ItemStack> predicate)
	{
		ImmutableList.Builder<ItemStack> builder = ImmutableList.builder();
		if (predicate.test(entity.getItemInHand(InteractionHand.OFF_HAND)))
		{
			builder.add(entity.getItemInHand(InteractionHand.OFF_HAND));
		}
		if (predicate.test(entity.getItemInHand(InteractionHand.MAIN_HAND)))
		{
			builder.add(entity.getItemInHand(InteractionHand.MAIN_HAND));
		}

		if (entity instanceof Player player)
		{
			Inventory inventory = player.getInventory();
			for (int i = 0; i < inventory.getContainerSize(); ++i)
			{
				ItemStack stack = inventory.getItem(i);
				if (predicate.test(stack))
					builder.add(stack);
			}
		}

		return builder.build();
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
		return false;
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
		return Components.WEAPON_INFO.hasAnd(entity, WeaponInfo::hasHigherStartup) ? withCancel : withoutCancel;
	}
	public static float startupSquidSwitch(LivingEntity entity, CommonRecords.ShotDataRecord shotData)
	{
		return returnValueDependantOnSquidCancel(entity, shotData.squidStartupTicks(), shotData.startupTicks());
	}
	public static float nextTriangular(RandomSource random, float mode, float deviation)
	{
		return mode + deviation * (random.nextFloat() - random.nextFloat());
	}
	public static boolean isOnTurretState(LivingEntity entity)
	{
		return EntityAction.hasSpecificEntityActionAnd(entity, action -> action.getRollState() == DualieItem.DodgeRollAction.RollState.TURRET, DualieItem.DodgeRollAction.class);
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
	public static Vec3 limitTo(AABB box, Vec3 position)
	{
		return new Vec3(
			Math.clamp(position.x, box.minX, box.maxX),
			Math.clamp(position.y, box.minY, box.maxY),
			Math.clamp(position.z, box.minZ, box.maxZ)
		);
	}
	public static boolean isSquid(LivingEntity entity)
	{
		if (entity instanceof InkSquidEntity)
			return true;

		return Components.SQUID_INFO.hasAnd(entity, SquidInfo::isSquid);
	}
	public static <I, O> ReseteableMemoizedFunction<I, O> memoizeResetable(Function<I, O> function)
	{
		return new ReseteableMemoizedFunction<>(function);
	}
	public static <O> ReseteableMemoizedSupplier<O> memoizeResetable(Supplier<O> supplier)
	{
		return new ReseteableMemoizedSupplier<>(supplier);
	}
	public static List<VoxelShape> createShapes(BlockGetter getter, Collection<BlockPos> blocks, BlockPos localZero)
	{
		if (blocks.isEmpty())
			return List.of();

		// ew
		final Vec3i[] offsets = new Vec3i[] {
			new Vec3i(0, 1, 0),
			new Vec3i(-1, 0, 0),
			new Vec3i(0, 0, 1),
			new Vec3i(0, 0, -1),
			new Vec3i(1, 0, 0),
			new Vec3i(0, -1, 0),
			new Vec3i(0, 1, 1),
			new Vec3i(0, 1, -1),
			new Vec3i(1, 1, 0),
			new Vec3i(0, -1, 1),
			new Vec3i(0, -1, -1),
			new Vec3i(-1, 1, 0),
			new Vec3i(1, 0, 1),
			new Vec3i(-1, 0, 1),
			new Vec3i(-1, 0, -1),
			new Vec3i(-1, -1, 0),
			new Vec3i(1, 0, -1),
			new Vec3i(1, -1, 0),
			new Vec3i(1, 1, -1),
			new Vec3i(-1, -1, -1),
			new Vec3i(1, -1, -1),
			new Vec3i(-1, 1, 1),
			new Vec3i(-1, 1, -1),
			new Vec3i(-1, -1, 1),
			new Vec3i(1, -1, 1),
			new Vec3i(1, 1, 1)
		};

		HashMap<BlockPos, VoxelShape> shapes = blocks.stream().map(v ->
		{
			BlockPos localPos = v.subtract(localZero);
			VoxelShape shape = getter.getBlockState(v).getVisualShape(getter, v, CollisionContext.empty());
			if (shape.isEmpty())
				return null;

			DiscreteVoxelShape bounds = ((VoxelShapeAccessor) shape).getShape();
			if (bounds.getXSize() < 1 || bounds.getZSize() < 1)
			{
				return null;
			}
			return Map.entry(localPos, shape.move(localPos.getX(), localPos.getY(), localPos.getZ()).optimize());
		}).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> x, HashMap::new));

		ImmutableList.Builder<VoxelShape> builder = ImmutableList.builder();

		while (!shapes.isEmpty())
		{
			Map.Entry<BlockPos, VoxelShape> firstEntry = Iterables.getFirst(shapes.entrySet(), null);
			final VoxelShape[] currentShape = {firstEntry.getValue()};
			while (true)
			{
				// TODO: optimize this correctly pls i cannot do Breadth-first search without processing the same block twice
				boolean joined = false;
				List<Map.Entry<BlockPos, VoxelShape>> shapesToAdd = new ArrayList<>();
				for (Map.Entry<BlockPos, VoxelShape> entry : shapes.entrySet())
				{
					if (shapesCollide(currentShape[0], entry.getValue()))
					{
						shapesToAdd.add(entry);
						joined = true;
					}
				}

				shapesToAdd.forEach(v ->
				{
					currentShape[0] = Shapes.joinUnoptimized(currentShape[0], v.getValue(), BooleanOp.OR);
					shapes.remove(v.getKey());
				});
				shapesToAdd.clear();

				if (!joined)
					break;
			}
			
/*
			while (true)
			{
				boolean searchingAdjacent = true;
				
				for (Vec3i offset : offsets)
				{
					BlockPos newPos = cursor.offset(offset);
					
					if (!shapes.containsKey(newPos))
						continue;
					
					VoxelShape otherShape = shapes.get(newPos);
					
					if (shapesCollide(currentShape, otherShape))
					{
						currentShape = Shapes.join(currentShape, otherShape, BooleanOp.OR);
						
						cursor = newPos;
						searchingAdjacent = false;
					}
				}
				if (counter == 10)
				{
//					currentShape = currentShape.optimize();
					counter = -1;
				}
				
				counter++;
				
				if (searchingAdjacent)
					break;
			}
*/

			builder.add(currentShape[0].optimize().optimize());
		}

		return builder.build();
	}
	private static boolean shapesCollide(VoxelShape shape1, VoxelShape shape2)
	{
		AtomicBoolean result = new AtomicBoolean(false);

		shape1.forAllBoxes((x1, y1, z1, x2, y2, z2) ->
		{
			if (result.get())
				return;

			shape2.forAllBoxes((x3, y3, z3, x4, y4, z4) ->
			{
				if (result.get())
					return;

				double tolerance = 10e-5;

				if (x1 - x4 <= tolerance && x2 - x3 >= -tolerance && y1 - y4 <= tolerance && y2 - y3 >= -tolerance && z1 - z4 <= tolerance && z2 - z3 >= -tolerance)
					result.set(true);
			});
		});

		return result.get();
	}
	public static AABB createInfiniteAABBFor(Vec3 position, Vec3 forward, float margin)
	{
		AABB infiniteAABB = new AABB(position, forward.scale(Double.POSITIVE_INFINITY));
		return infiniteAABB.inflate(margin, margin, margin);
	}
	/**
	 * Some collision thing that returns collision data of an "relative" bounding box with a ray.
	 *
	 * @param rayDirection The normalized direction vector of the ray to check with.
	 * @param relativeBox  An {@link AABB} that has relative coordinates in terms of the ray.
	 * @return A {@link Pair} that has the distance as the first element, and the relative impact position as the second.
	 */
	// these 5 methods comes from https://stackoverflow.com/questions/34952680/distance-between-a-ray-and-a-bound-box
	// yes stack overflow (and Raidho Coaxil with 41 of reputation score and 3 bronze badges who probably had access
	// to better search engines than now i suppose because i cant find this code anywhere else) comes to save
	// me from eternal torment
	public static Triplet<Double, Vec3, Double> getRayDistance(Vec3 rayDirection, AABB relativeBox)
	{
		Vec3 impactPos = getClosestPointToRay(rayDirection, relativeBox);

		double t = Math.max(0.0, rayDirection.dot(impactPos) / rayDirection.lengthSqr()); // orthogonal projection??? in my code???? its more likely than you think!
		double x = rayDirection.x * t - impactPos.x();
		double y = rayDirection.y * t - impactPos.y();
		double z = rayDirection.z * t - impactPos.z();
		return new Triplet<>(Math.sqrt(x * x + y * y + z * z), impactPos, t);
	}
	public static Triplet<Float, Vector3f, Float> getRayDistance(Vector3f rayDirection, AABB relativeBox)
	{
		Vector3f impactPos = getClosestPointToRay(rayDirection, relativeBox);

		float t = Math.max(0.0f, rayDirection.dot(impactPos) / rayDirection.lengthSquared());
		float x = rayDirection.x * t - impactPos.x();
		float y = rayDirection.y * t - impactPos.y();
		float z = rayDirection.z * t - impactPos.z();
		return new Triplet<>(Mth.sqrt(x * x + y * y + z * z), impactPos, t);
	}
	/**
	 * Some collision thing that returns collision data of an "relative" bounding box with an segment.
	 *
	 * @param lineVector  The line vector to check with.
	 * @param relativeBox An {@link AABB} that has relative coordinates in terms of the ray.
	 * @return A {@link Pair} that has the distance of the AABB to the line as the first element, and the relative impact position as the second.
	 */
	public static Triplet<Double, Vec3, Double> getLineDistance(Vec3 lineVector, AABB relativeBox)
	{
		Vec3 impactPos = getClosestPointToRay(lineVector, relativeBox);

		double t = Math.clamp(lineVector.dot(impactPos) / lineVector.lengthSqr(), 0.0, 1.0);
		double x = lineVector.x * t - impactPos.x();
		double y = lineVector.y * t - impactPos.y();
		double z = lineVector.z * t - impactPos.z();
		return new Triplet<>(Math.sqrt(x * x + y * y + z * z), impactPos, t);
	}
	public static Triplet<Float, Vector3f, Float> getLineDistance(Vector3f lineVector, AABB relativeBox)
	{
		Vector3f impactPos = getClosestPointToRay(lineVector, relativeBox);

		float t = Math.clamp(lineVector.dot(impactPos) / lineVector.lengthSquared(), 0.0f, 1.0f);
		float x = lineVector.x * t - impactPos.x();
		float y = lineVector.y * t - impactPos.y();
		float z = lineVector.z * t - impactPos.z();
		return new Triplet<>(Mth.sqrt(x * x + y * y + z * z), impactPos, t);
	}
	private static @NotNull Vec3 getClosestPointToRay(Vec3 lineVector, AABB relativeBox)
	{
		double tx1 = relativeBox.minX / lineVector.x;
		double tx2 = relativeBox.maxX / lineVector.x;
		double ty1 = relativeBox.minY / lineVector.y;
		double ty2 = relativeBox.maxY / lineVector.y;
		double tz1 = relativeBox.minZ / lineVector.z;
		double tz2 = relativeBox.maxZ / lineVector.z;

		double[] xPair = lineVector.x < 0 ? new double[] {tx2, tx1} : new double[] {tx1, tx2};
		double[] yPair = lineVector.y < 0 ? new double[] {ty2, ty1} : new double[] {ty1, ty2};
		double[] zPair = lineVector.z < 0 ? new double[] {tz2, tz1} : new double[] {tz1, tz2};

		double p1 = Math.max(0.0, IEEE754rUtils.max(xPair[0], yPair[0], zPair[0]));
		double p2 = Math.max(0.0, IEEE754rUtils.min(xPair[1], yPair[1], zPair[1]));
		double pSum = p1 + p2;

		double x = Mth.clamp((lineVector.x * pSum) / 2, relativeBox.minX, relativeBox.maxX);
		double y = Mth.clamp((lineVector.y * pSum) / 2, relativeBox.minY, relativeBox.maxY);
		double z = Mth.clamp((lineVector.z * pSum) / 2, relativeBox.minZ, relativeBox.maxZ);
		return new Vec3(x, y, z);
	}
	private static @NotNull Vector3f getClosestPointToRay(Vector3f lineVector, AABB relativeBox)
	{
		float minX = (float) relativeBox.minX;
		float maxX = (float) relativeBox.maxX;
		float minY = (float) relativeBox.minY;
		float maxY = (float) relativeBox.maxY;
		float minZ = (float) relativeBox.minZ;
		float maxZ = (float) relativeBox.maxZ;

		float tx1 = minX / lineVector.x;
		float tx2 = maxX / lineVector.x;
		float ty1 = minY / lineVector.y;
		float ty2 = maxY / lineVector.y;
		float tz1 = minZ / lineVector.z;
		float tz2 = maxZ / lineVector.z;

		float[] xPair = lineVector.x < 0 ? new float[] {tx2, tx1} : new float[] {tx1, tx2};
		float[] yPair = lineVector.y < 0 ? new float[] {ty2, ty1} : new float[] {ty1, ty2};
		float[] zPair = lineVector.z < 0 ? new float[] {tz2, tz1} : new float[] {tz1, tz2};

		float p1 = Math.max(0.0f, IEEE754rUtils.max(xPair[0], yPair[0], zPair[0]));
		float p2 = Math.max(0.0f, IEEE754rUtils.min(xPair[1], yPair[1], zPair[1]));

		float pSum = p1 + p2;

		float x = Mth.clamp((lineVector.x * pSum) / 2, minX, maxX);
		float y = Mth.clamp((lineVector.y * pSum) / 2, minY, maxY);
		float z = Mth.clamp((lineVector.z * pSum) / 2, minZ, maxZ);
		return new Vector3f(x, y, z);
	}
	public static Vec3 getClosestPoint(Vec3 rayDirection, Vec3 relativePoint)
	{
		double t = Math.max(0.0, rayDirection.dot(relativePoint) / rayDirection.lengthSqr());

		return rayDirection.scale(t - 1);
	}
	public static Vec3 @NotNull [] getBoundingBoxCorners(AABB aabb)
	{
		double minX = aabb.minX;
		double minY = aabb.minY;
		double minZ = aabb.minZ;
		double maxX = aabb.maxX;
		double maxY = aabb.maxY;
		double maxZ = aabb.maxZ;

		return new Vec3[] {
			new Vec3(minX, minY, minZ),
			new Vec3(minX, maxY, minZ),
			new Vec3(minX, minY, maxZ),
			new Vec3(minX, maxY, maxZ),
			new Vec3(maxX, minY, minZ),
			new Vec3(maxX, maxY, minZ),
			new Vec3(maxX, minY, maxZ),
			new Vec3(maxX, maxY, maxZ)
		};
	}
	public static Pair<Vec3, Float> @NotNull [] getWeightedBoundingBoxTestPoints(AABB aabb)
	{
		Vec3[] corners = getBoundingBoxCorners(aabb);

		Pair<Vec3, Float>[] pairs = new Pair[9];
		for (int i = 0; i < 8; i++)
		{
			pairs[i] = new Pair<>(corners[i], 1f);
		}
		pairs[8] = new Pair<>(aabb.getCenter(), 5f);

		return pairs;
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
	public static class ReseteableMemoizedFunction<I, O> implements Function<I, O>
	{
		private final Function<I, O> function;
		private final Map<I, O> cache = new Object2ObjectOpenHashMap<>();
		public ReseteableMemoizedFunction(Function<I, O> function)
		{
			this.function = function;
		}
		public O apply(I object)
		{
			return cache.computeIfAbsent(object, function);
		}
		public void reset()
		{
			cache.clear();
		}
		@Override
		public String toString()
		{
			return "reseteablememoize/1[function=" + function + ", size=" + cache.size() + "]";
		}
	}
	public static class ReseteableMemoizedSupplier<O> implements java.util.function.Supplier<O>
	{
		private final java.util.function.Supplier<O> function;
		private O cachedResult;
		public ReseteableMemoizedSupplier(java.util.function.Supplier<O> supplier)
		{
			function = supplier;
		}
		public O get()
		{
			if (cachedResult == null)
				cachedResult = function.get();

			return cachedResult;
		}
		public void reset()
		{
			cachedResult = null;
		}
	}
}