package net.splatcraft.network.s2c;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;

public class SendColorRegistryPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendColorRegistryPacket.class);
	private static final StreamCodec<RegistryFriendlyByteBuf, BiMap<ResourceLocation, InkColor>> PACKET_CODEC = ByteBufCodecs.map(HashBiMap::create, ResourceLocation.STREAM_CODEC, InkColor.PACKET_CODEC);
	private final BiMap<ResourceLocation, InkColor> colors;
	public SendColorRegistryPacket(BiMap<ResourceLocation, InkColor> colors)
	{
		this.colors = colors;
	}
	public static SendColorRegistryPacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new SendColorRegistryPacket(PACKET_CODEC.decode(buffer));
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		PACKET_CODEC.encode(buffer, colors);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		InkColorRegistry.REGISTRY.clear();
		InkColorRegistry.REGISTRY.putAll(colors);
	}
}