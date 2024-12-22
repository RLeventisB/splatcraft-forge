package net.splatcraft.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.World;
import net.splatcraft.Splatcraft;
import net.splatcraft.blocks.IColoredBlock;
import net.splatcraft.blocks.InkedBlock;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.SplatcraftTags;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.entities.SpawnShieldEntity;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.mixin.accessors.EntityAccessor;
import net.splatcraft.registries.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InkBlockUtils
{
	public static BlockInkedResult playerInkBlock(@Nullable PlayerEntity player, World world, BlockPos pos, InkColor color, Direction direction, InkType inkType, float damage)
	{
		BlockInkedResult inked = inkBlock(world, pos, color, direction, inkType, damage);
		
		if (player != null && inked == BlockInkedResult.SUCCESS)
		{
			player.incrementStat(SplatcraftStats.BLOCKS_INKED);
		}
		
		return inked;
	}
	public static Direction getRandomInkedFace(World world, BlockPos pos)
	{
		ChunkInk chunkInk = ChunkInkCapability.get(world, pos);
		ChunkInk.BlockEntry entry = chunkInk.getInk(RelativeBlockPos.fromAbsolute(pos));
		if (entry != null && entry.isInkedAny())
		{
			return Direction.byId(Util.getRandom(entry.getActiveIndices(), world.random));
		}
		return null;
	}
	public static boolean clearInk(World world, BlockPos pos, Direction direction, boolean removePermanent)
	{
		return clearInk(world, pos, direction.getId(), removePermanent);
	}
	public static boolean clearInk(World world, BlockPos pos, int index, boolean removePermanent)
	{
		ChunkInk worldInk = ChunkInkCapability.get(world, pos);
		RelativeBlockPos offset = RelativeBlockPos.fromAbsolute(pos);
		
		if (worldInk.isInkedAny(offset))
		{
			if (worldInk.clearInk(offset, index, removePermanent))
			{
				if (!world.isClient())
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
	public static boolean clearBlock(World world, BlockPos pos, boolean removePermanent)
	{
		ChunkInk worldInk = ChunkInkCapability.get(world, pos);
		RelativeBlockPos offset = RelativeBlockPos.fromAbsolute(pos);
		ChunkInk.BlockEntry entry = worldInk.getInk(offset);
		if (entry != null)
		{
			if (worldInk.clearBlock(offset, removePermanent))
			{
				if (!world.isClient())
					ChunkInkHandler.addInkToRemove(world, pos);
				return true;
			}
		}
		return false;
	}
	public static BlockInkedResult inkBlock(World world, BlockPos pos, InkColor color, Direction direction, InkType inkType, float damage)
	{
		return inkBlock(world, pos, color, direction.getId(), inkType, damage);
	}
	public static BlockInkedResult inkBlock(World world, BlockPos pos, InkColor color, int index, InkType inkType, float damage)
	{
		if (isUninkable(world, pos, Direction.byId(index)))
			return BlockInkedResult.FAIL;
		
		for (SpawnShieldEntity shieldEntity : world.getEntitiesByClass(SpawnShieldEntity.class, new Box(pos), (no) -> true))
			if (!ColorUtils.colorEquals(world, pos, ColorUtils.getEntityColor(shieldEntity), color))
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
		
		ChunkInk worldInk = ChunkInkCapability.getOrCreate(world, pos);
		RelativeBlockPos offset = RelativeBlockPos.fromAbsolute(pos);
		ChunkInk.BlockEntry entry = worldInk.getInk(offset);
		
		boolean isInked = entry != null && entry.isInked(index);
		if (entry != null && entry.inmutable)
			return BlockInkedResult.IS_PERMANENT;
		
		boolean sameColor = isInked && entry.color(index) == color;
		
		if (sameColor && entry.type(index) == inkType)
			return BlockInkedResult.ALREADY_INKED;
		
		worldInk.ink(offset, index, color, inkType);
		
		if (SplatcraftGameRules.getLocalizedRule(world, pos.up(), SplatcraftGameRules.INK_DESTROYS_FOLIAGE) &&
			isBlockFoliage(world.getBlockState(pos.up())))
			world.breakBlock(pos.up(), true);
		
		if (!world.isClient())
			ChunkInkHandler.addInkToUpdate(world, pos);
		
		return sameColor ? BlockInkedResult.ALREADY_INKED : BlockInkedResult.SUCCESS;
	}
	public static void forEachInkedBlockInBounds(World world, final Box bounds, InkedBlockConsumer action)
	{
		int chunkMinX = (int) Math.min(bounds.minX, bounds.maxX) >> 4;
		int chunkMinY = (int) Math.min(bounds.minY, bounds.maxY) >> 4;
		int chunkMinZ = (int) Math.min(bounds.minZ, bounds.maxZ) >> 4;
		int chunkmaxX = (int) Math.max(bounds.minX, bounds.maxX) >> 4;
		int chunkmaxY = (int) Math.max(bounds.minY, bounds.maxY) >> 4;
		int chunkmaxZ = (int) Math.max(bounds.minZ, bounds.maxZ) >> 4;
		for (int x = chunkMinX; x <= chunkmaxX; x++)
//            for (int y = chunkMinY; y <= chunkmaxY; y++)
			for (int z = chunkMinZ; z <= chunkmaxZ; z++)
			{
				ChunkPos chunkPos = new ChunkPos(x, z);
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
		return state.isIn(BlockTags.CROPS) || state.isIn(BlockTags.SAPLINGS) || state.isIn(BlockTags.REPLACEABLE);
	}
	public static BlockState getInkState(InkType inkType)
	{
		return (inkType == null ? InkType.NORMAL : inkType).block.getDefaultState();
	}
	public static @Nullable ChunkInk.BlockEntry getInkBlock(World world, BlockPos pos)
	{
		return ChunkInkCapability.getOrCreate(world, pos).getInk(RelativeBlockPos.fromAbsolute(pos));
	}
	public static ChunkInk.InkEntry getInkInFace(World world, BlockPos pos, Direction direction)
	{
		return getInkBlock(world, pos).get(direction.getId());
	}
	public static boolean isInked(World world, BlockPos pos, Direction direction)
	{
		return isInked(world, pos, direction.getId());
	}
	public static boolean isInked(World world, BlockPos pos, int index)
	{
		return ChunkInkCapability.getOrCreate(world, pos).isInked(RelativeBlockPos.fromAbsolute(pos), index);
	}
	public static boolean isInkedAny(World world, BlockPos pos)
	{
		return ChunkInkCapability.getOrCreate(world, pos).isInkedAny(RelativeBlockPos.fromAbsolute(pos));
	}
	public static boolean canInkFromFace(World world, BlockPos pos, Direction face)
	{
		if (!(world.getBlockState(pos).getBlock() instanceof IColoredBlock) && isUninkable(world, pos, face))
			return false;
		
		return canInkPassthrough(world, pos.offset(face)) || !world.getBlockState(pos.offset(face)).isIn(SplatcraftTags.Blocks.BLOCKS_INK);
	}
	public static boolean isUninkable(World world, BlockPos pos, Direction direction)
	{
		return isUninkable(world, pos, direction, false);
	}
	public static boolean isUninkable(World world, BlockPos pos, Direction direction, boolean checkGamemode)
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
		BlockState occludingBlockState = world.getBlockState(pos.offset(direction));
		VoxelShape blockCollision = blockState.getCollisionShape(world, pos).getFace(direction);
		VoxelShape occludingCollision = occludingBlockState.getCollisionShape(world, pos.offset(direction)).getFace(direction.getOpposite());
		return blockState.isOpaque() && !VoxelShapes.matchesAnywhere(blockCollision, occludingCollision, BooleanBiFunction.NOT_SAME);
	}
	public static boolean isBlockUninkable(World world, BlockPos pos)
	{
		BlockState state = world.getBlockState(pos);
		
		if (state.isIn(SplatcraftTags.Blocks.UNINKABLE_BLOCKS))
			return true;
		
		if (!state.isIn(SplatcraftTags.Blocks.RENDER_AS_CUBE) && state.getRenderType() != BlockRenderType.MODEL)
			return true;
		
		return canInkPassthrough(world, pos);
	}
	public static boolean canInkPassthrough(World world, BlockPos pos)
	{
		BlockState state = world.getBlockState(pos);
		
		return state.getCollisionShape(world, pos).isEmpty() || world.getBlockState(pos).isIn(SplatcraftTags.Blocks.INK_PASSTHROUGH);
	}
	public static boolean canSquidHide(LivingEntity entity)
	{
		if (entity instanceof PlayerEntity player && PlayerCooldown.hasPlayerCooldown(player) && PlayerCooldown.getPlayerCooldown(player) instanceof SuperJumpCommand.SuperJump)
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
		
		BlockPos down = entity.getSteppingPos();
		Block standingBlock = entity.getWorld().getBlockState(down).getBlock();
		
		if (isInked(entity.getWorld(), down, Direction.UP))
			return ColorUtils.colorEquals(entity.getWorld(), down, ColorUtils.getEntityColor(entity), getInkBlock(entity.getWorld(), down).color(Direction.UP.getId()));
		
		if (standingBlock instanceof IColoredBlock coloredBlock)
			canSwim = coloredBlock.canSwim();
		
		return canSwim && ColorUtils.colorEquals(entity, entity.getWorld().getBlockEntity(down));
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
			
			VoxelShape shape = entity.getWorld().getBlockState(result).getCollisionShape(entity.getWorld(), result, ShapeContext.of(entity));
			shape.calculateMaxDistance(Direction.Axis.Y, entity.getBoundingBox(), 0.0);
			
			if (!shape.isEmpty() && shape.getBoundingBox().minY <= entity.getY() - result.getY())
				return result;
		}
		return CommonUtils.createBlockPos(entity.getX(), entity.getY() - maxDepth, entity.getZ());
	}
	public static boolean onEnemyInk(LivingEntity entity)
	{
		if (!entity.isOnGround())
			return false;
		BlockPos pos = entity.getSteppingPos();
		
		if (isInked(entity.getWorld(), pos, Direction.UP))
			return !canSquidSwim(entity);
		else if (entity.getWorld().getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock)
			return coloredBlock.canDamage() && ColorUtils.getInkColor(entity.getWorld(), pos).isValid() && !canSquidSwim(entity);
		else return false;
	}
	public static Direction canSquidClimb(LivingEntity entity, float strafeImpulse, float movementForward, float yaw)
	{
		if (onEnemyInk(entity))
			return null;
		
		Vec3d inputVector = EntityAccessor.invokeMovementInputToVelocity(new Vec3d(Math.signum(strafeImpulse), 0, Math.signum(movementForward)), 0.1f, yaw);
		BlockCollisionSpliterator<BlockPos> collisions = new BlockCollisionSpliterator<>(entity.getWorld(), entity, entity.getBoundingBox().expand(inputVector.x, inputVector.y, inputVector.z), false, (bro, what) ->
			bro);
		
		return checkSquidCollisions(entity, collisions, inputVector);
	}
	@Nullable
	private static Direction checkSquidCollisions(LivingEntity entity, BlockCollisionSpliterator<BlockPos> collisions, Vec3d inputVector)
	{
		while (collisions.hasNext())
		{
			BlockPos collidedBlock = collisions.next();
			Vec3d center = collidedBlock.toCenterPos();
			Direction direction;
			if (Math.abs(center.x - entity.getX()) > Math.abs(center.z - entity.getZ()))
			{
				direction = center.x > entity.getX() ? Direction.WEST : Direction.EAST;
			}
			else
			{
				direction = center.z > entity.getZ() ? Direction.NORTH : Direction.SOUTH;
			}
			
			if (isInked(entity.getWorld(), collidedBlock, direction) &&
				ColorUtils.colorEquals(entity.getWorld(), collidedBlock,
					ColorUtils.getEntityColor(entity),
					getInkBlock(entity.getWorld(), collidedBlock).color(direction.getId())))
			{
				if (inputVector == null || Vec3d.ofBottomCenter(new Vec3i(direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ())).crossProduct(inputVector).getY() != 0)
					return direction;
			}
		}
		return null;
	}
	public static Direction getSquidClimbingDirection(LivingEntity entity, float strafeImpulse, float movementForward, Direction face)
	{
		Direction blockFaceToCheck = face.getOpposite();
		Box baseBoundingBox = SplatcraftEntities.INK_SQUID.get().getDimensions().getBoxAt(entity.getPos());
		Vec3d inputVector = EntityAccessor.invokeMovementInputToVelocity(new Vec3d(-Math.signum(strafeImpulse), Math.signum(movementForward), 0), 0.1f, face.asRotation());
		BlockCollisionSpliterator<BlockPos> collisions = new BlockCollisionSpliterator<>(entity.getWorld(), entity, baseBoundingBox.expand(inputVector.x, inputVector.y, inputVector.z), false, (bro, what) ->
			bro);
		
		Direction otherWallClosion = checkSquidCollisions(entity, collisions, inputVector);
		if (otherWallClosion != null)
			return otherWallClosion;
		
		inputVector = Vec3d.ofBottomCenter(new Vec3i(blockFaceToCheck.getOffsetX(), blockFaceToCheck.getOffsetY(), blockFaceToCheck.getOffsetZ())).multiply(0.01);
		Box aabb = baseBoundingBox.expand(inputVector.x, inputVector.y, inputVector.z);
		collisions = new BlockCollisionSpliterator<>(entity.getWorld(), entity, aabb, false, (bro, what) ->
			bro);
		
		return checkSquidCollisions(entity, collisions, null);
	}
	public static InkBlockUtils.InkType getInkType(LivingEntity entity)
	{
		return EntityInfoCapability.hasCapability(entity) ? EntityInfoCapability.get(entity).getInkType() : InkType.NORMAL;
	}
	public static InkType getInkTypeFromStack(ItemStack stack)
	{
		if (!stack.isEmpty())
			for (InkType t : InkType.values.values())
				if (t.getRepItem().equals(stack.getItem()))
					return t;
		
		return InkType.NORMAL;
	}
	public static boolean hasInkType(ItemStack stack)
	{
		if (!stack.isEmpty())
			for (InkType t : InkType.values.values())
				if (t.getRepItem().equals(stack.getItem()))
					return true;
		return false;
	}
	public interface InkedBlockConsumer
	{
		void accept(BlockPos pos, ChunkInk.BlockEntry ink);
	}
	public static class InkType implements Comparable<InkType>
	{
		public static final HashMap<Identifier, InkType> values = new HashMap<>();
		public static final InkType NORMAL = new InkType(0, Splatcraft.identifierOf("normal"), SplatcraftBlocks.inkedBlock.get());
		public static final InkType GLOWING = new InkType(1, Splatcraft.identifierOf("glowing"), SplatcraftItems.splatfestBand.get(), SplatcraftBlocks.glowingInkedBlock.get());
		public static final InkType CLEAR = new InkType(2, Splatcraft.identifierOf("clear"), SplatcraftItems.clearBand.get(), SplatcraftBlocks.clearInkedBlock.get());
		private final Identifier name;
		private final Item repItem;
		private final InkedBlock block;
		private final byte id;
		public InkType(int id, Identifier name, Item repItem, InkedBlock inkedBlock)
		{
			values.put(name, this);
			this.id = (byte) id;
			this.name = name;
			this.repItem = repItem;
			block = inkedBlock;
		}
		public InkType(int id, Identifier name, InkedBlock inkedBlock)
		{
			this(id, name, Items.AIR, inkedBlock);
		}
		public static InkType fromId(int id)
		{
			return switch (id)
			{
				case 0 -> NORMAL;
				case 1 -> GLOWING;
				case 2 -> CLEAR;
				default -> throw new IllegalStateException("Unexpected value: " + id);
			};
		}
		@Override
		public int compareTo(InkType o)
		{
			return getName().compareTo(o.getName());
		}
		public Identifier getName()
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
		public String getSerializedName()
		{
			return getName().toString();
		}
		public byte getId()
		{
			return id;
		}
	}
}