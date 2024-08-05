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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.blocks.IColoredBlock;
import net.splatcraft.forge.blocks.InkedBlock;
import net.splatcraft.forge.commands.SuperJumpCommand;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.data.capabilities.worldink.WorldInk;
import net.splatcraft.forge.data.capabilities.worldink.WorldInkCapability;
import net.splatcraft.forge.entities.SpawnShieldEntity;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.s2c.UpdateInkPacket;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftStats;
import net.splatcraft.forge.tileentities.InkColorTileEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class InkBlockUtils
{
	public static BlockInkedResult playerInkBlock(@Nullable Player player, Level level, BlockPos pos, int color, float damage, InkType inkType)
	{
		BlockInkedResult inked = inkBlock(level, pos, color, damage, inkType);
		
		if (player != null && inked == BlockInkedResult.SUCCESS)
		{
			player.awardStat(SplatcraftStats.BLOCKS_INKED);
		}
		
		return inked;
	}
	public static boolean clearInk(Level level, BlockPos pos, boolean removePermanent)
	{
		WorldInk worldInk = WorldInkCapability.get(level, pos);
		if (worldInk.isInked(pos))
		{
			boolean removed = worldInk.clearInk(pos);
			if ((removePermanent && worldInk.removePermanentInk(pos)))
				removed = true;
			
			if (removed)
			{
				level.getChunkAt(pos).setUnsaved(true);
				if (!level.isClientSide)
					SplatcraftPacketHandler.sendToDim(new UpdateInkPacket(pos, -1, null), level.dimension());
				return true;
			}
		}
		return false;
	}
	public static BlockInkedResult inkBlock(Level level, BlockPos pos, int color, float damage, InkType inkType)
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
		
		WorldInk worldInk = WorldInkCapability.get(level, pos);
		
		boolean sameColor = worldInk.isInked(pos) && worldInk.getInk(pos).color() == color;
		
		if (sameColor && worldInk.getInk(pos).type() == inkType)
			return BlockInkedResult.ALREADY_INKED;
		
		worldInk.ink(pos, color, inkType);
		level.getChunkAt(pos).setUnsaved(true);
		
		if (SplatcraftGameRules.getLocalizedRule(level, pos.above(), SplatcraftGameRules.INK_DESTROYS_FOLIAGE) &&
			isBlockFoliage(level.getBlockState(pos.above())))
			level.destroyBlock(pos.above(), true);
		
		if (!level.isClientSide)
			SplatcraftPacketHandler.sendToDim(new UpdateInkPacket(pos, color, inkType), level.dimension());
		
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
	public static WorldInk.Entry getInk(Level level, BlockPos pos)
	{
		return WorldInkCapability.get(level, pos).getInk(pos);
	}
	public static boolean isInked(Level level, BlockPos pos)
	{
		return WorldInkCapability.get(level, pos).isInked(pos);
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
		if (!(entity instanceof Player player) || !PlayerCooldown.hasPlayerCooldown(player) || !(PlayerCooldown.getPlayerCooldown(player) instanceof SuperJumpCommand.SuperJump))
			return !entity.isSpectator() && (canSquidSwim(entity) || canSquidClimb(entity));
		return false;
	}
	public static boolean canSquidSwim(LivingEntity entity)
	{
		boolean canSwim = false;
		
		BlockPos down = getBlockStandingOnPos(entity, 0.1);
		Block standingBlock = entity.level().getBlockState(down).getBlock();
		
		if (isInked(entity.level(), down))
			return ColorUtils.colorEquals(entity.level(), down, ColorUtils.getEntityColor(entity), getInk(entity.level(), down).color());
		
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
		
		if (isInked(entity.level(), pos))
			return !canSquidSwim(entity);
		else if (entity.level().getBlockState(pos).getBlock() instanceof IColoredBlock coloredBlock)
			return coloredBlock.canDamage() && ColorUtils.getInkColor(entity.level(), pos) != -1 && !canSquidSwim(entity);
		else return false;
	}
	public static boolean canSquidClimb(LivingEntity entity)
	{
		if (onEnemyInk(entity))
			return false;
		for (int i = 0; i < 4; i++)
		{
			float xOff = (i < 2 ? .32f : 0) * (i % 2 == 0 ? 1 : -1), zOff = (i < 2 ? 0 : .32f) * (i % 2 == 0 ? 1 : -1);
			BlockPos pos = CommonUtils.createBlockPos(entity.getX() - xOff, entity.getY(), entity.getZ() - zOff);
			Block block = entity.level().getBlockState(pos).getBlock();
			VoxelShape shape = entity.level().getBlockState(pos).getCollisionShape(entity.level(), pos, CollisionContext.of(entity));
			
			if (pos.equals(getBlockStandingOnPos(entity)) || (!shape.isEmpty() && (shape.bounds().maxY < (entity.getY() - entity.blockPosition().getY()) || shape.bounds().minY > (entity.getY() - entity.blockPosition().getY()))))
				continue;
			
			if (isInked(entity.level(), pos) && ColorUtils.colorEquals(entity.level(), pos, ColorUtils.getEntityColor(entity), getInk(entity.level(), pos).color()))
				return true;
			
			if ((!(block instanceof IColoredBlock) || ((IColoredBlock) block).canClimb()) && entity.level().getBlockEntity(pos) instanceof InkColorTileEntity && ColorUtils.colorEquals(entity, entity.level().getBlockEntity(pos)) && !entity.isPassenger())
				return true;
		}
		return false;
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
		public static final InkType NORMAL = new InkType(new ResourceLocation(Splatcraft.MODID, "normal"), SplatcraftBlocks.inkedBlock.get());
		public static final InkType GLOWING = new InkType(new ResourceLocation(Splatcraft.MODID, "glowing"), SplatcraftItems.splatfestBand.get(), SplatcraftBlocks.glowingInkedBlock.get());
		public static final InkType CLEAR = new InkType(new ResourceLocation(Splatcraft.MODID, "clear"), SplatcraftItems.clearBand.get(), SplatcraftBlocks.clearInkedBlock.get());
		private final ResourceLocation name;
		private final Item repItem;
		private final InkedBlock block;
		public InkType(ResourceLocation name, Item repItem, InkedBlock inkedBlock)
		{
			values.put(name, this);
			this.name = name;
			this.repItem = repItem;
			this.block = inkedBlock;
		}
		public InkType(ResourceLocation name, InkedBlock inkedBlock)
		{
			this(name, Items.AIR, inkedBlock);
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
	}
}
