package net.splatcraft.network.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.platform.Components;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CodecUtils;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.UUID;

public class UpdateSquidRollPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateSquidRollPacket.class);
	private final UUID uuid;
	private final Vector2f inputDirection;
	private final boolean rollRepeated;
	public UpdateSquidRollPacket(@NotNull LivingEntity entity, Vector2f inputDirection, boolean rollRepeated)
	{
		this(entity.getUUID(), inputDirection, rollRepeated);
	}
	public UpdateSquidRollPacket(@NotNull UUID uuid, Vector2f inputDirection, boolean rollRepeated)
	{
		this.uuid = uuid;
		this.inputDirection = inputDirection;
		this.rollRepeated = rollRepeated;
	}
	public static UpdateSquidRollPacket decode(FriendlyByteBuf buf)
	{
		return new UpdateSquidRollPacket(buf.readUUID(), CodecUtils.Codecs.VECTOR2F_STREAM_CODEC.decode(buf), buf.readBoolean());
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
		CodecUtils.Codecs.VECTOR2F_STREAM_CODEC.encode(buffer, inputDirection);
		buffer.writeBoolean(rollRepeated);
	}
	@Override
	public void execute()
	{
		Player player = ClientUtils.getClientPlayer().level().getPlayerByUUID(uuid);
		if (player != null)
		{
			Components.SQUID_INFO.updateOrCreate(player, info -> info.doSquidRoll(player, inputDirection, true));
		}
	}
}
