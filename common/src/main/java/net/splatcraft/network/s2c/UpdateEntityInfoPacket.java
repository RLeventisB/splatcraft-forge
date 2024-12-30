package net.splatcraft.network.s2c;

import com.mojang.serialization.DataResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;

import java.util.UUID;

public class UpdateEntityInfoPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateEntityInfoPacket.class);
	UUID target;
	NbtCompound nbt;
	protected UpdateEntityInfoPacket(UUID player, NbtCompound nbt)
	{
		target = player;
		this.nbt = nbt;
	}
	public UpdateEntityInfoPacket(PlayerEntity target)
	{
		this(target.getUuid(), (NbtCompound) EntityInfo.CODEC.encodeStart(NbtOps.INSTANCE, EntityInfoCapability.get(target)).getOrThrow());
	}
	public static UpdateEntityInfoPacket decode(RegistryByteBuf buffer)
	{
		return new UpdateEntityInfoPacket(buffer.readUuid(), buffer.readNbt());
	}
	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
	@Override
	public void encode(RegistryByteBuf buffer)
	{
		buffer.writeUuid(target);
		buffer.writeNbt(nbt);
	}
	@Override
	public void execute()
	{
		PlayerEntity target = MinecraftClient.getInstance().world.getPlayerByUuid(this.target);
		
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
