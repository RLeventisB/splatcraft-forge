package net.splatcraft.util.structs;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.splatcraft.Splatcraft;
import net.splatcraft.util.CodecUtils;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class AttackId
{
	public static final AttackId NONE = new EmptyAttackId();
	public static final List<AttackId> attackIdList = new ObjectArrayList<>();
	private static final ResourceKey<Registry<Class<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(Splatcraft.identifierOf("attack_id"));
	private static final MappedRegistry<Class<?>> REGISTRY = new MappedRegistry<>(REGISTRY_KEY, Lifecycle.stable());
	private static final Object2ObjectOpenHashMap<ResourceLocation, MapCodec<? extends AttackId>> ID_TO_CODEC_MAP = new Object2ObjectOpenHashMap<>();
	private static short nextAttackId = 0;
	static
	{
		register("default", DefaultAttackId.CODEC, DefaultAttackId.class);
		register("selective", DamageSelectiveAttackId.CODEC, DamageSelectiveAttackId.class);
		register("empty", EmptyAttackId.CODEC, EmptyAttackId.class);
	}
	private static <T extends AttackId> void register(String name, MapCodec<? extends T> codec, Class<T> clazz)
	{
		ResourceLocation id = Splatcraft.identifierOf(name);
		Registry.register(REGISTRY, id, clazz);
		ID_TO_CODEC_MAP.put(id, codec);
	}
	public static DefaultAttackId registerAttack()
	{
		return registerAttack(null);
	}
	public static DefaultAttackId registerAttack(Integer expected)
	{
		DefaultAttackId attackId = expected == null ? new DefaultAttackId(assignAttackId()) : new DefaultAttackId(assignAttackId(), expected);
		attackIdList.add(attackId);
		return attackId;
	}
	public static DamageSelectiveAttackId registerSelectiveAttack()
	{
		return registerSelectiveAttack(null);
	}
	public static DamageSelectiveAttackId registerSelectiveAttack(Integer expected)
	{
		DamageSelectiveAttackId attackId = expected == null ? new DamageSelectiveAttackId(assignAttackId()) : new DamageSelectiveAttackId(assignAttackId(), expected);
		attackIdList.add(attackId);
		return attackId;
	}
	public static short assignAttackId()
	{
		return nextAttackId++;
	}
	public static <T> AttackId parseAttackId(DynamicOps<T> ops, T input)
	{
		DataResult<T> identifierField = ops.get(input, "identifier");
		if (identifierField.isError())
		{
			throw new AssertionError("Error upon reading Identifier for an AttackId, field was not found.\n" + identifierField.error().get().message());
		}

		DataResult<ResourceLocation> identifierDataResult = ResourceLocation.CODEC.parse(ops, identifierField.getOrThrow());
		if (identifierDataResult.isError())
		{
			throw new AssertionError("Error upon reading Identifier for an AttackId.\n" + identifierDataResult.error().get().message());
		}
		MapCodec<? extends AttackId> codec = ID_TO_CODEC_MAP.get(identifierDataResult.getOrThrow());
		if (codec == null)
		{
			throw new AssertionError("Error upon reading encoded AttackId, id doesn't match any entry.");
		}
		DataResult<? extends AttackId> attackIdDataResult = codec.codec().parse(ops, input);
		if (attackIdDataResult.isError())
		{
			throw new AssertionError("Error upon reading encoded AttackId.\n" + attackIdDataResult.error().get().message());
		}
		return attackIdDataResult.getOrThrow();
	}
	public static <T, A extends AttackId> T encodeAttackId(DynamicOps<T> ops, A attackId)
	{
		ResourceLocation id = REGISTRY.getKey(attackId.getClass());
		if (id == null)
		{
			throw new AssertionError("AttackId failed encoding, invalid id.");
		}

		DataResult<T> identifierDataResult = ResourceLocation.CODEC.encodeStart(ops, id);
		if (identifierDataResult.isError())
		{
			throw new AssertionError("AttackId failed encoding, error upon encoding id %s.\n%s".formatted(id, identifierDataResult.error().get().message()));
		}

		DataResult<T> dataResult = attackId.getCodec().codec().encodeStart(ops, attackId);

		if (dataResult.isError())
		{
			throw new AssertionError("AttackId failed encoding, error upon encoding AttackId %s.\n%s".formatted(attackId, dataResult.error().get().message()));
		}

		RecordBuilder<T> builder = ops.mapBuilder().add("identifier", identifierDataResult);
		return builder.build(dataResult).getOrThrow();
	}
	public abstract short getId();
	public AttackId countProjectile()
	{
		return countProjectile(1);
	}
	public AttackId countProjectile(Collection<?> collection)
	{
		return countProjectile(collection.size());
	}
	public abstract AttackId countProjectile(int count);
	public abstract void projectileRemoved();
	public float getDamage(Entity entity, float damage)
	{
		return damage;
	}
	public abstract <SELF extends AttackId> MapCodec<SELF> getCodec();
	public abstract static class PrimitiveImplementedAttackId extends AttackId
	{
		protected final Short id;
		protected byte projectileCount;
		protected PrimitiveImplementedAttackId(Short id)
		{
			this.id = id;
		}
		@Override
		public short getId()
		{
			return id;
		}
		@Override
		public AttackId countProjectile(int count)
		{
			projectileCount += (byte) count;
			return this;
		}
		public byte getRemainingHits()
		{
			return projectileCount;
		}
		@Override
		public void projectileRemoved()
		{
			projectileCount--;
			if (projectileCount <= 0)
			{
				if (!attackIdList.remove(this))
				{
					Splatcraft.LOGGER.warn("Error trying removing attack id {}", id);
				}
			}
		}
	}
	public static final class DefaultAttackId extends PrimitiveImplementedAttackId
	{
		public static final MapCodec<DefaultAttackId> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
			Codec.SHORT.fieldOf("id").forGetter(DefaultAttackId::getId),
			Codec.BYTE.fieldOf("projectile_count").forGetter(DefaultAttackId::getRemainingHits),
			CodecUtils.collection(UUIDUtil.AUTHLIB_CODEC, ObjectOpenHashSet::new).fieldOf("hit_enemies").forGetter(v -> v.hitEnemies)
		).apply(inst, DefaultAttackId::new));
		public final ObjectOpenHashSet<UUID> hitEnemies;
		public DefaultAttackId(Short id)
		{
			super(id);

			hitEnemies = new ObjectOpenHashSet<>();
		}
		public DefaultAttackId(Short id, int expected)
		{
			super(id);

			hitEnemies = new ObjectOpenHashSet<>(expected);
		}
		public DefaultAttackId(short id, byte projectileCount, ObjectOpenHashSet<UUID> hitEnemies)
		{
			super(id);
			this.projectileCount = projectileCount;
			this.hitEnemies = hitEnemies;
		}
		public float getDamage(Entity entity, float damage)
		{
			if (!hitEnemies.contains(entity.getUUID()))
			{
				hitEnemies.add(entity.getUUID());
				return damage;
			}
			return 0;
		}
		@Override
		public MapCodec<DefaultAttackId> getCodec()
		{
			return CODEC;
		}
	}
	public static final class EmptyAttackId extends AttackId
	{
		public static final MapCodec<EmptyAttackId> CODEC = MapCodec.unit(EmptyAttackId::new);
		@Override
		public short getId()
		{
			return 0;
		}
		@Override
		public EmptyAttackId countProjectile(int count)
		{
			return this;
		}
		@Override
		public void projectileRemoved()
		{

		}
		@Override
		public MapCodec<EmptyAttackId> getCodec()
		{
			return CODEC;
		}
	}
}
