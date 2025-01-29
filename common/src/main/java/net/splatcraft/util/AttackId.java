package net.splatcraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.splatcraft.Splatcraft;

import java.util.List;
import java.util.UUID;

public abstract class AttackId
{
	public static final AttackId NONE = new EmptyAttackId();
	public static final List<AttackId> attackIdList = new ObjectArrayList<>();
	private static final RegistryKey<Registry<Class<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(Splatcraft.identifierOf("attack_id"));
	private static final SimpleRegistry<Class<?>> REGISTRY = new SimpleRegistry<>(REGISTRY_KEY, Lifecycle.stable());
	private static final Object2ObjectOpenHashMap<Identifier, Codec<? extends AttackId>> ID_TO_CODEC_MAP = new Object2ObjectOpenHashMap<>();
	private static short nextAttackId = 0;
	static
	{
		register("default", DefaultAttackId.CODEC, DefaultAttackId.class);
		register("selective", DamageSelectiveAttackId.CODEC, DamageSelectiveAttackId.class);
		register("empty", EmptyAttackId.CODEC, EmptyAttackId.class);
	}
	private static <T extends AttackId> void register(String name, Codec<? extends T> codec, Class<T> clazz)
	{
		Identifier id = Splatcraft.identifierOf(name);
		Registry.register(REGISTRY, id, clazz);
		ID_TO_CODEC_MAP.put(id, codec);
	}
	public static DefaultAttackId registerAttack()
	{
		DefaultAttackId attackId = new DefaultAttackId(assignAttackId());
		attackIdList.add(attackId);
		return attackId;
	}
	public static DamageSelectiveAttackId registerSelectiveAttack()
	{
		DamageSelectiveAttackId attackId = new DamageSelectiveAttackId(assignAttackId());
		attackIdList.add(attackId);
		return attackId;
	}
	public static short assignAttackId()
	{
		return nextAttackId++;
	}
	public static <T> AttackId parseAttackId(DynamicOps<T> ops, T input)
	{
		DataResult<Identifier> identifierDataResult = Identifier.CODEC.parse(ops, input);
		if (identifierDataResult.isError())
		{
			throw new AssertionError("Error upon reading Identifier for an AttackId.");
		}
		Codec<? extends AttackId> codec = ID_TO_CODEC_MAP.get(identifierDataResult.getOrThrow());
		if (codec == null)
		{
			throw new AssertionError("Error upon reading encoded AttackId, id doesn't match any entry.");
		}
		DataResult<? extends AttackId> attackIdDataResult = codec.parse(ops, input);
		if (attackIdDataResult.isError())
		{
			throw new AssertionError("Error upon reading encoded AttackId.");
		}
		return attackIdDataResult.getOrThrow();
	}
	public static <T, A extends AttackId> T encodeAttackId(DynamicOps<T> ops, A attackId)
	{
		Identifier id = REGISTRY.getId(attackId.getClass());
		if (id == null)
		{
			throw new AssertionError("AttackId failed encoding, invalid id.");
		}
		
		DataResult<T> identifierDataResult = Identifier.CODEC.encodeStart(ops, id);
		if (identifierDataResult.isError())
		{
			throw new AssertionError("AttackId failed encoding, error upon encoding id %s.".formatted(id));
		}
		
		DataResult<T> dataResult = attackId.getCodec().encode(attackId, ops, identifierDataResult.getPartialOrThrow());
		if (dataResult.isError())
		{
			throw new AssertionError("AttackId failed encoding, error upon encoding AttackId %s.".formatted(attackId));
		}
		
		return dataResult.getOrThrow();
	}
	public abstract short getId();
	public AttackId countProjectile()
	{
		return countProjectile(1);
	}
	public abstract AttackId countProjectile(int count);
	public abstract void projectileRemoved();
	public float getDamage(Entity entity, float damage)
	{
		return damage;
	}
	public abstract <SELF extends AttackId> Codec<SELF> getCodec();
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
					Splatcraft.LOGGER.warn("Error trying removing attack id " + id);
				}
			}
		}
	}
	public static final class DefaultAttackId extends PrimitiveImplementedAttackId
	{
		public static final Codec<DefaultAttackId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.SHORT.fieldOf("id").forGetter(DefaultAttackId::getId),
			Codec.BYTE.fieldOf("projectile_count").forGetter(DefaultAttackId::getRemainingHits),
			CodecUtils.arrayList(Uuids.CODEC).fieldOf("hit_enemies").forGetter(v -> v.hitEnemies)
		).apply(inst, DefaultAttackId::new));
		public final ObjectArrayList<UUID> hitEnemies;
		public DefaultAttackId(Short id)
		{
			super(id);
			
			hitEnemies = new ObjectArrayList<>(0);
		}
		public DefaultAttackId(short id, byte projectileCount, ObjectArrayList<UUID> hitEnemies)
		{
			super(id);
			this.projectileCount = projectileCount;
			this.hitEnemies = hitEnemies;
		}
		public float getDamage(Entity entity, float damage)
		{
			if (!hitEnemies.contains(entity.getUuid()))
			{
				hitEnemies.add(entity.getUuid());
				return damage;
			}
			return 0;
		}
		@Override
		public Codec<DefaultAttackId> getCodec()
		{
			return CODEC;
		}
	}
	public static final class EmptyAttackId extends AttackId
	{
		public static final Codec<EmptyAttackId> CODEC = Codec.unit(EmptyAttackId::new);
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
		public Codec<EmptyAttackId> getCodec()
		{
			return CODEC;
		}
	}
}
