package net.splatcraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.Splatcraft;
import net.splatcraft.blocks.IColoredBlock;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.items.SpecialProviderItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.*;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.structs.BlockInkedResult;
import net.splatcraft.util.structs.InkColor;
import net.splatcraft.util.structs.RelativeBlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import oshi.util.tuples.Triplet;

import java.util.*;
import java.util.function.Function;

public class InkBlockUtils
{
	public static BlockInkedResult playerInkBlock(@Nullable Player player, Level world, BlockPos pos, InkColor color, Direction direction, InkType inkType, float damage)
	{
		BlockInkedResult inked = inkBlock(world, pos, color, direction, inkType, damage);
		
		if (player != null && inked == BlockInkedResult.SUCCESS)
		{
			player.awardStat(SplatcraftStats.BLOCKS_INKED);
		}
		
		return inked;
	}
	public static Direction getRandomInkedFace(Level world, BlockPos pos)
	{
		if (!ChunkInkCapability.hasAndNotEmpty(world, pos))
			return null;
		ChunkInk chunkInk = ChunkInkCapability.getOrCreate(world, pos);
		ChunkInk.BlockEntry entry = chunkInk.getInk(RelativeBlockPos.fromAbsolute(pos));
		if (entry != null && entry.isInkedAny())
		{
			return Direction.from3DDataValue(Util.getRandom(entry.getActiveIndices(), world.random));
		}
		return null;
	}
	public static boolean clearInk(Level world, BlockPos pos, Direction direction, boolean removePermanent)
	{
		return clearInk(world, pos, direction.get3DDataValue(), removePermanent);
	}
	public static boolean clearInk(Level world, BlockPos pos, int index, boolean removePermanent)
	{
		if (!ChunkInkCapability.hasAndNotEmpty(world, pos))
			return false;
		
		ChunkInk worldInk = ChunkInkCapability.getOrCreate(world, pos);
		RelativeBlockPos offset = RelativeBlockPos.fromAbsolute(pos);
		
		if (worldInk.isInkedAny(offset))
		{
			if (worldInk.clearInk(offset, index, removePermanent))
			{
				if (!world.isClientSide())
				{
					if (worldInk.isInkedAny(offset))
						ChunkInkHandler.addInkToUpdate(world, pos);
					else
						ChunkInkHandler.addInkToRemove(world, pos);
				}
				return true;
			}
		}
		return false;
	}
	public static boolean clearBlock(Level world, BlockPos pos, boolean removePermanent)
	{
		if (!ChunkInkCapability.hasAndNotEmpty(world, pos))
			return false;
		
		ChunkInk worldInk = ChunkInkCapability.getOrCreate(world, pos);
		RelativeBlockPos offset = RelativeBlockPos.fromAbsolute(pos);
		ChunkInk.BlockEntry entry = worldInk.getInk(offset);
		if (entry != null)
		{
			if (worldInk.clearBlock(offset, removePermanent))
			{
				if (!world.isClientSide())
					ChunkInkHandler.addInkToRemove(world, pos);
				return true;
			}
		}
		return false;
	}
	public static BlockInkedResult inkBlock(@Nullable Entity entity, Level world, BlockPos pos, InkColor color, Direction face, InkType inkType, float damage)
	{
		if (entity instanceof Player player)
		{
			return playerInkBlock(player, world, pos, color, face, inkType, damage);
		}
		return inkBlock(world, pos, color, face, inkType, damage);
	}
	public static void awardTurfPoints(LivingEntity entity, ItemStack weaponStack, int points)
	{
		applyFunctionToProviderIfAny(entity, weaponStack, v -> v.incrementStoredPoints(points, WeaponBaseItem.getWeaponId(weaponStack)));
	}
	public static void setTurfPoints(LivingEntity entity, ItemStack weaponStack, int points)
	{
		applyFunctionToProviderIfAny(entity, weaponStack, v -> v.withStoredPoints(points, WeaponBaseItem.getWeaponId(weaponStack)));
	}
	private static void applyFunctionToProviderIfAny(LivingEntity entity, ItemStack weaponStack, Function<SplatcraftComponents.SpecialProviderData, SplatcraftComponents.SpecialProviderData> applier)
	{
		if (!(weaponStack.getItem() instanceof WeaponBaseItem<?> weaponItem))
			return;
		
		ResourceLocation weaponId = weaponItem.getSettingsAndValidId(weaponStack).getFirst();
		if (weaponId == null)
			return;
		
		// todo: OPTIMIZE THIS!!!! i suspect this will be pretty expensive
		ItemStack providerStack = CommonUtils.getStackAndIndexInInventory(entity, stack ->
		{
			if (stack.getItem() instanceof SpecialProviderItem provider)
			{
				SplatcraftComponents.SpecialProviderData data = provider.getData(stack);
				return data != null && data.testWeapon(weaponStack);
			}
			return false;
		}).getFirst();
		if (providerStack.isEmpty())
			return;
		
		SpecialProviderItem providerItem = (SpecialProviderItem) providerStack.getItem();
		providerItem.setData(providerStack, applier.apply(providerItem.getData(providerStack)));
	}
	public static BlockInkedResult inkBlock(Level world, BlockPos pos, InkColor color, Direction direction, InkType inkType, float damage)
	{
		return inkBlock(world, pos, color, direction.get3DDataValue(), inkType, damage);
	}
	public static BlockInkedResult inkBlock(Level world, BlockPos pos, InkColor color, int index, InkType inkType, float damage)
	{
		if (isUninkable(world, pos, Direction.from3DDataValue(index)))
			return BlockInkedResult.FAIL;
		
		if (!world.getEntitiesOfClass(SpawnShieldEntity.class, new AABB(pos), v -> !ColorUtils.colorEquals(world, pos, ColorUtils.getEntityColor(v), color)).isEmpty())
			return BlockInkedResult.FAIL;
		
		BlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof IColoredBlock coloredBlock)
		{
			BlockInkedResult result = coloredBlock.inkBlock(world, pos, color, damage, inkType);
			if (result != BlockInkedResult.PASS)
				return result;
		}
		
		if (!SplatcraftGameRules.getLocalizedRule(world, pos, SplatcraftGameRules.INKABLE_GROUND))
			return BlockInkedResult.FAIL;
		
		LevelChunk chunk = world.getChunkAt(pos);
		ChunkInk worldInk = ChunkInkCapability.getOrCreate(chunk);
		RelativeBlockPos offset = RelativeBlockPos.fromAbsolute(pos);
		ChunkInk.BlockEntry entry = worldInk.getInk(offset);
		
		boolean isInked = entry != null && entry.isInked(index);
		if (entry != null && entry.immutable)
			return BlockInkedResult.IS_PERMANENT;
		
		boolean sameColor = isInked && entry.color(index) == color;
		
		if (sameColor && entry.type(index) == inkType)
			return BlockInkedResult.ALREADY_INKED;
		
		worldInk.ink(offset, index, color, inkType);
		chunk.setUnsaved(true);
		
		if (index == 1) // facing up
			if (SplatcraftGameRules.getLocalizedRule(world, pos.above(), SplatcraftGameRules.INK_DESTROYS_FOLIAGE) &&
				isBlockFoliage(world.getBlockState(pos.above())))
				world.destroyBlock(pos.above(), true);
		
		if (!world.isClientSide())
			ChunkInkHandler.addInkToUpdate(world, pos);
		
		return sameColor ? BlockInkedResult.ALREADY_INKED : BlockInkedResult.SUCCESS;
	}
	public static void forEachInkedBlockInBounds(Level world, final AABB bounds, InkedBlockConsumer action)
	{
		int chunkMinX = (int) bounds.minX >> 4;
		int chunkMinZ = (int) bounds.minZ >> 4;
		int chunkmaxX = (int) bounds.maxX >> 4;
		int chunkmaxZ = (int) bounds.maxZ >> 4;
		for (int x = chunkMinX; x <= chunkmaxX; x++)
			for (int z = chunkMinZ; z <= chunkmaxZ; z++)
			{
				ChunkPos chunkPos = new ChunkPos(x, z);
				if (!ChunkInkCapability.hasAndNotEmpty(world, chunkPos))
					continue;
				Set<Map.Entry<RelativeBlockPos, ChunkInk.BlockEntry>> uhhh = ChunkInkCapability.getOrCreate(world, chunkPos).getInkInChunk().entrySet();
				List<Map.Entry<RelativeBlockPos, ChunkInk.BlockEntry>> entries;
				synchronized (uhhh)
				{
					entries = new ArrayList<>(uhhh);
				}
				for (var ink : entries)
				{
					BlockPos blockPos = ink.getKey().toAbsolute(chunkPos);
					if (bounds.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ()))
					{
						action.accept(blockPos, ink.getValue());
					}
				}
			}
	}
	public static boolean isBlockFoliage(BlockState state)
	{
		return state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.REPLACEABLE);
	}
	public static BlockState getInkState(InkType inkType)
	{
		return (inkType == null ? InkType.NORMAL : inkType).block.defaultBlockState();
	}
	public static @Nullable ChunkInk.BlockEntry getInkBlock(Level world, BlockPos pos)
	{
		if (!ChunkInkCapability.hasAndNotEmpty(world, pos))
			return null;
		
		return ChunkInkCapability.getOrCreate(world, pos).getInk(RelativeBlockPos.fromAbsolute(pos));
	}
	public static ChunkInk.InkEntry getInkInFace(Level world, BlockPos pos, Direction direction)
	{
		return getInkBlock(world, pos).get(direction.get3DDataValue());
	}
	public static boolean isInked(Level world, BlockPos pos, Direction direction)
	{
		return isInked(world, pos, direction.get3DDataValue());
	}
	public static boolean isInked(Level world, BlockPos pos, int index)
	{
		return ChunkInkCapability.has(world, pos) && ChunkInkCapability.getOrCreate(world, pos).isInked(RelativeBlockPos.fromAbsolute(pos), index);
	}
	public static boolean isInkedAny(Level world, BlockPos pos)
	{
		return ChunkInkCapability.has(world, pos) && ChunkInkCapability.getOrCreate(world, pos).isInkedAny(RelativeBlockPos.fromAbsolute(pos));
	}
	public static boolean canInkFromFace(Level world, BlockPos pos, Direction face)
	{
		if (!(world.getBlockState(pos).getBlock() instanceof IColoredBlock) && isUninkable(world, pos, face))
			return false;
		
		return canInkPassthrough(world, pos.relative(face)) || !world.getBlockState(pos.relative(face)).is(SplatcraftTags.Blocks.BLOCKS_INK);
	}
	public static boolean isUninkable(Level world, BlockPos pos, Direction direction)
	{
		return isUninkable(world, pos, direction, false);
	}
	public static boolean isUninkable(Level world, BlockPos pos, Direction direction, boolean checkGamemode)
	{
		if (InkedBlock.isTouchingLiquid(world, pos, direction))
			return true;
		
		if (isBlockUninkable(world, pos))
			return true;
		
		if (!checkGamemode)
			return false;
		
		if (!SplatcraftGameRules.getLocalizedRule(world, pos, SplatcraftGameRules.BLOCK_DESTROY_INK))
			return false;
		
		BlockState blockState = world.getBlockState(pos);
		BlockState occludingBlockState = world.getBlockState(pos.relative(direction));
		VoxelShape blockCollision = blockState.getCollisionShape(world, pos).getFaceShape(direction);
		VoxelShape occludingCollision = occludingBlockState.getCollisionShape(world, pos.relative(direction)).getFaceShape(direction.getOpposite());
		
		return Shapes.blockOccudes(blockCollision, occludingCollision, direction);
	}
	public static boolean isBlockUninkable(Level world, BlockPos pos)
	{
		BlockState state = world.getBlockState(pos);
		
		if (state.is(SplatcraftTags.Blocks.UNINKABLE_BLOCKS))
			return true;
		
		if (!state.is(SplatcraftTags.Blocks.RENDER_AS_CUBE) && state.getRenderShape() != RenderShape.MODEL)
			return true;
		
		return canInkPassthrough(world, pos);
	}
	public static boolean canInkPassthrough(Level world, BlockPos pos)
	{
		BlockState state = world.getBlockState(pos);
		
		return state.getCollisionShape(world, pos).isEmpty() || world.getBlockState(pos).is(SplatcraftTags.Blocks.INK_PASSTHROUGH);
	}
	public static boolean canSquidHide(LivingEntity entity)
	{
		if (EntityAction.hasSpecificEntityAction(entity, SuperJumpCommand.SuperJump.class))
		{
			return false;
		}
		
		EntityInfo entityInfo = Components.ENTITY_INFO.get(entity);
		if (entityInfo == null)
			return false;
		
		if (entityInfo.getSquidSurgeState() >= EntityInfo.MIN_SQUID_SURGE_CHARGE)
			return false;
		
		return !entity.isSpectator() && (canSquidSwim(entity) || entityInfo.getClimbedDirection().isPresent());
	}
	public static boolean canSquidSwim(LivingEntity entity)
	{
		boolean canSwim = false;
		
		Optional<BlockPos> down = getBlockStandingOnPos(entity.position().add(0, 10e-4, 0), entity.level(), 0.1);
		if (down.isEmpty())
			return false;
		Block standingBlock = entity.level().getBlockState(down.get()).getBlock();
		
		if (isInked(entity.level(), down.get(), Direction.UP))
			return ColorUtils.colorEquals(entity.level(), down.get(), ColorUtils.getEntityColor(entity), getInkBlock(entity.level(), down.get()).color(Direction.UP.get3DDataValue()));
		
		if (standingBlock instanceof IColoredBlock coloredBlock)
			canSwim = coloredBlock.canSwim();
		
		return canSwim && ColorUtils.colorEquals(entity, entity.level().getBlockEntity(down.get()));
	}
	public static Optional<BlockPos> getBlockStandingOnPos(Entity entity)
	{
		return getBlockStandingOnPos(entity, 0.1);
	}
	public static Optional<BlockPos> getBlockStandingOnPos(Entity entity, double maxDepth)
	{
		return getBlockStandingOnPos(entity.position(), entity.level(), maxDepth, entity);
	}
	public static Optional<BlockPos> getBlockStandingOnPos(Vec3 position, Level level, double maxDepth)
	{
		return getBlockStandingOnPos(position, level, maxDepth, null);
	}
	public static Optional<BlockPos> getBlockStandingOnPos(Vec3 position, Level level, double maxDepth, Entity clipContextEntity)
	{
		BlockHitResult result = level.clip(new ClipContext(position, position.subtract(0, maxDepth, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, clipContextEntity == null ? CollisionContext.empty() : CollisionContext.of(clipContextEntity)));
		if (result.getType() == HitResult.Type.MISS)
			return Optional.empty();
		return Optional.of(result.getBlockPos());
	}
	public static Optional<Float> getDistanceToFloor(Vec3 startPoint, Level level, float maxDepth, Entity clipContextEntity)
	{
		Vec3 endPoint = startPoint.subtract(0, maxDepth, 0);
		BlockHitResult result = level.clip(new ClipContext(startPoint, endPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, clipContextEntity == null ? CollisionContext.empty() : CollisionContext.of(clipContextEntity)));
		
		if (result.getType() == HitResult.Type.MISS)
			return Optional.empty();
		return Optional.of((float) startPoint.distanceTo(result.getLocation()));
	}
	public static boolean onEnemyInk(LivingEntity entity)
	{
		if (!entity.onGround())
			return false;
		
		Optional<BlockPos> pos = getBlockStandingOnPos(entity);
		if (pos.isEmpty())
			return false;
		
		if (isInked(entity.level(), pos.get(), Direction.UP))
			return !canSquidSwim(entity);
		else if (entity.level().getBlockState(pos.get()).getBlock() instanceof IColoredBlock coloredBlock)
			return coloredBlock.canDamage() && ColorUtils.getInkColor(entity.level(), pos.get()).isValid() && !canSquidSwim(entity);
		else return false;
	}
	public static Optional<Direction> getSquidClimbDirection(final LivingEntity entity, final float movementSideways, final float movementForward, final Optional<Direction> previousDirection)
	{
		if (onEnemyInk(entity))
			return Optional.empty();
		
		final Vector2f rotatedImpulse = PlayerMovementHandler.getRotatedImpulse(movementSideways, movementForward, entity.getYRot());
		final AABB originalBox = entity.getBoundingBox();
		final Vec3 deltaMovement = entity.getDeltaMovement().add(0, entity.isNoGravity() ? 0 : entity.getGravity(), 0);
		
		AABB extendedBox = originalBox.expandTowards(deltaMovement);
//		CommonUtils.showBoundingBoxCorners(entity.level(), extendedBox);
		
		Optional<Triplet<Direction, VoxelShape, BlockPos>> collisionData = checkSquidCollisions(entity, extendedBox);
		
		// this normally executes if the player is on a wall but is not inputting anything / only going upwards, since going downwards is treated like going out of the wall
		if (collisionData.isEmpty())
		{
			if (previousDirection.isPresent())
			{
				final Vec3 inputVector = PlayerMovementHandler.processImpulse(previousDirection.get(), rotatedImpulse);
				if (deltaMovement.y >= -0.1 || inputVector.y >= 0)
				{
					Vec3 normal = Vec3.atLowerCornerOf(previousDirection.get().getNormal()).scale(-0.1);
					extendedBox = originalBox.expandTowards(normal);
					
					collisionData = checkSquidCollisions(entity, extendedBox);
					if (!collisionData.map(Triplet::getA).equals(previousDirection))
						return Optional.empty();
				}
			}
			else
			{
				final Vec3 inputVector = new Vec3(rotatedImpulse.x, 0, rotatedImpulse.y).scale(0.01);
				extendedBox = originalBox.expandTowards(inputVector).move(0, 10e-4, 0);
				collisionData = checkSquidCollisions(entity, extendedBox);
			}
		}
		if (collisionData.isPresent())
		{
/*
			// check if the entity can "step up" the collision like with stairs, via a poor way obviously
			float maxUpStep = entity.maxUpStep();
			extendedBox = extendedBox.expandTowards(0, maxUpStep, 0);
			collisions = new BlockCollisions<>(entity.level(), entity, extendedBox, false, Pair::of);
			Tuple<Optional<Direction>, VoxelShape> upStepCollision = checkSquidCollisions(entity, collisions, extendedBox, entityCenter);
			if (upStepCollision.getA().isEmpty() || (
				upStepCollision.getA().get() == originalDirection.getA().get() &&
					upStepCollision.getB().max(Direction.Axis.Y) - originalDirection.getB().max(Direction.Axis.Y) <= maxUpStep))
			{
				// if there is no collision, we can up step!!! (however this will be handled by the movement code executed after this so
				// we fake not actually climbing
				return Optional.empty();
			}
*/
		}
		return collisionData.map(Triplet::getA);
	}
	private static Optional<Triplet<Direction, VoxelShape, BlockPos>> checkSquidCollisions(final LivingEntity entity, final AABB extendedBox)
	{
		return checkSquidCollisions(entity, extendedBox, entity.getBoundingBox().getCenter());
	}
	private static Optional<Triplet<Direction, VoxelShape, BlockPos>> checkSquidCollisions(final LivingEntity entity, final AABB extendedBox, final Vec3 entityCenter)
	{
		final Direction.Axis[] horizontalAxis = {Direction.Axis.X, Direction.Axis.Z};
		final VoxelShape collisionShape = Shapes.create(extendedBox);
		
		double minDistanceToBlock = Double.POSITIVE_INFINITY;
		
		Direction collidedDirection = null;
		VoxelShape usedJoined = null;
		BlockPos usedBlockPos = null;
		final BlockCollisions<Pair<BlockPos, VoxelShape>> collisions = new BlockCollisions<>(entity.level(), entity, extendedBox, false, Pair::of);
		
		while (collisions.hasNext())
		{
			Pair<BlockPos, VoxelShape> collidedBlock = collisions.next();
			BlockPos blockPos = collidedBlock.getFirst();
			VoxelShape voxelShape = collidedBlock.getSecond();
			
			if (voxelShape.isEmpty())
				continue;
			
			VoxelShape joined = Shapes.join(voxelShape, collisionShape, BooleanOp.AND)
				.move(
					-entityCenter.x(), -entityCenter.y(), -entityCenter.z());
			
			if (joined.isEmpty())
				continue;
			
			for (Direction.Axis axis : horizontalAxis)
			{
				double minDist = joined.min(axis);
				double maxDist = joined.max(axis);
				
				// since the joined shape is relative to the entity, having the same sign in an axis means the collision on the given axis was on a face, or something
				if (Math.signum(minDist) == Math.signum(maxDist))
				{
					// yes the distance can be calculated before but i dont wanna waste 3 cpu cycles >:(
					double distanceToBlock = Vec3.atCenterOf(blockPos).distanceToSqr(entityCenter);
					if (distanceToBlock < minDistanceToBlock)
					{
						minDistanceToBlock = distanceToBlock;
						
						Direction.AxisDirection axisDirection = Math.signum(minDist) == 1 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE;
						Direction directionCandidate = Direction.fromAxisAndDirection(axis, axisDirection).getOpposite();
						
						ChunkInk.BlockEntry inkData = getInkBlock(entity.level(), blockPos);
						if (inkData == null || inkData.get(directionCandidate.get3DDataValue()) == null ||
							!ColorUtils.colorEquals(entity.level(), blockPos, ColorUtils.getEntityColor(entity), inkData.color(directionCandidate.get3DDataValue()))
						)
						{
							continue;
						}
						
						collidedDirection = directionCandidate;
						usedJoined = joined;
						usedBlockPos = blockPos;
					}
				}
			}
		}
		if (collidedDirection == null)
			return Optional.empty();
		return Optional.of(new Triplet<>(collidedDirection, usedJoined, usedBlockPos));
	}
	public static InkBlockUtils.InkType getInkType(LivingEntity entity)
	{
		return Components.ENTITY_INFO.getOptional(entity).map(EntityInfo::getInkType).orElse(InkType.NORMAL);
	}
	public static InkType getInkTypeFromStack(ItemStack stack)
	{
		if (!stack.isEmpty())
			for (InkType t : InkType.values())
				if (t.getRepItem().equals(stack.getItem()))
					return t;
		
		return InkType.NORMAL;
	}
	public static boolean hasInkType(ItemStack stack)
	{
		if (!stack.isEmpty())
			for (InkType t : InkType.values())
				if (t.getRepItem().equals(stack.getItem()))
					return true;
		return false;
	}
	public enum InkType implements Comparable<InkType>, StringRepresentable
	{
		NORMAL(0, Splatcraft.identifierOf("normal"), SplatcraftBlocks.inkedBlock.value()),
		GLOWING(1, Splatcraft.identifierOf("glowing"), SplatcraftItems.splatfestBand.value(), SplatcraftBlocks.glowingInkedBlock.value()),
		CLEAR(2, Splatcraft.identifierOf("clear"), SplatcraftItems.clearBand.value(), SplatcraftBlocks.clearInkedBlock.value());
		public static final Map<ResourceLocation, InkType> IDENTIFIER_MAP = Map.of(
			Splatcraft.identifierOf("normal"), NORMAL,
			Splatcraft.identifierOf("glowing"), GLOWING,
			Splatcraft.identifierOf("clear"), CLEAR
		);
		public static final Codec<InkType> CODEC = StringRepresentable.fromEnum(InkType::values);
		public static final StreamCodec<ByteBuf, InkType> STREAM_CODEC = CodecUtils.createEnumPacketCodec(InkType::values);
		private final ResourceLocation name;
		private final Item repItem;
		private final InkedBlock block;
		private final byte id;
		InkType(int id, ResourceLocation name, Item repItem, InkedBlock inkedBlock)
		{
			this.id = (byte) id;
			this.name = name;
			this.repItem = repItem;
			block = inkedBlock;
		}
		InkType(int id, ResourceLocation name, InkedBlock inkedBlock)
		{
			this(id, name, Items.AIR, inkedBlock);
		}
		public static InkType fromId(int id)
		{
			return values()[id];
		}
		public ResourceLocation getName()
		{
			return name;
		}
		public Item getRepItem()
		{
			return repItem;
		}
		@Override
		public String toString()
		{
			return name.toString();
		}
		public String getIdString()
		{
			return getName().toString();
		}
		public byte getId()
		{
			return id;
		}
		@Override
		public @NotNull String getSerializedName()
		{
			return name();
		}
	}
	public interface InkedBlockConsumer
	{
		void accept(BlockPos pos, ChunkInk.BlockEntry ink);
	}
}