package net.splatcraft.forge.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.blocks.IColoredBlock;
import net.splatcraft.forge.blocks.InkedBlock;
import net.splatcraft.forge.commands.SuperJumpCommand;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInk;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.forge.entities.SpawnShieldEntity;
import net.splatcraft.forge.handlers.ChunkInkHandler;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftStats;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InkBlockUtils
{
	public static BlockInkedResult playerInkBlock(@Nullable Player player, Level level, BlockPos pos, int color, Direction direction, InkType inkType, float damage)
	{
		BlockInkedResult inked = inkBlock(level, pos, color, direction, inkType, damage);
		
		if (player != null && inked == BlockInkedResult.SUCCESS)
		{
			player.awardStat(SplatcraftStats.BLOCKS_INKED);
		}
		
		return inked;
	}
	public static Direction getRandomInkedFace(Level level, BlockPos pos)
	{
		ChunkInk worldInk = ChunkInkCapability.get(level, pos);
		if (worldInk.isInkedAny(pos))
		{
			List<Integer> indices = new ArrayList<>(6);
			ChunkInk.InkEntry[] entries = worldInk.getInk(pos).entries;
			for (int i = 0; i < entries.length; i++)
			{
				ChunkInk.InkEntry entry = entries[i];
				if (entry != null && entry.isInked())
				{
					indices.add(i);
				}
			}
			return Direction.from3DDataValue(CommonUtils.selectRandom(level.random, indices));
		}
		return null;
	}
	public static boolean clearInk(Level level, BlockPos pos, Direction direction, boolean removePermanent)
	{
		return clearInk(level, pos, direction.get3DDataValue(), removePermanent);
	}
	public static boolean clearInk(Level level, BlockPos pos, int index, boolean removePermanent)
	{
		ChunkInk worldInk = ChunkInkCapability.get(level, pos);
		if (worldInk.isInkedAny(pos))
		{
			if (worldInk.clearInk(pos, index, removePermanent))
			{
				if (!level.isClientSide)
				{
					if (worldInk.isInkedAny(pos))
						ChunkInkHandler.addInkToUpdate(level, pos);
					else
						ChunkInkHandler.addInkToRemove(level, pos);
				}
				return true;
			}
		}
		return false;
	}
	public static boolean clearBlock(Level level, BlockPos pos, boolean removePermanent)
	{
		ChunkInk worldInk = ChunkInkCapability.get(level, pos);
		if (worldInk.isInkedAny(pos))
		{
			if (worldInk.clearBlock(pos, removePermanent))
			{
				level.getChunkAt(pos).setUnsaved(true);
				if (!level.isClientSide)
					ChunkInkHandler.addInkToRemove(level, pos);
				return true;
			}
		}
		return false;
	}
	public static BlockInkedResult inkBlock(Level level, BlockPos pos, int color, Direction direction, InkType inkType, float damage)
	{
		if (isUninkable(level, pos))
			return BlockInkedResult.FAIL;
		
		for (SpawnShieldEntity shieldEntity : level.getEntitiesOfClass(SpawnShieldEntity.class, new AABB(pos)))
			if (!ColorUtils.colorEquals(level, pos, ColorUtils.getEntityColor(shieldEntity), color))
				return BlockInkedResult.FAIL;
		
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof IColoredBlock coloredBlock)
		{
			BlockInkedResult result = coloredBlock.inkBlock(level, pos, color, damage, inkType);
			if (result != BlockInkedResult.PASS)
				return result;
		}
		
		if (!SplatcraftGameRules.getLocalizedRule(level, pos, SplatcraftGameRules.INKABLE_GROUND))
			return BlockInkedResult.FAIL;
		
		ChunkInk worldInk = ChunkInkCapability.get(level, pos);
		
		boolean sameColor = worldInk.isInkedAny(pos) && worldInk.getInk(pos).color(direction) == color;
		
		if (sameColor && worldInk.getInk(pos).type(direction) == inkType)
			return BlockInkedResult.ALREADY_INKED;
		
		worldInk.ink(pos, direction, color, inkType);
		level.getChunkAt(pos).setUnsaved(true);
		
		if (SplatcraftGameRules.getLocalizedRule(level, pos.above(), SplatcraftGameRules.INK_DESTROYS_FOLIAGE) &&
			isBlockFoliage(level.getBlockState(pos.above())))
			level.destroyBlock(pos.above(), true);
		
		if (!level.isClientSide)
			ChunkInkHandler.addInkToUpdate(level, pos);
		
		return sameColor ? BlockInkedResult.ALREADY_INKED : BlockInkedResult.SUCCESS;
	}
	public static boolean isBlockFoliage(BlockState state)
	{
		return state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.REPLACEABLE);
	}
	public static BlockState getInkState(InkType inkType)
	{
		return (inkType == null ? InkType.NORMAL : inkType).block.defaultBlockState();
	}
	public static ChunkInk.BlockEntry getInkBlock(Level level, BlockPos pos)
	{
		return ChunkInkCapability.get(level, pos).getInk(pos);
	}
	public static ChunkInk.InkEntry getInkInFace(Level level, BlockPos pos, Direction direction)
	{
		return ChunkInkCapability.get(level, pos).getInk(pos).get(direction);
	}
	public static boolean isInked(Level level, BlockPos pos, Direction direction)
	{
		return isInked(level, pos, direction.get3DDataValue());
	}
	public static boolean isInked(Level level, BlockPos pos, int index)
	{
		return ChunkInkCapability.get(level, pos).isInked(pos, index);
	}
	public static boolean isInkedAny(Level level, BlockPos pos)
	{
		return ChunkInkCapability.get(level, pos).isInkedAny(pos);
	}
	public static boolean canInkFromFace(Level level, BlockPos pos, Direction face)
	{
		if (!(level.getBlockState(pos).getBlock() instanceof IColoredBlock) && isUninkable(level, pos))
			return false;
//		if (Minecraft.getInstance().player != null)
//			Minecraft.getInstance().player.sendMessage(Component.literal(pos + " -> " + face), UUID.randomUUID());
		
		return canInkPassthrough(level, pos.relative(face)) || !level.getBlockState(pos.relative(face)).is(SplatcraftTags.Blocks.BLOCKS_INK);
	}
	public static boolean isUninkable(Level level, BlockPos pos)
	{
		
		if (InkedBlock.isTouchingLiquid(level, pos))
			return true;
		
		BlockState state = level.getBlockState(pos);
		
		if (state.is(SplatcraftTags.Blocks.UNINKABLE_BLOCKS))
			return true;
		
		if (!state.is(SplatcraftTags.Blocks.RENDER_AS_CUBE) && state.getRenderShape() != RenderShape.MODEL)
			return true;
		
		return canInkPassthrough(level, pos);
	}
	public static boolean canInkPassthrough(Level level, BlockPos pos)
	{
		BlockState state = level.getBlockState(pos);
		
		return state.getCollisionShape(level, pos).isEmpty() || level.getBlockState(pos).is(SplatcraftTags.Blocks.INK_PASSTHROUGH);
	}
	public static boolean canSquidHide(LivingEntity entity)
	{
		if (entity instanceof Player player && PlayerCooldown.hasPlayerCooldown(player) && PlayerCooldown.getPlayerCooldown(player) instanceof SuperJumpCommand.SuperJump)
		{
			return false;
		}
		PlayerInfo playerInfo = PlayerInfoCapability.get(entity);
		if (playerInfo == null)
			return false;
		return !entity.isSpectator() && (canSquidSwim(entity) || playerInfo.getClimbedDirection().isPresent());
	}
	public static boolean canSquidSwim(LivingEntity entity)
	{
		boolean canSwim = false;
		
		BlockPos down = entity.getOnPos();
		Block standingBlock = entity.level().getBlockState(down).getBlock();
		
		if (isInked(entity.level(), down, Direction.UP))
			return ColorUtils.colorEquals(entity.level(), down, ColorUtils.getEntityColor(entity), getInkBlock(entity.level(), down).color(Direction.UP));
		
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
		BlockPos pos = getBlockStandingOnPos(entity);
		
		if (isInked(entity.level(), pos, Direction.UP))
			return !canSquidSwim(entity);
		else if (entity.level().getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock)
			return coloredBlock.canDamage() && ColorUtils.getInkColor(entity.level(), pos) != -1 && !canSquidSwim(entity);
		else return false;
	}
	public static Direction canSquidClimb(LivingEntity entity)
	{
		if (onEnemyInk(entity))
			return null;
		
		BlockCollisions<BlockPos> collisions = new BlockCollisions<>(entity.level, entity, entity.getBoundingBox().move(ClientUtils.getInputVector(new Vec3(entity.xxa, 0, entity.zza), entity.getYRot())), false, (bro, what) ->
			bro);
		
		while (collisions.hasNext())
		{
			BlockPos collidedBlock = collisions.next();
			Vec3 center = collidedBlock.getCenter();
			Direction xDirection = center.x > entity.getX() ? Direction.WEST : Direction.EAST;
			if (isInked(entity.level, collidedBlock, xDirection) && ColorUtils.colorEquals(entity.level(), collidedBlock, ColorUtils.getEntityColor(entity), getInkBlock(entity.level(), collidedBlock).color(xDirection)))
				return xDirection;
			Direction zDirection = center.z > entity.getZ() ? Direction.NORTH : Direction.SOUTH;
			if (isInked(entity.level, collidedBlock, zDirection) && ColorUtils.colorEquals(entity.level(), collidedBlock, ColorUtils.getEntityColor(entity), getInkBlock(entity.level(), collidedBlock).color(zDirection)))
				return zDirection;
		}
		return null;
	}
	public static boolean isSquidStillClimbing(LivingEntity entity, Direction face)
	{
		BlockPos collidedBlock = entity.blockPosition().relative(face);
		Direction blockFaceToCheck = face.getOpposite();
		return isInked(entity.level, collidedBlock, blockFaceToCheck) && ColorUtils.colorEquals(entity.level(), collidedBlock, ColorUtils.getEntityColor(entity), getInkBlock(entity.level(), collidedBlock).color(blockFaceToCheck));
	}
	public static InkBlockUtils.InkType getInkType(LivingEntity entity)
	{
		return PlayerInfoCapability.hasCapability(entity) ? PlayerInfoCapability.get(entity).getInkType() : InkType.NORMAL;
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
	public static class InkType implements Comparable<InkType>
	{
		public static final HashMap<ResourceLocation, InkType> values = new HashMap<>();
		public static final InkType NORMAL = new InkType(0, new ResourceLocation(Splatcraft.MODID, "normal"), SplatcraftBlocks.inkedBlock.get());
		public static final InkType GLOWING = new InkType(1, new ResourceLocation(Splatcraft.MODID, "glowing"), SplatcraftItems.splatfestBand.get(), SplatcraftBlocks.glowingInkedBlock.get());
		public static final InkType CLEAR = new InkType(2, new ResourceLocation(Splatcraft.MODID, "clear"), SplatcraftItems.clearBand.get(), SplatcraftBlocks.clearInkedBlock.get());
		private final ResourceLocation name;
		private final Item repItem;
		private final InkedBlock block;
		private final byte id;
		public InkType(int id, ResourceLocation name, Item repItem, InkedBlock inkedBlock)
		{
			values.put(name, this);
			this.id = (byte) id;
			this.name = name;
			this.repItem = repItem;
			this.block = inkedBlock;
		}
		public InkType(int id, ResourceLocation name, InkedBlock inkedBlock)
		{
			this(id, name, Items.AIR, inkedBlock);
		}
		@Override
		public int compareTo(InkType o)
		{
			return getName().compareTo(o.getName());
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
		public String getSerializedName()
		{
			return getName().toString();
		}
		public byte getId()
		{
			return id;
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
	}
}
