package net.splatcraft.util;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerWorld;
import net.minecraft.util.MathHelper;
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
import net.minecraft.world.phys.Vec3d;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.registries.SplatcraftBlocks;
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
    private final Vec3d position;
    private final InkBlockUtils.InkType inkType;
    private final DamageRangesRecord dmgCalculator;
    private final ItemStack weapon;

    public InkExplosion(@Nullable Entity source, double x, double y, double z, DamageRangesRecord damageCalculator, float paintRadius, InkBlockUtils.InkType inkType, ItemStack weapon, AttackId attackId)
    {
        this.exploder = source;
        this.paintRadius = paintRadius;
        this.x = x;
        this.y = y;
        this.z = z;
        this.attackId = attackId;
        this.position = new Vec3d(this.x, this.y, this.z);

        this.inkType = inkType;
        this.dmgCalculator = damageCalculator;
        this.weapon = weapon;
    }

    public static Vec3d adjustPosition(Vec3d pos, Vec3i normal)
    {
        float modifier = 0.01f;
        return pos.add(normal.getX() * modifier, normal.getY() * modifier, normal.getZ() * modifier);
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
        if (source == null || source.getWorld().isClientSide)
            return;

        InkExplosion inksplosion = new InkExplosion(source, pos.x(), pos.y(), pos.z(), damageManager, paintRadius, type, weapon, attackId);

        inksplosion.doExplosionA();
        inksplosion.doExplosionCosmetics(false);
    }

    /**
     * Does the first part of the explosion (destroy blocks)
     */
    public void doExplosionA()
    {
        List<BlockFace> set = new ArrayList<>();
        ServerWorld level = (ServerWorld) exploder.getWorld();
        Vec3d explosionPos = new Vec3d(x, y, z);
        getBlocksInSphereWithNoise(set, level);

        this.affectedBlockPositions.addAll(set);
        if (dmgCalculator.isInsignificant())
            return;
        float radiusSquared = dmgCalculator.getMaxDistance() * dmgCalculator.getMaxDistance();
        int k1 = MathHelper.floor(this.x - dmgCalculator.getMaxDistance() - 1F);
        int l1 = MathHelper.floor(this.x + dmgCalculator.getMaxDistance() + 1F);
        int i2 = MathHelper.floor(this.y - dmgCalculator.getMaxDistance() - 1F);
        int i1 = MathHelper.floor(this.y + dmgCalculator.getMaxDistance() + 1F);
        int j2 = MathHelper.floor(this.z - dmgCalculator.getMaxDistance() - 1F);
        int j1 = MathHelper.floor(this.z + dmgCalculator.getMaxDistance() + 1F);
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, new AABB(k1, i2, j2, l1, i1, j1));

        int color = ColorUtils.getEntityColor(exploder);
        for (LivingEntity entity : list)
        {
            AABB boundingBox = entity.getBoundingBox();
            Vec3d closestPos = new Vec3d(MathHelper.clamp(explosionPos.x, boundingBox.minX, boundingBox.maxX), MathHelper.clamp(explosionPos.y, boundingBox.minY, boundingBox.maxY), MathHelper.clamp(explosionPos.z, boundingBox.minZ, boundingBox.maxZ));

            float distance = (float) explosionPos.distanceToSqr(closestPos);
            if (distance > radiusSquared) // still collides even in the center isn't in radius
                continue;
            int targetColor = ColorUtils.getEntityColor(entity);
            if (targetColor == -1 || (color != targetColor && targetColor > -1))
            {
                float seenPercent = Explosion.getSeenPercent(explosionPos, entity);

                InkDamageUtils.doSplatDamage(entity, dmgCalculator.getDamage(MathHelper.sqrt(distance)) * seenPercent, exploder, weapon, attackId);
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

    private void getBlocksInSphereWithNoise(List<BlockFace> set, ServerWorld level)
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

                    VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
                    Vec3d relativePos = position.subtract(pos.getCenter());
                    Vec3d[] data = closestPointTo(shape, relativePos);
                    if (data.length == 0)
                        continue;

                    double blockCenterX = pos.getX() + 0.5;
                    double blockCenterY = pos.getY() + 0.5;
                    double blockCenterZ = pos.getZ() + 0.5;

                    double dist = relativePos.distanceTo(data[0]);
                    if (dist <= paintRadius + noiseRange)
                    {
                        List<Tuple<Vec3d, Direction>> points = new ArrayList<>(3);
//                        level.getChunk(pos).

//                        Vec3d worldClosestPos = data[0].add(pos.getCenter());
//                        points.add(new Tuple<>(worldClosestPos, Direction.getNearest(position.x - worldClosestPos.x, position.y - worldClosestPos.y, position.z - worldClosestPos.z)));
                        if (position.y > blockCenterY)
                            points.add(new Tuple<>(new Vec3d(blockCenterX, blockCenterY + 0.5, blockCenterZ), Direction.UP));
                        else if (position.y < blockCenterY)
                            points.add(new Tuple<>(new Vec3d(blockCenterX, blockCenterY - 0.5, blockCenterZ), Direction.DOWN));
                        if (position.x > blockCenterX)
                            points.add(new Tuple<>(new Vec3d(blockCenterX + 0.5, blockCenterY, blockCenterZ), Direction.EAST));
                        else if (position.x < blockCenterX)
                            points.add(new Tuple<>(new Vec3d(blockCenterX - 0.5, blockCenterY, blockCenterZ), Direction.WEST));
                        if (position.z > blockCenterZ)
                            points.add(new Tuple<>(new Vec3d(blockCenterX, blockCenterY, blockCenterZ + 0.5), Direction.SOUTH));
                        else if (position.z < blockCenterZ)
                            points.add(new Tuple<>(new Vec3d(blockCenterX, blockCenterY, blockCenterZ - 0.5), Direction.NORTH));
                        points.sort((tuple1, tuple2) -> Double.compare(tuple1.getA().distanceToSqr(position), tuple2.getA().distanceToSqr(position)));

                        for (Tuple<Vec3d, Direction> point : points)
                        {
                            if (raycastAndGetDirection(relativePos, point.getA(), level, pos, point.getB()) && InkBlockUtils.canInkFromFace(level, pos, point.getB()))
                            {
                                dist = point.getA().distanceTo(position);

                                boolean inRadius = dist <= paintRadius - noiseRange;
                                if (!inRadius && dist <= paintRadius + noiseRange)
                                {
                                    double noiseProgress = (dist - (paintRadius - noiseRange)) / (noiseRange * 2);
                                    inRadius = (noiseProgress * noiseProgress) <= level.getRandom().nextFloat();
                                }
                                if (inRadius)
                                {
                                    set.add(new BlockFace(pos, point.getB()));
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
            shape.forAllBoxes((xmin, ymin, zmin, xmax, ymax, zmax) ->
            {
                double x = MathHelper.clamp(point.x(), xmin, xmax);
                double y = MathHelper.clamp(point.y(), ymin, ymax);
                double z = MathHelper.clamp(point.z(), zmin, zmax);
                if (data[0] == null || point.distanceToSqr(x, y, z) < point.distanceToSqr(data[0]))
                {
                    data[0] = new Vec3d(x, y, z);
                    data[1] = new Vec3d(xmin, ymin, zmin);
                    data[2] = new Vec3d(xmax, ymax, zmax);
                }
            });
            return data;
        }
    }

    private boolean raycastAndGetDirection(Vec3d startPos, Vec3d endPos, ServerWorld level, BlockPos expectedPos, Direction expectedFace)
    {
        ClipContext clipContext = new ClipContext(new Vec3d(x, y, z), endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null); // actually don't know if it's faster to inline a method but ok
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
        Vec3d explosionPos = new Vec3d(x + 0.5f, y + 0.5f, z + 0.5f);

        Level level = exploder.getWorld();

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
                float dist = (float) Math.sqrt(blockFace.pos().distToCenterSqr(explosionPos.x, explosionPos.y, explosionPos.z));
                if (exploder instanceof Player player)
                {
                    InkBlockUtils.playerInkBlock(player, level, blockFace.pos(), color, blockFace.face(), inkType, dmgCalculator.getDamage(dist));
                }
                else
                {
                    InkBlockUtils.inkBlock(level, blockFace.pos(), color, blockFace.face(), inkType, dmgCalculator.getDamage(dist));
                }
            }
        }
    }

    public Vec3d getPosition()
    {
        return this.position;
    }
}
