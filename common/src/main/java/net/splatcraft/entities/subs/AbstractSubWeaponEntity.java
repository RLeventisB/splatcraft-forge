package net.splatcraft.entities.subs;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.entities.ISetVelocityExtension;
import net.splatcraft.items.weapons.settings.DynamicDataRecord;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSubWeaponEntity<Data extends DynamicDataRecord<Data>> extends Projectile implements IColoredEntity, ISetVelocityExtension
{
	protected static final ResourceKey<DamageType> SPLASH_DAMAGE_TYPE = SplatcraftDamageTypes.INK_SPLAT;
	private static final EntityDataAccessor<InkColor> COLOR = SynchedEntityData.defineId(AbstractSubWeaponEntity.class, CommonUtils.INKCOLORDATAHANDLER);
	private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(AbstractSubWeaponEntity.class, EntityDataSerializers.ITEM_STACK);
	public boolean isItem = false;
	public boolean bypassMobDamageMultiplier = false;
	public InkBlockUtils.InkType inkType;
	public ItemStack sourceWeapon = ItemStack.EMPTY;
	@Deprecated //use AbstractWeaponEntity.create
	public AbstractSubWeaponEntity(EntityType<? extends AbstractSubWeaponEntity<Data>> type, Level world)
	{
		super(type, world);
	}
	public static <Data extends DynamicDataRecord<Data>, A extends AbstractSubWeaponEntity<Data>> A create(EntityType<A> type, Level world, @NotNull LivingEntity thrower, ItemStack sourceWeapon)
	{
		return create(type, world, thrower, ColorUtils.getInkColor(sourceWeapon), InkBlockUtils.getInkType(thrower), sourceWeapon);
	}
	public static <Data extends DynamicDataRecord<Data>, A extends AbstractSubWeaponEntity<Data>> A create(EntityType<A> type, Level world, LivingEntity thrower, InkColor color, InkBlockUtils.InkType inkType, ItemStack sourceWeapon)
	{
		A result = create(type, world, thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ(), color, inkType, sourceWeapon);
		result.setOwner(thrower);

		return result;
	}
	public static <Data extends DynamicDataRecord<Data>, A extends AbstractSubWeaponEntity<Data>> A create(EntityType<A> type, Level world, double x, double y, double z, InkColor color, InkBlockUtils.InkType inkType, ItemStack sourceWeapon)
	{
		A result = type.create(world);
		result.setPosRaw(x, y, z);
		result.setColor(color);
		result.inkType = inkType;
		result.sourceWeapon = sourceWeapon;
		result.setItem(sourceWeapon);

		return result;
	}
	@Override
	public void tick()
	{
		super.tick();

		if (isUnderWater())
		{
			level().broadcastEntityEvent(this, (byte) -1);
			discard();
		}

		HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (hitResult.getType() != HitResult.Type.MISS)
		{
			hitTargetOrDeflectSelf(hitResult);
		}

		checkInsideBlocks();

		handleMovement();

		updateRotation();
		float f = getFriction();
		if (f != -1)
			setDeltaMovement(getDeltaMovement().scale(f));
		applyGravity();
	}
	public void handleMovement()
	{
		setPos(position().add(getDeltaMovement()));
	}
	public float getFriction()
	{
		return 0.94f;
	}
	@Override
	public void handleEntityEvent(byte id)
	{
		super.handleEntityEvent(id);

		if (id == -1)
		{
			level().addParticle(new InkExplosionParticleData(getColor(), .5f), getX(), getY(), getZ(), 0, 0, 0);
		}
	}
	@Override
	public void shootFromRotation(@NotNull Entity shooter, float pitch, float yaw, float roll, float speed, float divergence)
	{
		ISetVelocityExtension.super.setDeltaMovement(shooter, pitch, yaw, roll, speed, divergence);
	}
	@Override
	public void shoot(double x, double y, double z, float power, float uncertainty)
	{
		ISetVelocityExtension.super.setDeltaMovement(x, y, z, power, uncertainty);
	}
	@Override
	public void onVelocityCalculated(Vec3 velocity, float speed)
	{
		setDeltaMovement(velocity);
	}
	public double getDefaultGravity()
	{
		return 0.09;
	}
	public SubWeaponSettings<Data> getSettings()
	{
		if (getItem().getItem() instanceof SubWeaponItem<?> sub)
		{
			return (SubWeaponSettings<Data>) sub.getSettings(getItem());
		}
		return (SubWeaponSettings<Data>) SubWeaponSettings.DEFAULT;
	}
	@Override
	public void addAdditionalSaveData(CompoundTag nbt)
	{
		nbt.put("Color", getColor().getNbt());
		nbt.putBoolean("BypassMobDamageMultiplier", bypassMobDamageMultiplier);
		nbt.putString("InkType", inkType.getIdString());
		nbt.put("SourceWeapon", sourceWeapon.save(registryAccess()));

		ItemStack itemstack = getItemRaw();
		if (!itemstack.isEmpty())
			nbt.put("Item", itemstack.save(level().registryAccess()));
		super.addAdditionalSaveData(nbt);
	}
	@Override
	public void readAdditionalSaveData(CompoundTag nbt)
	{
		if (nbt.contains("Color"))
			setColor(InkColor.getFromNbt(nbt.get("Color")));
		bypassMobDamageMultiplier = nbt.getBoolean("DypassMobDamageMultiplier");
		inkType = InkBlockUtils.InkType.IDENTIFIER_MAP.getOrDefault(ResourceLocation.parse(nbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);
		sourceWeapon = ItemStack.CODEC.decode(NbtOps.INSTANCE, nbt.getCompound("SourceWeapon")).getOrThrow().getFirst();

		ItemStack itemstack = ItemStack.parseOptional(registryAccess(), nbt.getCompound("Item"));
		setItem(itemstack);
		super.readAdditionalSaveData(nbt);
	}
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(DATA_ITEM_STACK, new ItemStack(getDefaultItem()));
		builder.define(COLOR, ColorUtils.getDefaultColor());
	}
	@Override
	public InkColor getColor()
	{
		return entityData.get(COLOR);
	}
	@Override
	public void setColor(InkColor color)
	{
		entityData.set(COLOR, color);
	}
	protected abstract Item getDefaultItem();
	protected ItemStack getItemRaw()
	{
		return getEntityData().get(DATA_ITEM_STACK);
	}
	public ItemStack getItem()
	{
		ItemStack itemstack = getItemRaw();
		return itemstack.isEmpty() ? new ItemStack(getDefaultItem()) : itemstack;
	}
	public void setItem(ItemStack item)
	{
		if (item.getItem() != getDefaultItem())
		{
			getEntityData().set(DATA_ITEM_STACK, Util.make(item.copy(), (itemStack) ->
				itemStack.setCount(1)));
		}
	}
}
