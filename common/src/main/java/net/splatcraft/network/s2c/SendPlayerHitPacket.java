package net.splatcraft.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.particles.InkHitParticleData;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SendPlayerHitPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendPlayerHitPacket.class);
	private static final StreamCodec<ByteBuf, Optional<SoundEvent>> SOUND_STREAM_CODEC = ByteBufCodecs.optional(SoundEvent.DIRECT_STREAM_CODEC);
	private final Vec3 impactPos;
	private final Optional<SoundEvent> soundOptional;
	private final float scale;
	public SendPlayerHitPacket(@NotNull Vec3 impactPos, SoundEvent soundEvent, float scale)
	{
		this.impactPos = impactPos;
		soundOptional = Optional.ofNullable(soundEvent);
		this.scale = scale;
	}
	public SendPlayerHitPacket(@NotNull Vec3 impactPos, Optional<SoundEvent> soundOptional, float scale)
	{
		this.impactPos = impactPos;
		this.soundOptional = soundOptional;
		this.scale = scale;
	}
	public static SendPlayerHitPacket decode(RegistryFriendlyByteBuf buf)
	{
		return new SendPlayerHitPacket(buf.readVec3(), ByteBufCodecs.optional(SoundEvent.DIRECT_STREAM_CODEC).decode(buf), buf.readFloat());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buf)
	{
		buf.writeVec3(impactPos);
		SOUND_STREAM_CODEC.encode(buf, soundOptional);
		buf.writeFloat(scale);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		Player player = ClientUtils.getClientPlayer();
		if (player == null)
			return;
		
		Level level = player.level();
		soundOptional.ifPresent(soundEvent -> level.playLocalSound(impactPos.x, impactPos.y, impactPos.z, soundEvent, SoundSource.PLAYERS, 1f, 1f, false));
		level.addParticle(new InkHitParticleData(ColorUtils.getEntityColor(player), scale), impactPos.x, impactPos.y, impactPos.z, 0, 0, 0);
	}
}
