package net.splatcraft.network.s2c;

import com.mojang.serialization.DataResult;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class UpdateEntityInfoPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateEntityInfoPacket.class);
	UUID target;
	Tag nbt;
	protected UpdateEntityInfoPacket(UUID player, Tag nbt)
	{
		target = player;
		this.nbt = nbt;
	}
	public UpdateEntityInfoPacket(Player target)
	{
		this(target.getUUID(), EntityInfo.CODEC.encodeStart(NbtOps.INSTANCE, EntityInfoCapability.get(target)).getOrThrow());
	}
	public static UpdateEntityInfoPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new UpdateEntityInfoPacket(buffer.readUUID(), buffer.readNbt());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeUUID(target);
		buffer.writeNbt(nbt);
	}
	@Override
	public void execute()
	{
		Player target = Minecraft.getInstance().level.getPlayerByUUID(this.target);
		
		if (target != null)
		{
			DataResult<EntityInfo> result = EntityInfo.CODEC.parse(NbtOps.INSTANCE, nbt);
			if (result.isSuccess())
			{
				EntityInfo entityInfo = result.getOrThrow();
				EntityInfoCapability.set(target, entityInfo);
				ClientUtils.setClientPlayerColor(this.target, entityInfo.getColor());
			}
		}
	}
}
