package net.splatcraft.forge.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class InkExplosion
{
    public static final List<Vec3> rays = new ArrayList<>();
    private final double x;
    private final double y;
    private final double z;
    @Nullable
    private final Entity exploder;
    private final float size;
    private final List<BlockPos> affectedBlockPositions = Lists.newArrayList();
    private final Vec3 position;

    private final InkBlockUtils.InkType inkType;
    private final boolean damageMobs;
    private final float minDamage;
    private final float maxDamage;
    private final float blockDamage;
    private final ItemStack weapon;

    public InkExplosion(@Nullable Entity source, double x, double y, double z, float blockDamage, float minDamage, float maxDamage, boolean damageMobs, float size, InkBlockUtils.InkType inkType, ItemStack weapon) {
        this.exploder = source;
        this.size = size;
        this.x = x;
        this.y = y;
        this.z = z;
        this.position = new Vec3(this.x, this.y, this.z);


        this.inkType = inkType;
        this.damageMobs = damageMobs;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.blockDamage = blockDamage;
        this.weapon = weapon;
    }

    public static void createInkExplosion(Entity source, BlockPos pos, float size, float blockDamage, float damage, boolean damageMobs, InkBlockUtils.InkType type, ItemStack weapon) {
        createInkExplosion(source, pos, size, blockDamage, 0, damage, damageMobs, type, weapon);
    }

    public static void createInkExplosion(Entity source, BlockPos pos, float size, float blockDamage, float minDamage, float maxDamage, boolean damageMobs, InkBlockUtils.InkType type, ItemStack weapon) {

        if (source == null || source.getLevel().isClientSide)
            return;

        InkExplosion inksplosion = new InkExplosion(source, pos.getX(), pos.getY(), pos.getZ(), blockDamage, minDamage, maxDamage, damageMobs, size, type, weapon);

        inksplosion.doExplosionA();
        inksplosion.doExplosionCosmetics(false);
    }

    /**
     * Does the first part of the explosion (destroy blocks)
     */
    public void doExplosionA()
    {
        Set<BlockPos> set = Sets.newHashSet();
        Level level = exploder.getLevel();
        Vec3 explosionPos = new Vec3(x + 0.5f, y + 0.5f, z + 0.5f);

        for (Vec3 ray : rays)
        {
            float intensity = this.size * (0.7F + level.getRandom().nextFloat() * 0.6F);
            double x = explosionPos.x();
            double y = explosionPos.y();
            double z = explosionPos.z();

            while(intensity > 0.0)
            {
                BlockHitResult raytrace = level.clip(new ClipContext(new Vec3(explosionPos.x, explosionPos.y, explosionPos.z), new Vec3(x, y, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
                if (InkBlockUtils.canInkFromFace(level, raytrace.getBlockPos(), raytrace.getDirection()))
                    set.add(raytrace.getBlockPos());

                x += ray.x;
                y += ray.y;
                z += ray.z;
                intensity -= 0.315f;
            }
        }

        this.affectedBlockPositions.addAll(set);
        if(!damageMobs)
            return;
        float f2 = this.size;
        int k1 = Mth.floor(this.x - (double) f2 - 1.0D);
        int l1 = Mth.floor(this.x + (double) f2 + 1.0D);
        int i2 = Mth.floor(this.y - (double) f2 - 1.0D);
        int i1 = Mth.floor(this.y + (double) f2 + 1.0D);
        int j2 = Mth.floor(this.z - (double) f2 - 1.0D);
        int j1 = Mth.floor(this.z + (double) f2 + 1.0D);
        List<Entity> list = level.getEntities(this.exploder, new AABB(k1, i2, j2, l1, i1, j1));

        int color = ColorUtils.getEntityColor(exploder);
        for (Entity entity : list)
        {
            int targetColor = -2;
            if (entity instanceof LivingEntity)
                targetColor = ColorUtils.getEntityColor(entity);

            if (targetColor == -1 || (color != targetColor && targetColor > -1))
            {
                double f2Sq = f2 * f2;
                float pctg = Math.max(0, (float) ((f2Sq - entity.distanceToSqr(x + 0.5f, y + 0.5f, z + 0.5f)) / f2Sq));

                InkDamageUtils.doSplatDamage((LivingEntity) entity, Mth.lerp(pctg, minDamage, maxDamage) * Explosion.getSeenPercent(explosionPos, entity), exploder, weapon);
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

    /**
     * Does the second part of the explosion (sound, particles, drop spawn)
     */
    public void doExplosionCosmetics(boolean spawnParticles)
    {
        Level level = exploder.getLevel();

        if (spawnParticles)
        {
            if (this.size < 2.0F)
            {
                level.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
            }
            else
            {
                level.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
            }
        }

        Collections.shuffle(this.affectedBlockPositions, level.random);

        for (BlockPos blockpos : this.affectedBlockPositions)
        {
            BlockState blockstate = level.getBlockState(blockpos);
            if (!blockstate.isAir())
            {
                int color = ColorUtils.getEntityColor(exploder);
                if (exploder instanceof Player player)
                {
                    InkBlockUtils.playerInkBlock(player, level, blockpos, color, blockDamage, inkType);
                }
                else
                {
                    InkBlockUtils.inkBlock(level, blockpos, color, blockDamage, inkType);
                }
            }

        }
    }

    public Vec3 getPosition()
    {
        return this.position;
    }
}
