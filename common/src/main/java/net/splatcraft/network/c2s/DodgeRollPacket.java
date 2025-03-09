package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.splatcraft.Splatcraft;
import net.splatcraft.items.weapons.DualieItem;

import java.util.UUID;

public class DodgeRollPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = new Type<>(Splatcraft.identifierOf("dodge_roll_packet"));
	UUID target;
	ItemStack activeDualie;
	int maxRolls;
	Vec2 rollPotency;
	InteractionHand hand;
	public DodgeRollPacket(UUID target, ItemStack activeDualie, InteractionHand hand, int maxRolls, Vec2 rollPotency)
	{
		this.target = target;
		this.activeDualie = activeDualie;
		this.maxRolls = maxRolls;
		this.rollPotency = rollPotency;
		this.hand = hand;
	}
	public static DodgeRollPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new DodgeRollPacket(buffer.readUUID(), ItemStack.STREAM_CODEC.decode(buffer), buffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, buffer.readInt(), new Vec2(buffer.readFloat(), buffer.readFloat()));
	}
	@Override
	public void execute(Player player)
	{
		Player target = player.level().getPlayerByUUID(this.target);
		((DualieItem) activeDualie.getItem()).performRoll(target, activeDualie, hand, maxRolls, rollPotency, false);
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeUUID(target);
		ItemStack.STREAM_CODEC.encode(buffer, activeDualie);
		buffer.writeBoolean(hand == InteractionHand.OFF_HAND);
		buffer.writeInt(maxRolls);
		buffer.writeFloat(rollPotency.x); // important note dont use writeDouble so your rollDirection.x isnt't 3.16345E19 (god damn it minecraft why did you make it so Vec2 uses floats but Vec3d uses doubles)
		buffer.writeFloat(rollPotency.y);
	}
	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}