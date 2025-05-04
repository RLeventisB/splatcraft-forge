package net.splatcraft.util;

import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.Splatcraft;
import net.splatcraft.blocks.IColoredBlock;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.items.SpecialProviderItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.registries.*;
import net.splatcraft.util.action.EntityAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
		ChunkInk chunkInk = ChunkInkCapability.get(world, pos);
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

		ChunkInk worldInk = ChunkInkCapability.get(world, pos);
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

		ChunkInk worldInk = ChunkInkCapability.get(world, pos);
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
		applyFunctionToProviderIfAny(entity, weaponStack, v -> v.incrementStoredPoints(points));
	}
	public static void setTurfPoints(LivingEntity entity, ItemStack weaponStack, int points)
	{
		applyFunctionToProviderIfAny(entity, weaponStack, v -> v.withStoredPoints(points));
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
		ChunkInk worldInk = ChunkInkCapability.get(chunk);
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
				Set<Map.Entry<RelativeBlockPos, ChunkInk.BlockEntry>> uhhh = ChunkInkCapability.get(world, chunkPos).getInkInChunk().entrySet();
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

		return ChunkInkCapability.get(world, pos).getInk(RelativeBlockPos.fromAbsolute(pos));
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
		return ChunkInkCapability.has(world, pos) && ChunkInkCapability.get(world, pos).isInked(RelativeBlockPos.fromAbsolute(pos), index);
	}
	public static boolean isInkedAny(Level world, BlockPos pos)
	{
		return ChunkInkCapability.has(world, pos) && ChunkInkCapability.get(world, pos).isInkedAny(RelativeBlockPos.fromAbsolute(pos));
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
		if (entity instanceof Player player && EntityAction.hasEntityAction(player) && EntityAction.getEntityAction(player) instanceof SuperJumpCommand.SuperJump)
		{
			return false;
		}
		EntityInfo playerInfo = EntityInfoCapability.get(entity);
		if (playerInfo == null)
			return false;

		return !entity.isSpectator() && (canSquidSwim(entity) || playerInfo.getClimbedDirection().isPresent() && playerInfo.getSquidSurgeCharge() < 20);
	}
	public static boolean canSquidSwim(LivingEntity entity)
	{
		boolean canSwim = false;

		BlockPos down = entity.getOnPos();
		Block standingBlock = entity.level().getBlockState(down).getBlock();

		if (isInked(entity.level(), down, Direction.UP))
			return ColorUtils.colorEquals(entity.level(), down, ColorUtils.getEntityColor(entity), getInkBlock(entity.level(), down).color(Direction.UP.get3DDataValue()));

		if (standingBlock instanceof IColoredBlock coloredBlock)
			canSwim = coloredBlock.canSwim();

		return canSwim && ColorUtils.colorEquals(entity, entity.level().getBlockEntity(down));
	}
	public static BlockPos getBlockStandingOnPos(Entity entity)
	{
		return getBlockStandingOnPos(entity, 0.6);
	}
	public static BlockPos getBlockStandingOnPos(Entity entity, double maxDepth)
	{
		BlockPos result;
		for (double i = 0; i >= -maxDepth + 0.1; i -= 0.1)
		{
			result = CommonUtils.createBlockPos(entity.getX(), entity.getY() + i, entity.getZ());

			VoxelShape shape = entity.level().getBlockState(result).getCollisionShape(entity.level(), result, CollisionContext.of(entity));
			shape.collide(Direction.Axis.Y, entity.getBoundingBox(), 0.0);

			if (!shape.isEmpty() && shape.bounds().minY <= entity.getY() - result.getY())
				return result;
		}
		return CommonUtils.createBlockPos(entity.getX(), entity.getY() - maxDepth, entity.getZ());
	}
	public static boolean onEnemyInk(LivingEntity entity)
	{
		if (!entity.onGround())
			return false;
		BlockPos pos = entity.getOnPos();

		if (isInked(entity.level(), pos, Direction.UP))
			return !canSquidSwim(entity);
		else if (entity.level().getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock)
			return coloredBlock.canDamage() && ColorUtils.getInkColor(entity.level(), pos).isValid() && !canSquidSwim(entity);
		else return false;
	}
	public static Direction canSquidClimb(LivingEntity entity, float strafeImpulse, float movementForward, float yaw)
	{
		if (onEnemyInk(entity))
			return null;

		Vec3 inputVector = EntityAccessor.invokeGetInputVector(new Vec3(Math.signum(strafeImpulse), 0, Math.signum(movementForward)), 0.1f, yaw);
		BlockCollisions<BlockPos> collisions = new BlockCollisions<>(entity.level(), entity, entity.getBoundingBox().inflate(inputVector.x, inputVector.y, inputVector.z), false, (bro, what) ->
			bro);

		return checkSquidCollisions(entity, collisions, inputVector);
	}
	@Nullable
	private static Direction checkSquidCollisions(LivingEntity entity, BlockCollisions<BlockPos> collisions, Vec3 inputVector)
	{
		while (collisions.hasNext())
		{
			BlockPos collidedBlock = collisions.next();
			Vec3 center = collidedBlock.getCenter();
			Direction direction;
			if (Math.abs(center.x - entity.getX()) > Math.abs(center.z - entity.getZ()))
			{
				direction = center.x > entity.getX() ? Direction.WEST : Direction.EAST;
			}
			else
			{
				direction = center.z > entity.getZ() ? Direction.NORTH : Direction.SOUTH;
			}

			if (isInked(entity.level(), collidedBlock, direction) &&
				ColorUtils.colorEquals(entity.level(), collidedBlock,
					ColorUtils.getEntityColor(entity),
					getInkBlock(entity.level(), collidedBlock).color(direction.get3DDataValue())))
			{
				if (inputVector == null || Vec3.atBottomCenterOf(new Vec3i(direction.getStepX(), direction.getStepY(), direction.getStepZ())).cross(inputVector).y() != 0)
					return direction;
			}
		}
		return null;
	}
	public static Direction getSquidClimbingDirection(LivingEntity entity, float strafeImpulse, float movementForward, Direction face)
	{
		Direction blockFaceToCheck = face.getOpposite();
		AABB baseBoundingBox = SplatcraftEntities.INK_SQUID.value().getDimensions().makeBoundingBox(entity.position());
		Vec3 inputVector = EntityAccessor.invokeGetInputVector(new Vec3(-Math.signum(strafeImpulse), Math.signum(movementForward), 0), 0.1f, face.toYRot());
		BlockCollisions<BlockPos> collisions = new BlockCollisions<>(entity.level(), entity, baseBoundingBox.inflate(inputVector.x, inputVector.y, inputVector.z), false, (bro, what) ->
			bro);

		Direction otherWallClosion = checkSquidCollisions(entity, collisions, inputVector);
		if (otherWallClosion != null)
			return otherWallClosion;

		inputVector = Vec3.atBottomCenterOf(new Vec3i(blockFaceToCheck.getStepX(), blockFaceToCheck.getStepY(), blockFaceToCheck.getStepZ())).scale(0.01);
		AABB aabb = baseBoundingBox.inflate(inputVector.x, inputVector.y, inputVector.z);
		collisions = new BlockCollisions<>(entity.level(), entity, aabb, false, (bro, what) ->
			bro);

		return checkSquidCollisions(entity, collisions, null);
	}
	public static InkBlockUtils.InkType getInkType(LivingEntity entity)
	{
		return EntityInfoCapability.getOptional(entity).map(EntityInfo::getInkType).orElse(InkType.NORMAL);
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