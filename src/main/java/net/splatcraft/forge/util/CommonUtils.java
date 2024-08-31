package net.splatcraft.forge.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import org.joml.Vector3f;

import java.awt.*;
import java.util.function.Predicate;

public class CommonUtils
{
    public static void spawnTestParticle(Vec3 pos)
    {
        spawnTestParticle(Minecraft.getInstance().level, new DustParticleOptions(new Vector3f(1, 0, 0), 1), pos);
    }

    public static void spawnTestText(Level level, Vec3 pos, String text)
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

    public static void spawnTestParticle(Vec3 pos, Color color)
    {
        float[] rgb = color.getRGBColorComponents(null);

        spawnTestParticle(Minecraft.getInstance().level, new DustParticleOptions(new Vector3f(rgb[0], rgb[1], rgb[2]), 3), pos);
    }

    public static void spawnTestBlockParticle(Vec3 pos, BlockState state)
    {
        spawnTestParticle(Minecraft.getInstance().level, new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state), pos);
    }

    public static void spawnTestParticle(Level level, ParticleOptions options, Vec3 pos)
    {
        if (level != null)
        {
            if (level instanceof ClientLevel clientLevel)
            {
                clientLevel.addParticle(options, true, pos.x, pos.y, pos.z, 0, 0, 0);
            }
            if (level instanceof ServerLevel serverLevel)
            {
                serverLevel.sendParticles(options, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }
    }

    public static void showBoundingBoxCorners(Level level, AABB aabb)
    {
        for (int x = 0; x < 2; x++)
        {
            for (int y = 0; y < 2; y++)
            {
                for (int z = 0; z < 2; z++)
                {
                    CommonUtils.spawnTestParticle(level,
                            ParticleTypes.BUBBLE, new Vec3(
                                    x == 0 ? aabb.min(Direction.Axis.X) : aabb.max(Direction.Axis.X),
                                    y == 0 ? aabb.min(Direction.Axis.Y) : aabb.max(Direction.Axis.Y),
                                    z == 0 ? aabb.min(Direction.Axis.Z) : aabb.max(Direction.Axis.Z)));
                }
            }
        }
    }

    public static float nextFloat(RandomSource random, float min, float max)
    {
        return min + (max - min) * random.nextFloat();
    }

    public static double nextDouble(RandomSource random, double min, double max)
    {
        return min + (max - min) * random.nextDouble();
    }

    public static Vec3i round(Vec3 vec3)
    {
        return new Vec3i((int) Math.floor(vec3.x), (int) Math.floor(vec3.y), (int) Math.floor(vec3.z));
    }

    public static BlockPos createBlockPos(double x, double y, double z)
    {
        return new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public static ChunkPos getChunkPos(BlockPos pos)
    {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static BlockPos createBlockPos(Vec3 vec3)
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

        return Mth.lerp(0.2F, a, b);
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
}