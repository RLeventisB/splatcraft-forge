package net.splatcraft.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.platform.Components;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class UpdateSquidRollDataPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateSquidRollDataPacket.class);
	private final UUID uuid;
	private final byte rollLeniencyTime;
	private final byte rollRepeatTime;
	private final boolean isRollRepeated;
	public UpdateSquidRollDataPacket(@NotNull LivingEntity entity, byte rollLeniencyTime, byte rollRepeatTime, boolean isRollRepeated)
	{
		this(entity.getUUID(), rollLeniencyTime, rollRepeatTime, isRollRepeated);
	}
	public UpdateSquidRollDataPacket(@NotNull UUID uuid, byte rollLeniencyTime, byte rollRepeatTime, boolean isRollRepeated)
	{
		this.uuid = uuid;
		this.rollLeniencyTime = rollLeniencyTime;
		this.rollRepeatTime = rollRepeatTime;
		this.isRollRepeated = isRollRepeated;
	}
	public static UpdateSquidRollDataPacket decode(FriendlyByteBuf buf)
	{
		return new UpdateSquidRollDataPacket(buf.readUUID(), buf.readByte(), buf.readByte(), buf.readBoolean());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeUUID(uuid);
		buffer.writeByte(rollLeniencyTime);
		buffer.writeByte(rollRepeatTime);
		buffer.writeBoolean(isRollRepeated);
	}
	@Override
	public void execute()
	{
		Player player = ClientUtils.getClientPlayer().level().getPlayerByUUID(uuid);
		if (player != null)
		{
			Components.SQUID_INFO.updateOrCreate(player, info -> info.setRollLeniencyTime(rollLeniencyTime).setRollRepeatPunishmentTime(rollRepeatTime).setIsRollRepeated(isRollRepeated));
		}
	}
}
