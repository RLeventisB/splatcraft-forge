package net.splatcraft.network.s2c;

import com.mojang.serialization.DataResult;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.action.EntityAction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class UpdateEntityActionOnlyPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateEntityActionOnlyPacket.class);
	UUID target;
	Tag nbt;
	protected UpdateEntityActionOnlyPacket(UUID player, Tag nbt)
	{
		target = player;
		this.nbt = nbt;
	}
	public UpdateEntityActionOnlyPacket(LivingEntity target)
	{
		this(target.getUUID(), EntityAction.SERIALIZER_CODEC.encodeStart(NbtOps.INSTANCE, EntityInfoCapability.get(target).getEntityAction()).getOrThrow());
	}
	public static UpdateEntityActionOnlyPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new UpdateEntityActionOnlyPacket(buffer.readUUID(), buffer.readNbt());
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
			DataResult<EntityAction> result = EntityAction.SERIALIZER_CODEC.parse(NbtOps.INSTANCE, nbt);
			if (result.isSuccess())
			{
				EntityAction entityAction = result.getOrThrow();
				EntityAction.setEntityAction(target, entityAction);
			}
		}
	}
}
