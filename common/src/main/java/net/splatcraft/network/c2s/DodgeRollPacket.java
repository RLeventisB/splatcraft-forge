package net.splatcraft.network.c2s;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec2f;
import net.splatcraft.Splatcraft;
import net.splatcraft.items.weapons.DualieItem;

import java.util.UUID;

public class DodgeRollPacket extends PlayC2SPacket
{
	public static final Id<? extends CustomPayload> ID = new Id<>(Splatcraft.identifierOf("dodge_roll_packet"));
	UUID target;
	ItemStack activeDualie;
	int maxRolls;
	Vec2f rollPotency;
	Hand hand;
	public DodgeRollPacket(UUID target, ItemStack activeDualie, Hand hand, int maxRolls, Vec2f rollPotency)
	{
		this.target = target;
		this.activeDualie = activeDualie;
		this.maxRolls = maxRolls;
		this.rollPotency = rollPotency;
		this.hand = hand;
	}
	public static DodgeRollPacket decode(RegistryByteBuf buffer)
	{
		return new DodgeRollPacket(buffer.readUuid(), ItemStack.PACKET_CODEC.decode(buffer), buffer.readBoolean() ? Hand.OFF_HAND : Hand.MAIN_HAND, buffer.readInt(), new Vec2f(buffer.readFloat(), buffer.readFloat()));
	}
	@Override
	public void execute(PlayerEntity player)
	{
		PlayerEntity target = player.getWorld().getPlayerByUuid(this.target);
		((DualieItem) activeDualie.getItem()).performRoll(target, activeDualie, hand, maxRolls, rollPotency, false);
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		buffer.writeUuid(target);
		ItemStack.PACKET_CODEC.encode(buffer, activeDualie);
		buffer.writeRegistryKey(activeDualie.getRegistryEntry().getKey().get());
		buffer.writeBoolean(hand == Hand.OFF_HAND);
		buffer.writeInt(maxRolls);
		buffer.writeFloat(rollPotency.x); // important note dont use writeDouble so your rollDirection.x isnt't 3.16345E19 (god damn it minecraft why did you make it so Vec2 uses floats but Vec3d uses doubles)
		buffer.writeFloat(rollPotency.y);
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
}