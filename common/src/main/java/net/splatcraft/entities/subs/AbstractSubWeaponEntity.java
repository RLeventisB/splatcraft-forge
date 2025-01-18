package net.splatcraft.entities.subs;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.splatcraft.client.particles.InkExplosionParticleData;
import net.splatcraft.entities.IColoredEntity;
import net.splatcraft.items.weapons.settings.SubWeaponRecords;
import net.splatcraft.items.weapons.settings.SubWeaponSettings;
import net.splatcraft.items.weapons.subs.SubWeaponItem;
import net.splatcraft.registries.SplatcraftDamageTypes;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSubWeaponEntity<Data extends SubWeaponRecords.SubDataRecord<Data>> extends ProjectileEntity implements IColoredEntity
{
	protected static final RegistryKey<DamageType> SPLASH_DAMAGE_TYPE = SplatcraftDamageTypes.INK_SPLAT;
	private static final TrackedData<InkColor> COLOR = DataTracker.registerData(AbstractSubWeaponEntity.class, CommonUtils.INKCOLORDATAHANDLER);
	private static final TrackedData<ItemStack> DATA_ITEM_STACK = DataTracker.registerData(AbstractSubWeaponEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
	public boolean isItem = false;
	public boolean bypassMobDamageMultiplier = false;
	public InkBlockUtils.InkType inkType;
	public ItemStack sourceWeapon = ItemStack.EMPTY;
	@Deprecated //use AbstractWeaponEntity.create
	public AbstractSubWeaponEntity(EntityType<? extends AbstractSubWeaponEntity<Data>> type, World world)
	{
		super(type, world);
	}
	public static <Data extends SubWeaponRecords.SubDataRecord<Data>, A extends AbstractSubWeaponEntity<Data>> A create(EntityType<A> type, World world, @NotNull LivingEntity thrower, ItemStack sourceWeapon)
	{
		return create(type, world, thrower, ColorUtils.getInkColor(sourceWeapon), InkBlockUtils.getInkType(thrower), sourceWeapon);
	}
	public static <Data extends SubWeaponRecords.SubDataRecord<Data>, A extends AbstractSubWeaponEntity<Data>> A create(EntityType<A> type, World world, LivingEntity thrower, InkColor color, InkBlockUtils.InkType inkType, ItemStack sourceWeapon)
	{
		A result = create(type, world, thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ(), color, inkType, sourceWeapon);
		result.setOwner(thrower);
		
		return result;
	}
	public static <Data extends SubWeaponRecords.SubDataRecord<Data>, A extends AbstractSubWeaponEntity<Data>> A create(EntityType<A> type, World world, double x, double y, double z, InkColor color, InkBlockUtils.InkType inkType, ItemStack sourceWeapon)
	{
		A result = type.create(world);
		result.setPos(x, y, z);
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
		
		if (isSubmergedInWater())
		{
			getWorld().sendEntityStatus(this, (byte) -1);
			discard();
		}
		
		HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
		if (hitResult.getType() != HitResult.Type.MISS)
		{
			hitOrDeflect(hitResult);
		}
		
		checkBlockCollision();
		
		handleMovement();
		
		updateRotation();
		float f = getFriction();
		if (f != -1)
			setVelocity(getVelocity().multiply(f));
		applyGravity();
	}
	public void handleMovement()
	{
		setPosition(getPos().add(getVelocity()));
	}
	public float getFriction()
	{
		return 0.94f;
	}
	@Override
	public void handleStatus(byte id)
	{
		super.handleStatus(id);
		
		if (id == -1)
		{
			getWorld().addParticle(new InkExplosionParticleData(getColor(), .5f), getX(), getY(), getZ(), 0, 0, 0);
		}
	}
	@Override
	public void setVelocity(Entity thrower, float pitch, float yaw, float pitchOffset, float speed, float divergence)
	{
		float f = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
		float g = -MathHelper.sin((pitch + pitchOffset) * 0.017453292F);
		float h = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
		setVelocity(f, g, h, speed, divergence);
		
		Vec3d posDiff = new Vec3d(0, 0, 0);
		
		if (thrower instanceof PlayerEntity player)
		{
			try
			{
				posDiff = thrower.getPos().subtract(player.getLerpedPos(0));
				if (thrower.isOnGround())
					posDiff.multiply(1, 0, 1);
			}
			catch (NullPointerException ignored)
			{
			}
		}
		
		refreshPositionAfterTeleport(getPos().add(posDiff));
		addVelocity(posDiff.multiply(0.8, 0.8, 0.8));
	}
	public double getGravity()
	{
		return 0.09;
	}
	public SubWeaponSettings<Data> getSettings()
	{
		if (getItem().getItem() instanceof SubWeaponItem<?> sub)
		{
			return (SubWeaponSettings<Data>) sub.getSettings(getItemRaw());
		}
		return (SubWeaponSettings<Data>) SubWeaponSettings.DEFAULT;
	}
	@Override
	public void writeCustomDataToNbt(NbtCompound nbt)
	{
		nbt.put("Color", getColor().getNbt());
		nbt.putBoolean("BypassMobDamageMultiplier", bypassMobDamageMultiplier);
		nbt.putString("InkType", inkType.getSerializedName());
		nbt.put("SourceWeapon", sourceWeapon.encode(getRegistryManager()));
		
		ItemStack itemstack = getItemRaw();
		if (!itemstack.isEmpty())
			nbt.put("Item", itemstack.encode(getWorld().getRegistryManager()));
		super.writeCustomDataToNbt(nbt);
	}
	@Override
	public void readCustomDataFromNbt(NbtCompound nbt)
	{
		if (nbt.contains("Color"))
			setColor(InkColor.getFromNbt(nbt.get("Color")));
		bypassMobDamageMultiplier = nbt.getBoolean("DypassMobDamageMultiplier");
		inkType = InkBlockUtils.InkType.IDENTIFIER_MAP.getOrDefault(Identifier.of(nbt.getString("InkType")), InkBlockUtils.InkType.NORMAL);
		sourceWeapon = ItemStack.CODEC.decode(NbtOps.INSTANCE, nbt.getCompound("SourceWeapon")).getOrThrow().getFirst();
		
		ItemStack itemstack = ItemStack.fromNbtOrEmpty(getRegistryManager(), nbt.getCompound("Item"));
		setItem(itemstack);
		super.readCustomDataFromNbt(nbt);
	}
	@Override
	protected void initDataTracker(DataTracker.Builder builder)
	{
		builder.add(DATA_ITEM_STACK, ItemStack.EMPTY);
		builder.add(COLOR, ColorUtils.getDefaultColor());
	}
	@Override
	public InkColor getColor()
	{
		return dataTracker.get(COLOR);
	}
	@Override
	public void setColor(InkColor color)
	{
		dataTracker.set(COLOR, color);
	}
	protected abstract Item getDefaultItem();
	protected ItemStack getItemRaw()
	{
		return getDataTracker().get(DATA_ITEM_STACK);
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
			getDataTracker().set(DATA_ITEM_STACK, Util.make(item.copy(), (itemStack) ->
				itemStack.setCount(1)));
		}
	}
}
