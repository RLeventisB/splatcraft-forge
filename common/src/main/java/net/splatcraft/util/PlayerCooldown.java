package net.splatcraft.util;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.splatcraft.Splatcraft;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.items.weapons.SlosherItem;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class PlayerCooldown
{
	public static final int OVERLOAD_LIMIT = -28800;
	private static final Codec<PlayerCooldown> DEFAULT_CODEC = RecordCodecBuilder.create(inst -> inst.group(
		ItemStack.OPTIONAL_CODEC.fieldOf("stored_stack").forGetter(v -> v.storedStack),
		Codec.BOOL.optionalFieldOf("cancellable", false).forGetter(v -> v.cancellable),
		Codec.FLOAT.fieldOf("max_time").forGetter(v -> v.maxTime),
		Codec.INT.fieldOf("slot_index").forGetter(v -> v.slotIndex),
		CommonUtils.HAND_NULL_IS_MAIN_CODEC.fieldOf("used_hand").forGetter(v -> v.hand),
		Codec.BOOL.fieldOf("can_move").forGetter(v -> v.canMove),
		Codec.BOOL.fieldOf("force_crouch").forGetter(v -> v.forceCrouch),
		Codec.BOOL.fieldOf("prevent_weapon_use").forGetter(v -> v.preventWeaponUse),
		Codec.BOOL.fieldOf("is_grounded").forGetter(v -> v.isGrounded),
		Codec.FLOAT.fieldOf("time").forGetter(v -> v.time)
	).apply(inst, PlayerCooldown::new));
	public static Registry<Class<? extends PlayerCooldown>> CLASS_REGISTRY = new SimpleRegistry<>(RegistryKey.ofRegistry(Splatcraft.identifierOf("player_cooldowns")), Lifecycle.stable());
	public static Registry<Supplier<Codec<PlayerCooldown>>> CODEC_REGISTRY = new SimpleRegistry<>(RegistryKey.ofRegistry(Splatcraft.identifierOf("player_cooldowns")), Lifecycle.stable());
	public static final MapCodec<PlayerCooldown> SERIALIZER_CODEC = new MapCodec<>()
	{
		@Override
		public <T> RecordBuilder<T> encode(PlayerCooldown input, DynamicOps<T> ops, RecordBuilder<T> builder)
		{
			Identifier id = CLASS_REGISTRY.getId(input.getClass());
			
			builder.add("id", Identifier.CODEC.encodeStart(ops, id));
			builder.add("data", ops.withEncoder(CODEC_REGISTRY.get(id).get()).apply(input));
			
			return builder;
		}
		@Override
		public <T> DataResult<PlayerCooldown> decode(DynamicOps<T> ops, MapLike<T> input)
		{
			Identifier cooldownClass = Identifier.CODEC.parse(ops, input.get("id")).getOrThrow();
			return CODEC_REGISTRY.get(cooldownClass).get().parse(ops, input.get("data"));
		}
		@Override
		public <T> Stream<T> keys(DynamicOps<T> ops)
		{
			return Stream.of(ops.createString("id"), ops.createString("data"));
		}
	};
	public ItemStack storedStack;
	public boolean cancellable = false;
	float maxTime;
	int slotIndex;
	Hand hand;
	boolean canMove;
	boolean forceCrouch;
	boolean preventWeaponUse;
	boolean isGrounded;
	float time;
	public PlayerCooldown(ItemStack stack, float time, float maxTime, int slotIndex, Hand hand, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded)
	{
		storedStack = stack;
		this.time = time;
		this.maxTime = maxTime;
		this.slotIndex = slotIndex;
		this.hand = hand;
		this.canMove = canMove;
		this.forceCrouch = forceCrouch;
		this.preventWeaponUse = preventWeaponUse;
		this.isGrounded = isGrounded;
	}
	public PlayerCooldown(ItemStack stack, float time, int slotIndex, Hand hand, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded)
	{
		this(stack, time, time, slotIndex, hand, canMove, forceCrouch, preventWeaponUse, isGrounded);
	}
	public PlayerCooldown(RegistryWrapper.WrapperLookup wrapperLookup, NbtCompound nbt)
	{
		fromNbt(wrapperLookup, nbt);
	}
	public PlayerCooldown(ItemStack storedStack, boolean cancellable, float maxTime, int slotIndex, Hand hand, boolean canMove, boolean forceCrouch, boolean preventWeaponUse, boolean isGrounded, float time)
	{
		this.storedStack = storedStack;
		this.cancellable = cancellable;
		this.time = time;
		this.maxTime = maxTime;
		this.slotIndex = slotIndex;
		this.hand = hand;
		this.canMove = canMove;
		this.forceCrouch = forceCrouch;
		this.preventWeaponUse = preventWeaponUse;
		this.isGrounded = isGrounded;
	}
	public static void registerCooldowns()
	{
		register("default", PlayerCooldown.class, () -> DEFAULT_CODEC);
		register("super_jump", SuperJumpCommand.SuperJump.class, () -> SuperJumpCommand.SuperJump.CODEC);
		register("slosh_cooldown", SlosherItem.SloshCooldown.class, () -> SlosherItem.SloshCooldown.CODEC);
		register("dodge_roll_cooldown", DualieItem.DodgeRollCooldown.class, () -> DualieItem.DodgeRollCooldown.CODEC);
	}
	private static <T extends PlayerCooldown> void register(String name, Class<T> clazz, Supplier<Codec<T>> codecSupplier)
	{
		Identifier id = Splatcraft.identifierOf(name);
		Registry.register(CLASS_REGISTRY, id, clazz);
		Registry.register(CODEC_REGISTRY, id, (Supplier<Codec<PlayerCooldown>>) (Object) codecSupplier); // >:(
	}
	public static PlayerCooldown getPlayerCooldown(LivingEntity entity)
	{
		EntityInfo playerInfo = EntityInfoCapability.get(entity);
		if (playerInfo == null)
			return null;
		return playerInfo.getPlayerCooldown();
	}
	public static void setPlayerCooldown(LivingEntity player, PlayerCooldown playerCooldown)
	{
		EntityInfoCapability.get(player).setPlayerCooldown(playerCooldown);
	}
	public static PlayerCooldown setCooldownTime(LivingEntity player, int time)
	{
		PlayerCooldown cooldown = EntityInfoCapability.get(player).getPlayerCooldown();
		if (cooldown == null)
		{
			return null;
		}
		else
		{
			cooldown.setTime(time);
		}
		
		return cooldown;
	}
	public static boolean hasPlayerCooldown(LivingEntity player)
	{
		if (player == null || !EntityInfoCapability.hasCapability(player))
			return false;
		PlayerCooldown cooldown = EntityInfoCapability.get(player).getPlayerCooldown();
		return cooldown != null && cooldown.getTime() > 0;
	}
	public static boolean hasOverloadedPlayerCooldown(LivingEntity player)
	{
		if (player == null || !EntityInfoCapability.hasCapability(player))
			return false;
		PlayerCooldown cooldown = EntityInfoCapability.get(player).getPlayerCooldown();
		return cooldown != null;
	}
	public final void fromNbt(RegistryWrapper.WrapperLookup wrapperLookup, NbtCompound nbt)
	{
		storedStack = ItemStack.fromNbtOrEmpty(wrapperLookup, nbt.getCompound("StoredStack"));
		time = nbt.getFloat("Time");
		maxTime = nbt.getFloat("MaxTime");
		slotIndex = nbt.getInt("SlotIndex");
		hand = nbt.getBoolean("MainHand") ? Hand.MAIN_HAND : Hand.OFF_HAND;
		canMove = nbt.getBoolean("CanMove");
		forceCrouch = nbt.getBoolean("ForceCrouch");
		preventWeaponUse = nbt.getBoolean("PreventWeaponUse");
		isGrounded = nbt.getBoolean("IsGrounded");
	}
	public PlayerCooldown setCancellable()
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
	public boolean isGrounded()
	{
		return isGrounded;
	}
	public float getTime()
	{
		return time;
	}
	public PlayerCooldown setTime(float v)
	{
		time = v;
		return this;
	}
	public float getMaxTime()
	{
		return maxTime;
	}
	public int getSlotIndex()
	{
		return slotIndex;
	}
	public Hand getHand()
	{
		return hand;
	}
	public void setHand(Hand hand)
	{
		this.hand = hand;
	}
	public void tick(LivingEntity player)
	{
	}
	public NbtCompound writeNBT(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup)
	{
		nbt.putFloat("Time", time);
		nbt.putFloat("MaxTime", maxTime);
		nbt.putInt("SlotIndex", slotIndex);
		nbt.putBoolean("CanMove", canMove);
		nbt.putBoolean("ForceCrouch", forceCrouch);
		nbt.putBoolean("PreventWeaponUse", preventWeaponUse);
		nbt.putBoolean("IsGrounded", isGrounded);
		nbt.putBoolean("MainHand", hand.equals(Hand.MAIN_HAND));
		if (storedStack.getItem() != Items.AIR)
		{
			nbt.put("StoredStack", storedStack.encode(registryLookup, nbt));
		}
		return nbt;
	}
	public void onStart(LivingEntity player)
	{
	}
	public void onEnd(LivingEntity player)
	{
	}
}
