package net.splatcraft.util.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.util.CodecUtils;

public class EntityCooldown extends EntityActionWithTime
{
	public static final int OVERLOAD_LIMIT = -28800;
	public static final Codec<EntityCooldown> CODEC = RecordCodecBuilder.create(
		inst ->
			CodecUtils.MissingProducts.and(
				codecStart(inst),
				inst.group(
					ItemStack.OPTIONAL_CODEC.fieldOf("stored_stack").forGetter(v -> v.storedStack),
					Codec.BOOL.optionalFieldOf("cancellable", false).forGetter(v -> v.cancellable),
					EntitySlot.SERIALIZER_CODEC.fieldOf("item_slot").forGetter(v -> v.itemSlot),
					Codec.BOOL.fieldOf("can_move").forGetter(v -> v.canMove),
					Codec.BOOL.fieldOf("force_crouch").<EntityCooldown>forGetter(v -> v.forceCrouch),
					Codec.BOOL.fieldOf("prevent_weapon_use").<EntityCooldown>forGetter(v -> v.preventWeaponUse),
					Codec.BOOL.fieldOf("is_grounded").<EntityCooldown>forGetter(v -> v.isGrounded),
					Codec.BOOL.fieldOf("prevent_stop_using").<EntityCooldown>forGetter(v -> v.preventStopUsing)
				)
			).apply(inst, EntityCooldown::new)
	);
	final boolean forceCrouch;
	final boolean preventWeaponUse;
	final boolean isGrounded;
	final boolean preventStopUsing;
	final EntitySlot itemSlot;
	final boolean canMove;
	public ItemStack storedStack;
	public boolean cancellable = false;
	public EntityCooldown(ItemStack stack, float time, float maxTime, EntitySlot itemSlot, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded)
	{
		super(time, maxTime);
		storedStack = stack;
		this.itemSlot = itemSlot;
		this.canMove = canMove;
		this.forceCrouch = forceCrouch;
		this.preventWeaponUse = preventWeaponUse;
		this.isGrounded = isGrounded;
		preventStopUsing = false;
	}
	public EntityCooldown(ItemStack stack, float time, EntitySlot itemSlot, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded)
	{
		this(stack, time, time, itemSlot, canMove, forceCrouch, preventWeaponUse, isGrounded);
	}
	public EntityCooldown(float time, float maxTime, ItemStack storedStack, boolean cancellable, EntitySlot itemSlot, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded, boolean preventStopUsing)
	{
		super(time, maxTime);
		this.storedStack = storedStack;
		this.cancellable = cancellable;
		this.itemSlot = itemSlot;
		this.canMove = canMove;
		this.forceCrouch = forceCrouch;
		this.preventWeaponUse = preventWeaponUse;
		this.isGrounded = isGrounded;
		this.preventStopUsing = preventStopUsing;
	}
	public boolean isCancellable()
	{
		return cancellable;
	}
	public EntityAction setCancellable()
	{
		cancellable = true;
		return this;
	}
	public boolean canMove()
	{
		return canMove;
	}
	public boolean forceCrouch()
	{
		return forceCrouch;
	}
	public boolean preventWeaponUse()
	{
		return preventWeaponUse;
	}
	public boolean preventStopUsing()
	{
		return preventStopUsing;
	}
	public EntitySlot getItemSlot()
	{
		return itemSlot;
	}
}
