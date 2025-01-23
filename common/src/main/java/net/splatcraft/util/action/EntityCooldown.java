package net.splatcraft.util.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.splatcraft.util.CommonUtils;

public class EntityCooldown extends EntityActionWithTime
{
	public static final int OVERLOAD_LIMIT = -28800;
	public static final Codec<EntityCooldown> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		ItemStack.OPTIONAL_CODEC.fieldOf("stored_stack").forGetter(v -> v.storedStack),
		Codec.BOOL.optionalFieldOf("cancellable", false).forGetter(v -> v.cancellable),
		getMaxTimeCodec(),
		Codec.INT.fieldOf("slot_index").forGetter(v -> v.slotIndex),
		CommonUtils.HAND_NULL_IS_MAIN_CODEC.fieldOf("used_hand").forGetter(v -> v.hand),
		Codec.BOOL.fieldOf("can_move").forGetter(v -> v.canMove),
		Codec.BOOL.fieldOf("force_crouch").forGetter(v -> v.forceCrouch),
		Codec.BOOL.fieldOf("prevent_weapon_use").forGetter(v -> v.preventWeaponUse),
		Codec.BOOL.fieldOf("is_grounded").forGetter(v -> v.isGrounded),
		getTimeCodec(),
		Codec.BOOL.fieldOf("prevent_stop_using").forGetter(v -> v.preventStopUsing)
	).apply(inst, EntityCooldown::new));
	final boolean forceCrouch;
	final boolean preventWeaponUse;
	final boolean isGrounded;
	final boolean preventStopUsing;
	final int slotIndex;
	final Hand hand;
	final boolean canMove;
	public ItemStack storedStack;
	public boolean cancellable = false;
	public EntityCooldown(ItemStack stack, float time, float maxTime, int slotIndex, Hand hand, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded)
	{
		super(time, maxTime);
		storedStack = stack;
		this.slotIndex = slotIndex;
		this.hand = hand;
		this.canMove = canMove;
		this.forceCrouch = forceCrouch;
		this.preventWeaponUse = preventWeaponUse;
		this.isGrounded = isGrounded;
		preventStopUsing = false;
	}
	public EntityCooldown(ItemStack stack, float time, int slotIndex, Hand hand, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded)
	{
		this(stack, time, time, slotIndex, hand, canMove, forceCrouch, preventWeaponUse, isGrounded);
	}
	public EntityCooldown(ItemStack storedStack, boolean cancellable, float maxTime, int slotIndex, Hand hand, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded, float time, boolean preventStopUsing)
	{
		super(time, maxTime);
		this.storedStack = storedStack;
		this.cancellable = cancellable;
		this.slotIndex = slotIndex;
		this.hand = hand;
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
	public boolean isGrounded()
	{
		return isGrounded;
	}
	public int getSlotIndex()
	{
		return slotIndex;
	}
	public Hand getHand()
	{
		return hand;
	}
}
