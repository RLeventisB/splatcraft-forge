package net.splatcraft.forge.util;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.forge.blocks.ColoredBarrierBlock;
import net.splatcraft.forge.registries.SplatcraftBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class InkExplosion
{
	private final double x;
	private final double y;
	private final double z;
	private final AttackId attackId;
	@Nullable
	private final Entity exploder;
	private final float paintRadius;
	private final List<BlockFace> affectedBlockPositions = Lists.newArrayList();
	private final Vec3 position;
	private final InkBlockUtils.InkType inkType;
	private final float minDamage;
	private final float maxDamage;
	private final float damageRadius;
	private final ItemStack weapon;
	public InkExplosion(@Nullable Entity source, double x, double y, double z, float damageRadius, float minDamage, float maxDamage, float paintRadius, InkBlockUtils.InkType inkType, ItemStack weapon, AttackId attackId)
	{
		this.exploder = source;
		this.paintRadius = paintRadius;
		this.x = x;
		this.y = y;
		this.z = z;
		this.attackId = attackId;
		this.position = new Vec3(this.x, this.y, this.z);
		
		this.inkType = inkType;
		this.minDamage = minDamage;
		this.maxDamage = maxDamage;
		this.damageRadius = damageRadius;
		this.weapon = weapon;
	}
	public static Vec3 adjustPosition(Vec3 pos, Vec3i normal)
	{
		float modifier = 0.01f;
		return pos.add(normal.getX() * modifier, normal.getY() * modifier, normal.getZ() * modifier);
	}
	public static void createInkExplosion(Entity source, Vec3 pos, float paintRadius, float damageRadius, float damage, InkBlockUtils.InkType type, ItemStack weapon, AttackId attackId)
	{
		createInkExplosion(source, pos, paintRadius, damageRadius, 0, damage, type, weapon, attackId);
	}
	public static void createInkExplosion(Entity source, Vec3 pos, float paintRadius, float damageRadius, float damage, InkBlockUtils.InkType type, ItemStack weapon)
	{
		createInkExplosion(source, pos, paintRadius, damageRadius, 0, damage, type, weapon, AttackId.NONE);
	}
	public static void createInkExplosion(Entity source, Vec3 pos, float paintRadius, float damageRadius, float minDamage, float maxDamage, InkBlockUtils.InkType type, ItemStack weapon)
	{
		createInkExplosion(source, pos, paintRadius, damageRadius, minDamage, maxDamage, type, weapon, AttackId.NONE);
	}
	public static void createInkExplosion(Entity source, Vec3 pos, float paintRadius, float damageRadius, float minDamage, float maxDamage, InkBlockUtils.InkType type, ItemStack weapon, AttackId attackId)
	{
		if (source == null || source.level().isClientSide)
			return;
		
		InkExplosion inksplosion = new InkExplosion(source, pos.x(), pos.y(), pos.z(), damageRadius, minDamage, maxDamage, paintRadius, type, weapon, attackId);
		
		inksplosion.doExplosionA();
		inksplosion.doExplosionCosmetics(false);
	}
	/**
	 * Does the first part of the explosion (destroy blocks)
	 */
	public void doExplosionA()
	{
		List<BlockFace> set = new ArrayList<>();
		ServerLevel level = (ServerLevel) exploder.level();
		Vec3 explosionPos = new Vec3(x, y, z);
		getBlocksInSphereWithNoise(set, level);
		
		this.affectedBlockPositions.addAll(set);
		if (damageRadius == 0 || minDamage <= 0.1 && maxDamage <= 0.1)
			return;
		boolean sameDamage = minDamage == maxDamage;
		float radiusSquared = this.damageRadius * this.damageRadius;
		int k1 = Mth.floor(this.x - (double) this.damageRadius - 1.0D);
		int l1 = Mth.floor(this.x + (double) this.damageRadius + 1.0D);
		int i2 = Mth.floor(this.y - (double) this.damageRadius - 1.0D);
		int i1 = Mth.floor(this.y + (double) this.damageRadius + 1.0D);
		int j2 = Mth.floor(this.z - (double) this.damageRadius - 1.0D);
		int j1 = Mth.floor(this.z + (double) this.damageRadius + 1.0D);
		List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, new AABB(k1, i2, j2, l1, i1, j1));
		
		int color = ColorUtils.getEntityColor(exploder);
		for (LivingEntity entity : list)
		{
			AABB boundingBox = entity.getBoundingBox();
			Vec3 closestPos = new Vec3(Mth.clamp(explosionPos.x, boundingBox.minX, boundingBox.maxX), Mth.clamp(explosionPos.y, boundingBox.minY, boundingBox.maxY), Mth.clamp(explosionPos.z, boundingBox.minZ, boundingBox.maxZ));
			
			float distance = (float) explosionPos.distanceToSqr(closestPos);
			if (distance > radiusSquared) // still collides even in the center isn't in radius
				continue;
			int targetColor = ColorUtils.getEntityColor(entity);
			if (targetColor == -1 || (color != targetColor && targetColor > -1))
			{
				float seenPercent = Explosion.getSeenPercent(explosionPos, entity);
				if (sameDamage)
				{
					InkDamageUtils.doSplatDamage(entity, maxDamage * seenPercent, exploder, weapon, attackId);
				}
				else
				{
					float pctg = Mth.sqrt(distance) / damageRadius;
					
					InkDamageUtils.doSplatDamage(entity, Mth.lerp(pctg, maxDamage, minDamage) * seenPercent, exploder, weapon, attackId);
				}
			}
			
			DyeColor dyeColor = null;
			
			if (InkColor.getByHex(color) != null)
				dyeColor = InkColor.getByHex(color).getDyeColor();
			
			if (dyeColor != null && entity instanceof Sheep sheep)
			{
				sheep.setColor(dyeColor);
			}
		}
	}
	private void getBlocksInSphereWithNoise(List<BlockFace> set, ServerLevel level)
	{
		final float noiseRange = 0.2f;
		int cubeSizeHalf = ((int) Math.ceil(paintRadius + noiseRange) >> 1) + 1;
//		CommonUtils.spawnTestText(level, position, position.toString());
		
		for (int x = -cubeSizeHalf; x <= cubeSizeHalf; x++)
			for (int y = -cubeSizeHalf; y <= cubeSizeHalf; y++)
				for (int z = -cubeSizeHalf; z <= cubeSizeHalf; z++)
				{
					BlockPos pos = CommonUtils.createBlockPos(position.x() + x, position.y() + y, position.z() + z);
					if (InkBlockUtils.isBlockUninkable(level, pos))
						continue;
					double blockCenterX = pos.getX() + 0.5;
					double blockCenterY = pos.getY() + 0.5;
					double blockCenterZ = pos.getZ() + 0.5;
					
					double dX = blockCenterX - position.x();
					double dY = blockCenterY - position.y();
					double dZ = blockCenterZ - position.z();
					double dist = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
					if (dist <= paintRadius + noiseRange + 0.5f * Math.sqrt(2))
					{
						List<Tuple<Vec3, Direction>> points = new ArrayList<>(6);
						
						if (position.y > blockCenterY)
							points.add(new Tuple<>(new Vec3(blockCenterX, blockCenterY + 0.5, blockCenterZ), Direction.UP));
						if (position.y < blockCenterY)
							points.add(new Tuple<>(new Vec3(blockCenterX, blockCenterY - 0.5, blockCenterZ), Direction.DOWN));
						if (position.x > blockCenterX)
							points.add(new Tuple<>(new Vec3(blockCenterX + 0.5, blockCenterY, blockCenterZ), Direction.EAST));
						if (position.x < blockCenterX)
							points.add(new Tuple<>(new Vec3(blockCenterX - 0.5, blockCenterY, blockCenterZ), Direction.WEST));
						if (position.z > blockCenterZ)
							points.add(new Tuple<>(new Vec3(blockCenterX, blockCenterY, blockCenterZ + 0.5), Direction.SOUTH));
						if (position.z < blockCenterZ)
							points.add(new Tuple<>(new Vec3(blockCenterX, blockCenterY, blockCenterZ - 0.5), Direction.NORTH));
						points.sort((tuple1, tuple2) -> Double.compare(tuple1.getA().distanceToSqr(position), tuple2.getA().distanceToSqr(position)));
						
						for (Tuple<Vec3, Direction> point : points)
						{
							if (raycastAndGetDirection(position, point.getA(), level, pos, point.getB()) && InkBlockUtils.canInkFromFace(level, pos, point.getB()))
							{
								dX = point.getA().x - position.x;
								dY = point.getA().y - position.y;
								dZ = point.getA().z - position.z;
								dist = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
//								CommonUtils.spawnTestText(level, point.getA(), Double.toString(dist));
								
								boolean inRadius = dist <= paintRadius - noiseRange;
								if (!inRadius && dist <= paintRadius + noiseRange)
								{
									double noiseProgress = (dist - (paintRadius - noiseRange)) / (noiseRange * 2);
									inRadius = (noiseProgress * noiseProgress) <= level.getRandom().nextFloat();
								}
								if (inRadius)
								{
									set.add(new BlockFace(pos, point.getB()));
								}
							}
						}
					}
				}
	}
	private boolean raycastAndGetDirection(Vec3 startPos, Vec3 endPos, ServerLevel level, BlockPos expectedPos, Direction expectedFace)
	{
		ClipContext clipContext = new ClipContext(new Vec3(x, y, z), endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null); // actually don't know if it's faster to inline a method but ok
		int ownerColor = ColorUtils.getEntityColor(exploder);
		BlockHitResult hitResult = BlockGetter.traverseBlocks(startPos, endPos, clipContext, (clipContext1, blockPos1) ->
		{
			BlockState blockstate = level.getBlockState(blockPos1);
			VoxelShape voxelshape = blockstate.getCollisionShape(level, blockPos1);
			
			if (blockstate.is(SplatcraftBlocks.allowedColorBarrier.get()))
			{
				ColoredBarrierBlock block = (ColoredBarrierBlock) blockstate.getBlock();
				if (ColorUtils.colorEquals(level, blockPos1, ownerColor, block.getColor(level, blockPos1)))
				{
					return null;
				}
			}
			if (blockstate.is(SplatcraftBlocks.deniedColorBarrier.get()))
			{
				ColoredBarrierBlock block = (ColoredBarrierBlock) blockstate.getBlock();
				if (!ColorUtils.colorEquals(level, blockPos1, ownerColor, block.getColor(level, blockPos1)))
				{
					return null;
				}
			}
			return level.clipWithInteractionOverride(clipContext1.getFrom(), clipContext1.getTo(), blockPos1, voxelshape, blockstate);
		}, (clipContext1) ->
		{
			return BlockHitResult.miss(endPos, expectedFace, expectedPos);
//			return BlockHitResult.miss(startPos, Direction.UP, BlockPos.ZERO);
		});
		return (hitResult.getBlockPos().equals(expectedPos) && hitResult.getDirection().equals(expectedFace)) || hitResult.getType() == HitResult.Type.MISS;
	}
	/**
	 * Does the second part of the explosion (sound, particles, drop spawn)
	 */
	public void doExplosionCosmetics(boolean spawnParticles)
	{
		Vec3 explosionPos = new Vec3(x + 0.5f, y + 0.5f, z + 0.5f);
		
		Level level = exploder.level();
		
		if (spawnParticles)
		{
			if (this.paintRadius < 2.0F)
			{
				level.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
			}
			else
			{
				level.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
			}
		}
		
		for (BlockFace blockFace : this.affectedBlockPositions)
		{
			BlockState blockstate = level.getBlockState(blockFace.pos());
			if (!blockstate.isAir())
			{
				int color = ColorUtils.getEntityColor(exploder);
				float percentage = (float) Math.sqrt(blockFace.pos().distToCenterSqr(explosionPos.x, explosionPos.y, explosionPos.z)) / damageRadius;
				if (exploder instanceof Player player)
				{
					InkBlockUtils.playerInkBlock(player, level, blockFace.pos(), color, blockFace.face(), inkType, Mth.lerp(percentage, minDamage, maxDamage));
				}
				else
				{
					InkBlockUtils.inkBlock(level, blockFace.pos(), color, blockFace.face(), inkType, Mth.lerp(percentage, minDamage, maxDamage));
				}
			}
		}
	}
	public Vec3 getPosition()
	{
		return this.position;
	}
}
