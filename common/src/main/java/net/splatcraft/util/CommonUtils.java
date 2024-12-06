package net.splatcraft.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.BaseRandom;
import net.minecraft.world.World;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.CommonRecords;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.awt.*;
import java.util.function.Predicate;

public class CommonUtils
{
    public static final EntityDataSerializer<Vec3d> VEC3SERIALIZER = new EntityDataSerializer<>()
    {
        @Override
        public void write(@NotNull FriendlyByteBuf buf, @NotNull Vec3d vec3)
        {
            buf.writeDouble(vec3.x);
            buf.writeDouble(vec3.y);
            buf.writeDouble(vec3.z);
        }

        @Override
        public @NotNull Vec3d read(@NotNull FriendlyByteBuf buf)
        {
            return new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }

        @Override
        public @NotNull Vec3d copy(@NotNull Vec3d vec3)
        {
            return new Vec3d(vec3.x, vec3.y, vec3.z);
        }
    };
    public static final EntityDataSerializer<Vec2> VEC2SERIALIZER = new EntityDataSerializer<>()
    {
        @Override
        public void write(@NotNull FriendlyByteBuf buf, @NotNull Vec2 vec2)
        {
            buf.writeFloat(vec2.x);
            buf.writeFloat(vec2.y);
        }

        @Override
        public @NotNull Vec2 read(@NotNull FriendlyByteBuf buf)
        {
            return new Vec2(buf.readFloat(), buf.readFloat());
        }

        @Override
        public @NotNull Vec2 copy(@NotNull Vec2 vec2)
        {
            return new Vec2(vec2.x, vec2.y);
        }
    };

    public static void spawnTestParticle(Vec3d pos)
    {
        spawnTestParticle(MinecraftClient.getInstance().world, new DustParticleEffect(new Vector3f(1, 0, 0), 1), pos);
    }

    public static void spawnTestText(Level level, Vec3d pos, String text)
    {
        if (level != null)
        {
            Display.TextDisplay entity = new Display.TextDisplay(EntityType.TEXT_DISPLAY, Minecraft.getInstance().level);
            entity.setPos(pos);
            entity.setText(Component.literal(text));
            entity.setBillboardConstraints(Display.BillboardConstraints.CENTER);
            level.addFreshEntity(entity);
        }
    }

    public static void spawnTestParticle(Vec3d pos, Color color)
    {
        float[] rgb = color.getRGBColorComponents(null);

        spawnTestParticle(MinecraftClient.getInstance().world, new DustParticleEffect(new Vector3f(rgb[0], rgb[1], rgb[2]), 3), pos);
    }

    public static void spawnTestBlockParticle(Vec3d pos, BlockState state)
    {
        spawnTestParticle(MinecraftClient.getInstance().world, new DustParticleEffect(ParticleTypes.BLOCK_MARKER, state), pos);
    }

    public static void spawnTestParticle(World world, ParticleEffect options, Vec3d pos)
    {
        if (world != null)
        {
            if (world instanceof ClientWorld clientLevel)
            {
                clientLevel.addParticle(options, true, pos.x, pos.y, pos.z, 0, 0, 0);
            }
            if (world instanceof ServerWorld serverLevel)
            {
                serverLevel.spawnParticles(options, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void showBoundingBoxCorners(World world, Box aabb)
    {
        for (int x = 0; x < 2; x++)
        {
            for (int y = 0; y < 2; y++)
            {
                for (int z = 0; z < 2; z++)
                {
                    CommonUtils.spawnTestParticle(world,
                        ParticleTypes.BUBBLE, new Vec3d(
                            x == 0 ? aabb.getMin(Direction.Axis.X) : aabb.getMax(Direction.Axis.X),
                            y == 0 ? aabb.getMin(Direction.Axis.Y) : aabb.getMax(Direction.Axis.Y),
                            z == 0 ? aabb.getMin(Direction.Axis.Z) : aabb.getMax(Direction.Axis.Z)));
                }
            }
        }
    }

    public static float nextFloat(BaseRandom random, float min, float max)
    {
        return min + (max - min) * random.nextFloat();
    }

    public static double nextDouble(RandomSource random, double min, double max)
    {
        return min + (max - min) * random.nextDouble();
    }

    public static net.minecraft.util.math.Vec3i round(Vec3d vec3)
    {
        return new Vec3i((int) Math.floor(vec3.x), (int) Math.floor(vec3.y), (int) Math.floor(vec3.z));
    }

    public static net.minecraft.util.math.BlockPos createBlockPos(double x, double y, double z)
    {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public static ChunkPos getChunkPos(BlockPos pos)
    {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static BlockPos createBlockPos(Vec3d vec3)
    {
        return new BlockPos(round(vec3));
    }

    private static boolean isValidNamespace(String namespaceIn)
    {
        for (int i = 0; i < namespaceIn.length(); ++i)
        {
            if (!validateNamespaceChar(namespaceIn.charAt(i)))
            {
                return false;
            }
        }

        return true;
    }

    public static boolean validatePathChar(char charValue)
    {
        return charValue == '_' || charValue == '-' || charValue >= 'a' && charValue <= 'z' || charValue >= '0' && charValue <= '9' || charValue == '/' || charValue == '.';
    }

    private static boolean validateNamespaceChar(char charValue)
    {
        return charValue == '_' || charValue == '-' || charValue >= 'a' && charValue <= 'z' || charValue >= '0' && charValue <= '9' || charValue == '.';
    }

    private static boolean isPathValid(String pathIn)
    {
        for (int i = 0; i < pathIn.length(); ++i)
        {
            if (!validatePathChar(pathIn.charAt(i)))
            {
                return false;
            }
        }

        return true;
    }

    public static boolean isResourceNameValid(String resourceName)
    {
        return isResourceNameValid(resourceName, Splatcraft.MODID);
    }

    public static boolean isResourceNameValid(String resourceName, String defaultLoc)
    {
        String[] astring = decompose(resourceName, ':', defaultLoc);
        return isValidNamespace(org.apache.commons.lang3.StringUtils.isEmpty(astring[0]) ? defaultLoc : astring[0]) && isPathValid(astring[1]);
    }

    protected static String[] decompose(String resourceName, char splitOn, String defaultLoc)
    {
        String[] astring = new String[]{defaultLoc, resourceName};
        int i = resourceName.indexOf(splitOn);
        if (i >= 0)
        {
            astring[1] = resourceName.substring(i + 1);
            if (i >= 1)
            {
                astring[0] = resourceName.substring(0, i);
            }
        }

        return astring;
    }

    public static void blockDrop(Level levelIn, BlockPos pos, ItemStack stack)
    {
        if (levelIn.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !levelIn.captureBlockSnapshots)
            spawnItem(levelIn, pos, stack);
    }

    public static void spawnItem(Level levelIn, BlockPos pos, ItemStack stack)
    {
        if (!levelIn.isClientSide() && !stack.isEmpty())
        {
            double d0 = (double) (levelIn.random.nextFloat() * 0.5F) + 0.25D;
            double d1 = (double) (levelIn.random.nextFloat() * 0.5F) + 0.25D;
            double d2 = (double) (levelIn.random.nextFloat() * 0.5F) + 0.25D;
            ItemEntity itementity = new ItemEntity(levelIn, (double) pos.getX() + d0, (double) pos.getY() + d1, (double) pos.getZ() + d2, stack);
            itementity.setDefaultPickUpDelay();
            levelIn.addFreshEntity(itementity);
        }
    }

    public static ItemStack getItemInInventory(Player entity, Predicate<ItemStack> predicate)
    {
        ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(entity, predicate);
        if (!itemstack.isEmpty())
            return itemstack;
        else
        {
            for (int i = 0; i < entity.getInventory().getContainerSize(); ++i)
            {
                ItemStack itemstack1 = entity.getInventory().getItem(i);
                if (predicate.test(itemstack1))
                    return itemstack1;
            }

            return ItemStack.EMPTY;
        }
    }

    public static boolean anyWeaponOnCooldown(Player player)
    {
        boolean isMainOnCooldown = player.getMainHandItem().getItem() instanceof WeaponBaseItem weapon && player.getCooldowns().isOnCooldown(weapon);
        boolean isOffOnCooldown = player.getOffhandItem().getItem() instanceof WeaponBaseItem weapon && player.getCooldowns().isOnCooldown(weapon);
        return isMainOnCooldown || isOffOnCooldown;
    }

    public static float lerpRotation(float value, float a, float b)
    {
        while (b - a < -180.0F)
        {
            a -= 360.0F;
        }

        while (b - a >= 180.0F)
        {
            a += 360.0F;
        }

        return MathHelper.lerp(0.2F, a, b);
    }

    public static <S, V> V getArgumentOrDefault(CommandContext<S> context, String name, Class<V> clazz, V defaultValue) // why do i have to do this though
    {
        try
        {
            return context.getArgument(name, clazz);
        }
        catch (IllegalArgumentException e)
        {
            if (e.getMessage().startsWith("No such argument '"))
            {
                return defaultValue;
            }
            throw e;
        }
    }

    public static <S> boolean hasArgument(CommandContext<S> context, String name) // why do i have to do this though
    {
        try
        {
            context.getArgument(name, Object.class);
        }
        catch (IllegalArgumentException e)
        {
            if (e.getMessage().startsWith("No such argument '"))
            {
                return false;
            }
        }
        return true;
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

    public static Vec3d getOldPosition(Entity entity, double partialTick)
    {
        if (entity instanceof Player player)
            return WeaponHandler.getPlayerPrevPos(player).getPosition(partialTick);
        throw new NotImplementedException();
    }

    public static <T> T returnValueDependantOnSquidCancel(Player player, T withCancel, T withoutCancel)
    {
        boolean didCancel = false;
        if (PlayerInfoCapability.hasCapability(player))
        {
            didCancel = PlayerInfoCapability.get(player).didSquidCancelThisTick();
        }
        return didCancel ? withCancel : withoutCancel;
    }

    public static float startupSquidSwitch(LivingEntity entity, CommonRecords.ShotDataRecord shotData)
    {
        if (entity instanceof Player player)
            return returnValueDependantOnSquidCancel(player, shotData.squidStartupTicks(), shotData.startupTicks());
        return shotData.startupTicks();
    }

    public static float startupSquidSwitch(LivingEntity entity, ShootingHandler.FiringStatData firingData)
    {
        if (entity instanceof Player player)
            return returnValueDependantOnSquidCancel(player, firingData.squidStartupFrames(), firingData.startupFrames());
        return firingData.startupFrames();
    }

    public static float triangle(RandomSource random, float min, float max)
    {
        return min + max * (random.nextFloat() - random.nextFloat());
    }

    public static boolean isRolling(LivingEntity entity)
    {
        return entity instanceof Player player && PlayerCooldown.hasPlayerCooldown(player) && PlayerCooldown.getPlayerCooldown(player) instanceof DualieItem.DodgeRollCooldown;
    }

    public static InteractionHand otherHand(InteractionHand hand)
    {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    public record Result(float delay, float value)
    {
    }
}