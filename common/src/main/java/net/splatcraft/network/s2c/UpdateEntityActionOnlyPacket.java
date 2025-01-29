package net.splatcraft.network.s2c;

import com.mojang.serialization.DataResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.action.EntityAction;

import java.util.UUID;

public class UpdateEntityActionOnlyPacket extends PlayS2CPacket
{
	public static final Id<? extends CustomPayload> ID = CommonUtils.createIdFromClass(UpdateEntityActionOnlyPacket.class);
	UUID target;
	NbtElement nbt;
	protected UpdateEntityActionOnlyPacket(UUID player, NbtElement nbt)
	{
		target = player;
		this.nbt = nbt;
	}
	public UpdateEntityActionOnlyPacket(LivingEntity target)
	{
		this(target.getUuid(), EntityAction.SERIALIZER_CODEC.encodeStart(NbtOps.INSTANCE, EntityInfoCapability.get(target).getEntityAction()).getOrThrow());
	}
	public static UpdateEntityActionOnlyPacket decode(RegistryByteBuf buffer)
	{
		return new UpdateEntityActionOnlyPacket(buffer.readUuid(), buffer.readNbt());
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
			DataResult<EntityAction> result = EntityAction.SERIALIZER_CODEC.parse(NbtOps.INSTANCE, nbt);
			if (result.isSuccess())
			{
				EntityAction entityAction = result.getOrThrow();
				EntityAction.setEntityAction(target, entityAction);
			}
		}
	}
}
