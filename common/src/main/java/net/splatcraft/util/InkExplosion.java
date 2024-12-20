package net.splatcraft.util;

import com.google.common.collect.Lists;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.registries.SplatcraftBlocks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

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
    private final Vec3d position;
    private final InkBlockUtils.InkType inkType;
    private final DamageRangesRecord dmgCalculator;
    private final ItemStack weapon;

    public InkExplosion(@Nullable Entity source, double x, double y, double z, DamageRangesRecord damageCalculator, float paintRadius, InkBlockUtils.InkType inkType, ItemStack weapon, AttackId attackId)
    {
        exploder = source;
        this.paintRadius = paintRadius;
        this.x = x;
        this.y = y;
        this.z = z;
        this.attackId = attackId;
        position = new Vec3d(this.x, this.y, this.z);

        this.inkType = inkType;
        dmgCalculator = damageCalculator;
        this.weapon = weapon;
    }

    public static Vec3d adjustPosition(Vec3d pos, Vector3f normal)
    {
        float modifier = 0.01f;
        return pos.add(normal.x() * modifier, normal.y() * modifier, normal.z() * modifier);
    }

    public static void createInkExplosion(Entity source, Vec3d pos, float paintRadius, float damageRadius, float damage, InkBlockUtils.InkType type, ItemStack weapon)
    {
        createInkExplosion(source, pos, paintRadius, DamageRangesRecord.createSimpleLerped(damage, damageRadius), type, weapon, AttackId.NONE);
    }

    public static void createInkExplosion(Entity source, Vec3d pos, float paintRadius, float damageRadius, float closeDamage, float farDamage, InkBlockUtils.InkType type, ItemStack weapon)
    {
        createInkExplosion(source, pos, paintRadius, DamageRangesRecord.createSimpleLerped(closeDamage, farDamage, damageRadius), type, weapon, AttackId.NONE);
    }

    public static void createInkExplosion(Entity source, Vec3d pos, float paintRadius, DamageRangesRecord damageManager, InkBlockUtils.InkType type, ItemStack weapon, AttackId attackId)
    {
        if (source == null || source.getWorld().isClient)
            return;

        InkExplosion inksplosion = new InkExplosion(source, pos.x, pos.y, pos.z, damageManager, paintRadius, type, weapon, attackId);

        inksplosion.doExplosionA();
        inksplosion.doExplosionCosmetics(false);
    }

    /**
     * Does the first part of the explosion (destroy blocks)
     */
    public void doExplosionA()
    {
        List<BlockFace> set = new ArrayList<>();
        ServerWorld world = (ServerWorld) exploder.getWorld();
        Vec3d explosionPos = new Vec3d(x, y, z);
        getBlocksInSphereWithNoise(set, world);

        affectedBlockPositions.addAll(set);
        if (dmgCalculator.isInsignificant())
            return;
        float radiusSquared = dmgCalculator.getMaxDistance() * dmgCalculator.getMaxDistance();
        int k1 = MathHelper.floor(x - dmgCalculator.getMaxDistance() - 1F);
        int l1 = MathHelper.floor(x + dmgCalculator.getMaxDistance() + 1F);
        int i2 = MathHelper.floor(y - dmgCalculator.getMaxDistance() - 1F);
        int i1 = MathHelper.floor(y + dmgCalculator.getMaxDistance() + 1F);
        int j2 = MathHelper.floor(z - dmgCalculator.getMaxDistance() - 1F);
        int j1 = MathHelper.floor(z + dmgCalculator.getMaxDistance() + 1F);
        List<LivingEntity> list = world.getEntitiesByClass(LivingEntity.class, new Box(k1, i2, j2, l1, i1, j1), v -> true);

        InkColor color = ColorUtils.getEntityColor(exploder);
        for (LivingEntity entity : list)
        {
            Box boundingBox = entity.getBoundingBox();
            Vec3d closestPos = new Vec3d(MathHelper.clamp(explosionPos.x, boundingBox.minX, boundingBox.maxX), MathHelper.clamp(explosionPos.y, boundingBox.minY, boundingBox.maxY), MathHelper.clamp(explosionPos.z, boundingBox.minZ, boundingBox.maxZ));

            float distance = (float) explosionPos.squaredDistanceTo(closestPos);
            if (distance > radiusSquared) // still collides even in the center isn't in radius
                continue;
            InkColor targetColor = ColorUtils.getEntityColor(entity);
            if (!targetColor.isValid() || (color != targetColor && targetColor.isValid()))
            {
                float seenPercent = Explosion.getExposure(explosionPos, entity);

                InkDamageUtils.doSplatDamage(entity, dmgCalculator.getDamage(MathHelper.sqrt(distance)) * seenPercent, exploder, weapon, attackId);
            }

            DyeColor dyeColor = color.getDyeColor();

            if (dyeColor != null && entity instanceof SheepEntity sheep)
            {
                sheep.setColor(dyeColor);
            }
        }
    }

    private void getBlocksInSphereWithNoise(List<BlockFace> set, ServerWorld level)
    {
        final float noiseRange = 0.2f;
        int cubeSizeHalf = ((int) Math.ceil(paintRadius + noiseRange) >> 1) + 1;
//		CommonUtils.spawnTestText(level, position, position.toString());

        for (int x = -cubeSizeHalf; x <= cubeSizeHalf; x++)
            for (int y = -cubeSizeHalf; y <= cubeSizeHalf; y++)
                for (int z = -cubeSizeHalf; z <= cubeSizeHalf; z++)
                {
                    BlockPos pos = CommonUtils.createBlockPos(position.x + x, position.y + y, position.z + z);
                    if (InkBlockUtils.isBlockUninkable(level, pos))
                        continue;

                    VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
                    Vec3d relativePos = position.subtract(pos.toCenterPos());
                    Vec3d[] data = closestPointTo(shape, relativePos);
                    if (data.length == 0)
                        continue;

                    double blockCenterX = pos.getX() + 0.5;
                    double blockCenterY = pos.getY() + 0.5;
                    double blockCenterZ = pos.getZ() + 0.5;

                    double dist = relativePos.distanceTo(data[0]);
                    if (dist <= paintRadius + noiseRange)
                    {
                        List<Pair<Vec3d, Direction>> points = new ArrayList<>(3);
//                        level.getChunk(pos).

//                        Vec3d worldClosestPos = data[0].add(pos.getCenter());
//                        points.add(new Tuple<>(worldClosestPos, Direction.getNearest(position.x - worldClosestPos.x, position.y - worldClosestPos.y, position.z - worldClosestPos.z)));
                        if (position.y > blockCenterY)
                            points.add(new Pair<>(new Vec3d(blockCenterX, blockCenterY + 0.5, blockCenterZ), Direction.UP));
                        else if (position.y < blockCenterY)
                            points.add(new Pair<>(new Vec3d(blockCenterX, blockCenterY - 0.5, blockCenterZ), Direction.DOWN));
                        if (position.x > blockCenterX)
                            points.add(new Pair<>(new Vec3d(blockCenterX + 0.5, blockCenterY, blockCenterZ), Direction.EAST));
                        else if (position.x < blockCenterX)
                            points.add(new Pair<>(new Vec3d(blockCenterX - 0.5, blockCenterY, blockCenterZ), Direction.WEST));
                        if (position.z > blockCenterZ)
                            points.add(new Pair<>(new Vec3d(blockCenterX, blockCenterY, blockCenterZ + 0.5), Direction.SOUTH));
                        else if (position.z < blockCenterZ)
                            points.add(new Pair<>(new Vec3d(blockCenterX, blockCenterY, blockCenterZ - 0.5), Direction.NORTH));
                        points.sort((tuple1, tuple2) -> Double.compare(tuple1.getLeft().squaredDistanceTo(position), tuple2.getLeft().squaredDistanceTo(position)));

                        for (Pair<Vec3d, Direction> point : points)
                        {
                            if (raycastAndGetDirection(relativePos, point.getLeft(), level, pos, point.getRight()) && InkBlockUtils.canInkFromFace(level, pos, point.getRight()))
                            {
                                dist = point.getLeft().distanceTo(position);

                                boolean inRadius = dist <= paintRadius - noiseRange;
                                if (!inRadius && dist <= paintRadius + noiseRange)
                                {
                                    double noiseProgress = (dist - (paintRadius - noiseRange)) / (noiseRange * 2);
                                    inRadius = (noiseProgress * noiseProgress) <= level.getRandom().nextFloat();
                                }
                                if (inRadius)
                                {
                                    set.add(new BlockFace(pos, point.getRight()));
                                    break;
                                }
                            }
                        }
                    }
                }
    }

    private Vec3d[] closestPointTo(VoxelShape shape, final Vec3d point)
    {
        if (shape.isEmpty())
        {
            return new Vec3d[]{};
        }
        else
        {
            Vec3d[] data = new Vec3d[3];
            shape.forEachBox((xmin, ymin, zmin, xmax, ymax, zmax) ->
            {
                double x = MathHelper.clamp(point.x, xmin, xmax);
                double y = MathHelper.clamp(point.y, ymin, ymax);
                double z = MathHelper.clamp(point.z, zmin, zmax);
                if (data[0] == null || point.squaredDistanceTo(x, y, z) < point.squaredDistanceTo(data[0]))
                {
                    data[0] = new Vec3d(x, y, z);
                    data[1] = new Vec3d(xmin, ymin, zmin);
                    data[2] = new Vec3d(xmax, ymax, zmax);
                }
            });
            return data;
        }
    }

    private boolean raycastAndGetDirection(Vec3d startPos, Vec3d endPos, ServerWorld world, BlockPos expectedPos, Direction expectedFace)
    {
        RaycastContext clipContext = new RaycastContext(new Vec3d(x, y, z), endPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, exploder); // actually don't know if it's faster to inline a method but ok
        InkColor ownerColor = ColorUtils.getEntityColor(exploder);
        BlockHitResult hitResult = BlockView.raycast(startPos, endPos, clipContext, (clipContext1, blockPos1) ->
        {
            BlockState blockstate = world.getBlockState(blockPos1);
            VoxelShape voxelshape = blockstate.getCollisionShape(world, blockPos1);

            if (blockstate.isOf(SplatcraftBlocks.allowedColorBarrier.get()))
            {
                ColoredBarrierBlock block = (ColoredBarrierBlock) blockstate.getBlock();
                if (ColorUtils.colorEquals(world, blockPos1, ownerColor, block.getColor(world, blockPos1)))
                {
                    return null;
                }
            }
            if (blockstate.isOf(SplatcraftBlocks.deniedColorBarrier.get()))
            {
                ColoredBarrierBlock block = (ColoredBarrierBlock) blockstate.getBlock();
                if (!ColorUtils.colorEquals(world, blockPos1, ownerColor, block.getColor(world, blockPos1)))
                {
                    return null;
                }
            }
            return world.raycastBlock(clipContext1.getStart(), clipContext1.getEnd(), blockPos1, voxelshape, blockstate);
        }, (clipContext1) ->
        {
            return BlockHitResult.createMissed(endPos, expectedFace, expectedPos);
//			return BlockHitResult.miss(startPos, Direction.UP, BlockPos.ZERO);
        });
        return (hitResult.getPos().equals(expectedPos) && hitResult.getSide().equals(expectedFace)) || hitResult.getType() == HitResult.Type.MISS;
    }

    /**
     * Does the second part of the explosion (sound, particles, drop spawn)
     */
    public void doExplosionCosmetics(boolean spawnParticles)
    {
        Vec3d explosionPos = new Vec3d(x + 0.5f, y + 0.5f, z + 0.5f);

        World world = exploder.getWorld();

        if (spawnParticles)
        {
            if (paintRadius < 2.0F)
            {
                world.addParticle(ParticleTypes.EXPLOSION, x, y, z, 1.0D, 0.0D, 0.0D);
            }
            else
            {
                world.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0D, 0.0D, 0.0D);
            }
        }

        for (BlockFace blockFace : affectedBlockPositions)
        {
            BlockState blockstate = world.getBlockState(blockFace.pos());
            if (!blockstate.isAir())
            {
                InkColor color = ColorUtils.getEntityColor(exploder);
                float dist = (float) Math.sqrt(blockFace.pos().getSquaredDistanceFromCenter(explosionPos.x, explosionPos.y, explosionPos.z));
                if (exploder instanceof PlayerEntity player)
                {
                    InkBlockUtils.playerInkBlock(player, world, blockFace.pos(), color, blockFace.face(), inkType, dmgCalculator.getDamage(dist));
                }
                else
                {
                    InkBlockUtils.inkBlock(world, blockFace.pos(), color, blockFace.face(), inkType, dmgCalculator.getDamage(dist));
                }
            }
        }
    }

    public Vec3d getPosition()
    {
        return position;
    }
}
