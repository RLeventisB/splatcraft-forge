package net.splatcraft.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.DataResult;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.renderer.InkSquidRenderer;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.handlers.ShootingHandler;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.CommonRecords;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class CommonUtils
{
    public static final TrackedDataHandler<Vector2f> VEC2DATAHANDLER = new TrackedDataHandler<>()
    {
        public static final PacketCodec<RegistryByteBuf, Vector2f> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.FLOAT, Vector2f::x,
            PacketCodecs.FLOAT, Vector2f::y,
            Vector2f::new);

        @Override
        public PacketCodec<? super RegistryByteBuf, Vector2f> codec()
        {
            return PACKET_CODEC;
        }

        @Override
        public @NotNull Vector2f copy(@NotNull Vector2f vec2)
        {
            return new Vector2f(vec2.x, vec2.y);
        }
    };

    public static final TrackedDataHandler<InkColor> INKCOLORDATAHANDLER = new TrackedDataHandler<>()
    {
        public static final PacketCodec<RegistryByteBuf, InkColor> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, InkColor::getColor,
            InkColor::new);

        @Override
        public PacketCodec<? super RegistryByteBuf, InkColor> codec()
        {
            return PACKET_CODEC;
        }

        @Override
        public @NotNull InkColor copy(@NotNull InkColor color)
        {
            return InkColor.constructOrReuse(color.getColor());
        }
    };

    public static CustomPayload.Id<?> createIdFromClass(Class<?> clazz)
    {
        StringBuilder builder = new StringBuilder();
        for (char chr : clazz.getSimpleName().toCharArray())
        {
            if (chr == '-' || chr == '_' || chr == '.' || Character.isLetterOrDigit(chr))
            {
                if (Character.isUpperCase(chr))
                {
                    builder.append('_');
                    builder.append(Character.toLowerCase(chr));
                }
                else builder.append(chr);
            }
        }
        return new CustomPayload.Id<>(Splatcraft.identifierOf(builder.toString()));
    }

    public static String makeStringIdentifierValid(String text)
    {
        StringBuilder builder = new StringBuilder();
        for (char chr : text.toCharArray())
        {
            if (chr == '-' || chr == '_' || chr == '.' || Character.isLetterOrDigit(chr))
            {
                if (Character.isUpperCase(chr))
                {
                    builder.append('_');
                    builder.append(Character.toLowerCase(chr));
                }
                else builder.append(chr);
            }
        }
        return builder.toString();
    }

    public static void spawnTestParticle(Vec3d pos)
    {
        spawnTestParticle(MinecraftClient.getInstance().world, new DustParticleEffect(new Vector3f(1, 0, 0), 1), pos);
    }

    public static void spawnTestText(World world, Vec3d pos, String text)
    {
        if (world != null)
        {
            DisplayEntity.TextDisplayEntity entity = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, MinecraftClient.getInstance().world);
            entity.setPosition(pos);
            entity.setText(Text.literal(text));
            entity.setDisplayFlags(DisplayEntity.BillboardMode.CENTER.getIndex());
            world.spawnEntity(entity);
        }
    }

    public static void spawnTestParticle(Vec3d pos, Color color)
    {
        float[] rgb = color.getRGBColorComponents(null);

        spawnTestParticle(MinecraftClient.getInstance().world, new DustParticleEffect(new Vector3f(rgb[0], rgb[1], rgb[2]), 3), pos);
    }

    public static void spawnTestBlockParticle(Vec3d pos, BlockState state)
    {
        spawnTestParticle(MinecraftClient.getInstance().world, new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, state), pos);
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
                    spawnTestParticle(world,
                        ParticleTypes.BUBBLE, new Vec3d(
                            x == 0 ? aabb.getMin(Direction.Axis.X) : aabb.getMax(Direction.Axis.X),
                            y == 0 ? aabb.getMin(Direction.Axis.Y) : aabb.getMax(Direction.Axis.Y),
                            z == 0 ? aabb.getMin(Direction.Axis.Z) : aabb.getMax(Direction.Axis.Z)));
                }
            }
        }
    }

    public static float nextFloat(Random random, float min, float max)
    {
        return min + (max - min) * random.nextFloat();
    }

    public static double nextDouble(Random random, double min, double max)
    {
        return min + (max - min) * random.nextDouble();
    }

    public static Vec3i round(Vec3d vec3)
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

    public static BlockPos createBlockPos(Vec3d vec3)
    {
        return new BlockPos(round(vec3));
    }

    public static void blockDrop(World world, BlockPos pos, ItemStack stack)
    {
        if (world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS) /*&& !world.captureBlockSnapshots*/)
            spawnItem(world, pos, stack);
    }

    public static void spawnItem(World world, BlockPos pos, ItemStack stack)
    {
        if (!world.isClient() && !stack.isEmpty())
        {
            double d0 = (double) (world.random.nextFloat() * 0.5F) + 0.25D;
            double d1 = (double) (world.random.nextFloat() * 0.5F) + 0.25D;
            double d2 = (double) (world.random.nextFloat() * 0.5F) + 0.25D;
            ItemEntity itementity = new ItemEntity(world, (double) pos.getX() + d0, (double) pos.getY() + d1, (double) pos.getZ() + d2, stack);
            itementity.setToDefaultPickupDelay();
            world.spawnEntity(itementity);
        }
    }

    public static ItemStack getItemInInventory(PlayerEntity entity, Predicate<ItemStack> predicate)
    {
        ItemStack itemstack = RangedWeaponItem.getHeldProjectile(entity, predicate);
        if (!itemstack.isEmpty())
            return itemstack;
        else
        {
            for (int i = 0; i < entity.getInventory().size(); ++i)
            {
                ItemStack itemstack1 = entity.getInventory().getStack(i);
                if (predicate.test(itemstack1))
                    return itemstack1;
            }

            return ItemStack.EMPTY;
        }
    }

    public static boolean anyWeaponOnCooldown(PlayerEntity player)
    {
        boolean isMainOnCooldown = player.getMainHandStack().getItem() instanceof WeaponBaseItem weapon && player.getItemCooldownManager().isCoolingDown(weapon);
        boolean isOffOnCooldown = player.getOffHandStack().getItem() instanceof WeaponBaseItem weapon && player.getItemCooldownManager().isCoolingDown(weapon);
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
        if (entity instanceof PlayerEntity player)
            return WeaponHandler.getPlayerPrevPos(player).getPosition(partialTick);
        throw new NotImplementedException();
    }

    public static <T> T returnValueDependantOnSquidCancel(LivingEntity player, T withCancel, T withoutCancel)
    {
        boolean didCancel = false;
        if (EntityInfoCapability.hasCapability(player))
        {
            didCancel = EntityInfoCapability.get(player).didSquidCancelThisTick();
        }
        return didCancel ? withCancel : withoutCancel;
    }

    public static float startupSquidSwitch(LivingEntity entity, CommonRecords.ShotDataRecord shotData)
    {
//        if (entity instanceof PlayerEntity player)
        return returnValueDependantOnSquidCancel(entity, shotData.squidStartupTicks(), shotData.startupTicks());
//        return shotData.startupTicks();
    }

    public static float startupSquidSwitch(LivingEntity entity, ShootingHandler.FiringStatData firingData)
    {
//        if (entity instanceof PlayerEntity player)
        return returnValueDependantOnSquidCancel(entity, firingData.squidStartupFrames(), firingData.startupFrames());
//        return firingData.startupFrames();
    }

    public static float triangle(Random random, float min, float max)
    {
        return min + max * (random.nextFloat() - random.nextFloat());
    }

    public static boolean isRolling(LivingEntity entity)
    {
        return entity instanceof PlayerEntity player && PlayerCooldown.hasPlayerCooldown(player) && PlayerCooldown.getPlayerCooldown(player) instanceof DualieItem.DodgeRollCooldown;
    }

    public static Hand otherHand(Hand hand)
    {
        return hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
    }

    public static <K, V> DataResult<V> getFromMap(Map<K, V> map, K key)
    {
        if (map.containsKey(key))
        {
            return DataResult.success(map.get(key));
        }
        return DataResult.error(() -> "The key " + key + " is not registered in the map.");
    }

    @ExpectPlatform
    public static <T> int @Nullable [] findMatches(List<T> inputs, java.util.List<? extends Predicate<T>> tests)
    {
        throw new AssertionError();
    }

    public static RegistryEntry.Reference<Enchantment> getEnchantmentEntry(WorldView world, RegistryKey<Enchantment> enchantment)
    {
        return world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(enchantment).get();
    }

    @ExpectPlatform
    public static void doPlayerSquidForgeEvent(AbstractClientPlayerEntity player, InkSquidRenderer squidRenderer, float g, MatrixStack matrixStack, VertexConsumerProvider consumerProvider, int i)
    {

    }

    @ExpectPlatform
    public static InteractionEventResultDummy doPlayerUseItemForgeEvent(int i, KeyBinding useKey, Hand hand)
    {
        return new InteractionEventResultDummy(true, false);
    }

    @ExpectPlatform
    public static void doForgeEmptyClickEvent(ClientPlayerEntity player, Hand hand)
    {

    }

    public record Result(float delay, float value)
    {
    }

    public static class InteractionEventResultDummy
    {
        private final boolean canceled;
        private boolean handSwing = true;

        public InteractionEventResultDummy(boolean handSwing, boolean canceled)
        {
            this.handSwing = handSwing;
            this.canceled = canceled;
        }

        public boolean shouldSwingHand()
        {
            return handSwing;
        }

        public boolean isCanceled()
        {
            return canceled;
        }
    }
}