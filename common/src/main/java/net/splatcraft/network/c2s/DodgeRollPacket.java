package net.splatcraft.network.c2s;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.items.weapons.DualieItem;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DodgeRollPacket extends PlayC2SPacket
{
	public static final Type<? extends CustomPacketPayload> ID = new Type<>(Splatcraft.identifierOf("dodge_roll_packet"));
	UUID target;
	ItemStack activeDualie;
	Vec2 rollPotency;
	EntitySlot dualieSlot;
	public DodgeRollPacket(UUID target, ItemStack activeDualie, EntitySlot dualieSlot, Vec2 rollPotency)
	{
		this.target = target;
		this.activeDualie = activeDualie;
		this.dualieSlot = dualieSlot;
		this.rollPotency = rollPotency;
	}
	public static DodgeRollPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new DodgeRollPacket(
			buffer.readUUID(),
			ItemStack.STREAM_CODEC.decode(buffer),
			EntitySlot.SERIALIZER_STREAM_CODEC.decode(buffer),
			new Vec2(buffer.readFloat(), buffer.readFloat())
		);
	}
	@Override
	public void execute(ServerPlayer player)
	{
		Player target = player.level().getPlayerByUUID(this.target);
		((DualieItem) activeDualie.getItem()).performRoll(target, activeDualie, dualieSlot, rollPotency);
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeUUID(target);
		ItemStack.STREAM_CODEC.encode(buffer, activeDualie);
		EntitySlot.SERIALIZER_STREAM_CODEC.encode(buffer, dualieSlot);
		buffer.writeFloat(rollPotency.x); // important note dont use writeDouble so your rollDirection.x isnt't 3.16345E19 (god damn it minecraft why did you make it so Vec2 uses floats but Vec3d uses doubles)
		buffer.writeFloat(rollPotency.y);
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}