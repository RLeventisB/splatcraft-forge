package net.splatcraft.util.action.specials;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.entities.ExtraSaveData;
import net.splatcraft.entities.InkDropEntity;
import net.splatcraft.entities.InkProjectileEntity;
import net.splatcraft.items.weapons.settings.SpecialWeaponSettings;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.UpdateJumpInputPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.SplatcraftAttributes;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.*;
import net.splatcraft.util.action.ActionEndResult;
import net.splatcraft.util.action.RenderableEntityAction;
import net.splatcraft.util.structs.AttackId;
import net.splatcraft.util.structs.DamageCalculator;
import net.splatcraft.util.structs.InkColor;
import net.splatcraft.util.structs.RangedValueCollection;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Optional;

import static net.splatcraft.items.weapons.settings.SpecialWeaponRecords.InkJetDataRecord;

public class InkjetAction extends BaseSpecialAction implements RenderableEntityAction
{
	private static final ResourceLocation RECALL_ICON_TEXTURE = Splatcraft.identifierOf("textures/entity/special/inkjet_start_icon.png");
	public static final Codec<InkjetAction> CODEC = RecordCodecBuilder.create(
		inst ->
			CodecUtils.MissingProducts.and(specialCodecStart(inst),
				inst.group(
					InkJetDataRecord.CODEC.fieldOf("special_data").forGetter(v -> v.specialData),
					Codec.FLOAT.fieldOf("shot_cooldown").forGetter(v -> v.shotCooldown),
					Codec.INT.fieldOf("shot_cooldown").forGetter(v -> v.boostCooldown),
					Codec.INT.fieldOf("queued_shot_time").forGetter(v -> v.queuedShotTime),
					Codec.INT.fieldOf("queued_boost_time").forGetter(v -> v.queuedBoostTime),
					Codec.FLOAT.fieldOf("mobility").forGetter(v -> v.mobility),
					Vec3.CODEC.fieldOf("start_pos").forGetter(v -> v.startPos)
				)
			).apply(inst, InkjetAction::new)
	);
	private final InkJetDataRecord specialData;
	protected float mobility;
	protected float shotCooldown;
	protected int boostCooldown;
	protected int queuedShotTime, queuedBoostTime;
	protected boolean didBreakSound;
	protected Vec3 startPos;
	public InkjetAction(float time,
	                    float maxTime,
	                    EntitySlot weaponSlot,
	                    EntitySlot providerSlot,
	                    InkJetDataRecord specialData,
	                    float shotCooldown,
	                    int boostCooldown,
	                    int queuedShotTime,
	                    int queuedBoostTime,
	                    float mobility,
	                    Vec3 startPos
	)
	{
		super(time, maxTime, weaponSlot, providerSlot);
		this.specialData = specialData;
		this.shotCooldown = shotCooldown;
		this.boostCooldown = boostCooldown;
		this.queuedShotTime = queuedShotTime;
		this.queuedBoostTime = queuedBoostTime;
		this.mobility = mobility;
		this.startPos = startPos;
	}
	public InkjetAction(SpecialWeaponSettings<InkJetDataRecord> settings, EntitySlot weaponSlot, EntitySlot providerSlot, Vec3 startPos)
	{
		super(settings.dataRecord.specialDuration(), weaponSlot, providerSlot);
		specialData = settings.specialDataRecord;
		shotCooldown = -settings.specialDataRecord.initialStartup();
		mobility = settings.dataRecord.mobility();
		this.startPos = startPos;
	}
	@Override
	public void onStart(LivingEntity entity)
	{
		entity.level().playSound(null, entity, SplatcraftSounds.inkjetStart, SoundSource.PLAYERS, 1f, 1f);

		super.onStart(entity);
	}
	@Override
	public ActionEndResult tick(LivingEntity entity)
	{
		if (CommonUtils.isSquid(entity))
			return ActionEndResult.dontEnd(this);

		if (Components.WEAPON_INFO.getOrCreate(entity).hasHigherStartup() && shotCooldown <= 0 && shotCooldown > -8)
		{
			shotCooldown = -8;
			Components.WEAPON_INFO.get(entity).resetHigherStartup();
		}

		if (entity.isUsingItem() && !CommonUtils.isSquid(entity))
		{
			queuedShotTime = 3;
		}
		if (entity.jumping) // jumping is only updated on clients
		{
			queuedBoostTime = 3;
		}

		entity.resetFallDistance();
		entity.setYBodyRot(entity.getVisualRotationYInDegrees());
		doJetpackPhysics(entity);
		if (entity.level().isClientSide())
		{
			spawnDroplets(entity);
			// this updates the jumping field of an livingentity
			// the reason being: the server doesn't update this field lol, and we kinda need it for doing the boost sound
			SplatcraftPacketHandler.sendToServer(new UpdateJumpInputPacket(entity));
		}

		{
			float extraTime = tickShotCooldown();

			if (queuedShotTime > 0)
			{
				if (shotCooldown == 0)
					doShot(entity, extraTime);
				else
					queuedShotTime--;
			}

			tickBoost(entity);
		}

		if (getTime() <= 40 && !didBreakSound)
		{
			entity.level().playSound(null, entity, SplatcraftSounds.inkjetBreak, SoundSource.PLAYERS, 1f, 1f);
			entity.level().playSound(null, entity, SplatcraftSounds.inkjetCounter, SoundSource.PLAYERS, 0.7f, 1f);
			didBreakSound = true;
		}
		return super.tick(entity);
	}
	private void spawnDroplets(LivingEntity entity)
	{
		if (!(entity.level() instanceof ClientLevel clientLevel))
			return;
		Vec3 deltaMovement = entity.getDeltaMovement();
		for (int i = -1; i < 2; i += 2)
		{
			Vector2f jetOffset = PlayerMovementHandler.getRotatedImpulse(0.3f * i, -0.4f, entity.getYRot());
			Vec3 pos = new Vec3(entity.getX() + jetOffset.x, entity.getY(0.3), entity.getZ() + jetOffset.y);
			InkDropEntity drop = new InkDropEntity(entity.level(), pos, entity, ColorUtils.getEntityColor(entity), InkBlockUtils.getInkType(entity), 1, ItemStack.EMPTY);
			drop.shoot(deltaMovement.x / -3f, -1, deltaMovement.z / -3f, 5f, 0);
			clientLevel.addEntity(drop);
		}
	}
	private void doJetpackPhysics(LivingEntity entity)
	{
		double impulseX = entity.getDeltaMovement().x;
		double impulseY = entity.getDeltaMovement().y;
		double impulseZ = entity.getDeltaMovement().z;

		if (entity.xxa != 0 || entity.zza != 0)
		{
			float sidewaysSpeed = Math.signum(entity.xxa) * mobility;
			float forwardSpeed = Math.signum(entity.zza) * mobility;
			Vector2f rotatedImpulse = PlayerMovementHandler.getRotatedImpulse(sidewaysSpeed, forwardSpeed, entity.getYRot());
			impulseX += rotatedImpulse.x;
			impulseZ += rotatedImpulse.y;
		}

		float maxDistance = specialData.thrustData().getMaxKey();
		Vec3 startPoint = entity.position().add(entity.getDeltaMovement().scale(3f));
		Optional<Float> distanceToFloor = InkBlockUtils.getDistanceToFloor(startPoint, entity.level(), maxDistance, entity);
		float yDepth = distanceToFloor.orElse(maxDistance);
		impulseY += distanceToFloor
			.map(distance -> specialData.thrustData().getValue(distance))
			.orElseGet(() -> specialData.thrustData().getMaxValue());

		Vec3 paintPos = startPoint.add(0, -yDepth + 0.01, 0);
		InkExplosion.createInkExplosion(entity, paintPos, specialData.exhaustPaint(), RangedValueCollection.EMPTY, InkBlockUtils.getInkType(entity), ItemStack.EMPTY);

		for (Entity collidedEntity : entity.level().getEntities(entity, AABB.ofSize(entity.position().add(0, -yDepth / 2, 0), specialData.exhaustRange(), yDepth, specialData.exhaustRange())))
		{
			if (InkDamageUtils.canDamage(collidedEntity, entity))
				InkDamageUtils.doSplatDamage(collidedEntity, specialData.exhaustDamage(), entity, ItemStack.EMPTY, AttackId.NONE);
		}

		double horizontalMagnitudeSquared = impulseX * impulseX + impulseZ * impulseZ;
		float mobilitySquared = Mth.square(specialData.maxMobility());
		if (horizontalMagnitudeSquared > mobilitySquared)
		{
			double penalty = Math.max(0.75, Math.sqrt(mobilitySquared / horizontalMagnitudeSquared));
			impulseX *= penalty;
			impulseZ *= penalty;
		}

		entity.setDeltaMovement(impulseX, impulseY, impulseZ);
	}
	private void doBoost(LivingEntity entity)
	{
		if (entity.level().isClientSide())
			entity.addDeltaMovement(new Vec3(0, specialData.impulseOnJump(), 0));
		CommonUtils.setSquidDelay(entity, 5f);
		boostCooldown = specialData.impulseCooldown();
		queuedBoostTime = 0;

		entity.level().playSound(null, entity, SplatcraftSounds.inkjetBoost, SoundSource.PLAYERS, 1f, 1f);
	}
	private void doShot(LivingEntity entity, float extraTime)
	{
		InkProjectileEntity proj = new InkProjectileEntity(entity.level(),
			entity,
			ColorUtils.getEntityColor(entity),
			InkBlockUtils.getInkType(entity),
			specialData.projectile().size(),
			DamageCalculator.basic(specialData.projectile()));
		proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0f, specialData.projectileSpeed(), 0f);

		proj.setCommonProjectileStats(specialData.projectile());
		proj.explodes = true;
		proj.explodesOnExpire = false;
		proj.setProjectileType(InkProjectileEntity.Types.BLASTER);

		proj.setAttackId(AttackId.registerAttack().countProjectile());
		proj.addExtraData(new ExtraSaveData.ExplosionExtraData(specialData.blast()));
		proj.addExtraData(new ExtraSaveData.ImpactSoundExtraData(SplatcraftSounds.inkjetShotExplosion));

		entity.level().playSound(null, entity, SplatcraftSounds.inkjetShot, SoundSource.PLAYERS, 1f, 1f);
		entity.level().addFreshEntity(proj);
		proj.tick(extraTime);
		queuedShotTime = 0;
		shotCooldown = specialData.firingRepeatTicks();
	}
	private void tickBoost(LivingEntity entity)
	{
		if (boostCooldown > 0)
		{
			boostCooldown--;
		}
		if (queuedBoostTime > 0)
		{
			if (boostCooldown <= 0)
				doBoost(entity);
			else
				queuedBoostTime--;
		}
	}
	private float tickShotCooldown()
	{
		float extraTime = 0;
		if (shotCooldown > 0)
		{
			shotCooldown--;
			if (shotCooldown < 0)
			{
				extraTime = -shotCooldown;
				shotCooldown = 0;
			}
		}
		else if (shotCooldown < 0) // only accessible on startup
		{
			shotCooldown++;
			if (shotCooldown > 0)
			{
				extraTime = shotCooldown;
				shotCooldown = 0;
			}
		}
		return extraTime;
	}
	@Override
	public boolean isCancellable(LivingEntity entity)
	{
		return true;
	}
	@Override
	public Optional<Float> mobility(LivingEntity entity)
	{
		if (CommonUtils.isSquid(entity))
			return Optional.empty();

		return Optional.of(0f);
	}
	@Override
	public ActionEndResult canEnd(LivingEntity entity, EndType endType)
	{
		if (entity.level().isClientSide())
		{
			SplatcraftPacketHandler.sendToServer(new UpdateJumpInputPacket(false));
			return ActionEndResult.dontEnd(this);
		}

		if (endType == EndType.CANCELLED)
			return ActionEndResult.dontEnd(this);

		entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.inkjetReturn, SoundSource.PLAYERS, 1f, 1f);
		entity.jumping = false;

		return ActionEndResult.dontEndWithSync(new SuperJumpCommand.SuperJump(entity.position(),
			startPos,
			0,
			specialData.recallTime(),
			entity.getAttributeValue(SplatcraftAttributes.superJumpHeight),
			entity.noPhysics,
			entity instanceof Player player && player.getAbilities().invulnerable,
			false, true, SuperJumpCommand.SuperJump.JETPACK_ICON));
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void renderExtra(@NotNull PoseStack poseStack, @NotNull MultiBufferSource provider, LivingEntity entity, float partialTicks)
	{
		Quaternionf quaternion = new Quaternionf();
		Camera camera = ClientUtils.getClient().gameRenderer.getMainCamera();
		SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ.setRotation(quaternion, camera, partialTicks);
		InkColor entityColor = ColorUtils.getEntityColor(entity);
		if (entityColor.isInvalid())
			return;

		int color = ColorUtils.makeBrighter(entityColor);
		final float size = 0.4f;

		VertexConsumer consumer = provider.getBuffer(RenderType.armorCutoutNoCull(RECALL_ICON_TEXTURE));
		Vector3f quadCenter = startPos.toVector3f().add(0, 0.5f, 0).sub(camera.getPosition().toVector3f());

		consumer.addVertex((new Vector3f(size, size, 0f)).rotate(quaternion).add(quadCenter)).setColor(color).setUv(0, 0).setUv1(0, 0).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
		consumer.addVertex((new Vector3f(size, -size, 0f)).rotate(quaternion).add(quadCenter)).setColor(color).setUv(0, 1).setUv1(0, 16).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
		consumer.addVertex((new Vector3f(-size, -size, 0f)).rotate(quaternion).add(quadCenter)).setColor(color).setUv(1, 1).setUv1(16, 16).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
		consumer.addVertex((new Vector3f(-size, size, 0f)).rotate(quaternion).add(quadCenter)).setColor(color).setUv(1, 0).setUv1(16, 0).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
	}
}
