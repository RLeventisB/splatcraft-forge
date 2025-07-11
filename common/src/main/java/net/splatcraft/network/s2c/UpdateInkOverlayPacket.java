package net.splatcraft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.platform.Components;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class UpdateInkOverlayPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(UpdateInkOverlayPacket.class);
	int entityId;
	CompoundTag nbt;
	public UpdateInkOverlayPacket(LivingEntity entity, InkOverlayInfo info)
	{
		this(entity.getId(), info.writeNBT(new CompoundTag()));
	}
	public UpdateInkOverlayPacket(int entity, CompoundTag info)
	{
		entityId = entity;
		nbt = info;
	}
	public static UpdateInkOverlayPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new UpdateInkOverlayPacket(buffer.readInt(), buffer.readNbt());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void execute()
	{
		Entity entity = Minecraft.getInstance().level.getEntity(entityId);
		
		if (!(entity instanceof LivingEntity living))
		{
			return;
		}
		Components.INK_OVERLAY.set(living, InkOverlayInfo.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow());
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeInt(entityId);
		buffer.writeNbt(nbt);
	}
}
