package net.splatcraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.util.Uuids;

import java.util.UUID;

public class DamageSelectiveAttackId extends AttackId.PrimitiveImplementedAttackId
{
	public static final Codec<DamageSelectiveAttackId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		Codec.SHORT.fieldOf("id").forGetter(DamageSelectiveAttackId::getId),
		Codec.BYTE.fieldOf("projectile_count").forGetter(DamageSelectiveAttackId::getRemainingHits),
		CodecUtils.hashMapCodec(Uuids.CODEC, Codec.FLOAT).fieldOf("hit_enemies").forGetter(v -> v.enemyToDamageDoneData)
	).apply(inst, DamageSelectiveAttackId::new));
	public final Object2ObjectOpenHashMap<UUID, Float> enemyToDamageDoneData;
	public DamageSelectiveAttackId(Short id)
	{
		super(id);
		
		enemyToDamageDoneData = new Object2ObjectOpenHashMap<>();
	}
	public DamageSelectiveAttackId(short id, byte projectileCount, Object2ObjectOpenHashMap<UUID, Float> enemyToDamageDoneData)
	{
		super(id);
		this.projectileCount = projectileCount;
		this.enemyToDamageDoneData = enemyToDamageDoneData;
	}
	@Override
	public float getDamage(Entity entity, float damage)
	{
		Float lastDamage = enemyToDamageDoneData.get(entity.getUuid());
		if (lastDamage != null)
		{
			return Math.max(0, damage - lastDamage);
		}
		return damage;
	}
	@Override
	public Codec<DamageSelectiveAttackId> getCodec()
	{
		return CODEC;
	}
}
