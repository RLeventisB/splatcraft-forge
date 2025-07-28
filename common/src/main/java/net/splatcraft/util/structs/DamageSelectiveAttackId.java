package net.splatcraft.util.structs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import net.splatcraft.util.CodecUtils;

import java.util.UUID;

public class DamageSelectiveAttackId extends AttackId.PrimitiveImplementedAttackId
{
	public static final MapCodec<DamageSelectiveAttackId> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
		Codec.SHORT.fieldOf("id").forGetter(DamageSelectiveAttackId::getId),
		Codec.BYTE.fieldOf("projectile_count").forGetter(DamageSelectiveAttackId::getRemainingHits),
		CodecUtils.hashMapCodec(UUIDUtil.AUTHLIB_CODEC, Codec.FLOAT).fieldOf("hit_enemies").forGetter(v -> v.enemyToDamageDoneData)
	).apply(inst, DamageSelectiveAttackId::new));
	public final Object2ObjectMap<UUID, Float> enemyToDamageDoneData;
	public DamageSelectiveAttackId(Short id)
	{
		super(id);
		
		enemyToDamageDoneData = new Object2ObjectOpenHashMap<>();
	}
	public DamageSelectiveAttackId(Short id, int expected)
	{
		super(id);
		
		enemyToDamageDoneData = new Object2ObjectOpenHashMap<>(expected);
	}
	public DamageSelectiveAttackId(short id, byte projectileCount, Object2ObjectMap<UUID, Float> enemyToDamageDoneData)
	{
		super(id);
		this.projectileCount = projectileCount;
		this.enemyToDamageDoneData = enemyToDamageDoneData;
	}
	@Override
	public float getDamage(Entity entity, float damage)
	{
		Float lastDamage = enemyToDamageDoneData.get(entity.getUUID());
		enemyToDamageDoneData.put(entity.getUUID(), damage);
		if (lastDamage != null)
		{
			return Math.max(0, damage - lastDamage);
		}
		return damage;
	}
	@Override
	public MapCodec<DamageSelectiveAttackId> getCodec()
	{
		return CODEC;
	}
}
