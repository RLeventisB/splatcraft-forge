package net.splatcraft.network.s2c;

import com.mojang.datafixers.util.Pair;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.particles.InkHitParticleData;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.CommonUtils;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SendPlayerHitPacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendPlayerHitPacket.class);
	private static final StreamCodec<ByteBuf, Optional<Pair<Vector3f, SoundEvent>>> SOUND_STREAM_CODEC =
		ByteBufCodecs.optional(StreamCodec.composite(
			ByteBufCodecs.VECTOR3F, Pair::getFirst,
			SoundEvent.DIRECT_STREAM_CODEC, Pair::getSecond,
			Pair::new
		));
	private static final StreamCodec<ByteBuf, SendPlayerHitPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.VECTOR3F), v -> v.impactPositions,
		SOUND_STREAM_CODEC, v -> v.soundOptional,
		ByteBufCodecs.FLOAT, v -> v.scale,
		SendPlayerHitPacket::new
	);
	private static final Map<Pair<SoundEvent, Float>, List<Vector3f>> sharedPositions = new Object2ObjectArrayMap<>();
	private final List<Vector3f> impactPositions;
	private final Optional<Pair<Vector3f, SoundEvent>> soundOptional;
	private final float scale;
	public SendPlayerHitPacket(@NotNull List<Vector3f> impactPositions, Optional<Pair<Vector3f, SoundEvent>> soundData, float scale)
	{
		this.impactPositions = impactPositions;
		soundOptional = soundData;
		this.scale = scale;
	}
	public SendPlayerHitPacket(@NotNull List<Vector3f> impactPositions, Vector3f soundPos, SoundEvent soundEvent, float scale)
	{
		this(impactPositions, Optional.of(Pair.of(soundPos, soundEvent)), scale);
	}
	public SendPlayerHitPacket(@NotNull Vector3f impactPos, SoundEvent soundEvent, float scale)
	{
		this(List.of(impactPos), Optional.of(Pair.of(impactPos, soundEvent)), scale);
	}
	public SendPlayerHitPacket(@NotNull List<Vector3f> impactPositions, float scale)
	{
		this(impactPositions, Optional.empty(), scale);
	}
	public static SendPlayerHitPacket decode(RegistryFriendlyByteBuf buf)
	{
		return STREAM_CODEC.decode(buf);
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buf)
	{
		STREAM_CODEC.encode(buf, this);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		Player player = ClientUtils.getClientPlayer();
		if (player == null)
			return;

		Level level = player.level();
		soundOptional.ifPresent(soundData ->
		{
			Vector3f pos = soundData.getFirst();
			level.playLocalSound(pos.x, pos.y, pos.z, soundData.getSecond(), SoundSource.PLAYERS, 1f, 1f, false);
		});
		InkColor entityColor = ColorUtils.getEntityColor(player);
		for (Vector3f impactPos : impactPositions)
		{
			level.addParticle(new InkHitParticleData(entityColor, scale), impactPos.x, impactPos.y, impactPos.z, 0, 0, 0);
		}
	}
	public static void accumulateHitPositions(Vector3f hitPos, SoundEvent soundEvent, float scale)
	{
		List<Vector3f> hitPositions = sharedPositions.computeIfAbsent(Pair.of(soundEvent, scale), no -> new ArrayList<>());
		hitPositions.add(hitPos);
	}
	public static void releaseHitPositions(Entity targetEntity)
	{
		if (!(targetEntity instanceof ServerPlayer serverPlayer))
		{
			sharedPositions.clear();
			return;
		}

		sharedPositions.forEach((soundData, hitPositions) ->
		{
			SplatcraftPacketHandler.sendToPlayer(new SendPlayerHitPacket(hitPositions, hitPositions.getFirst(), soundData.getFirst(), soundData.getSecond()), serverPlayer);
		});
		sharedPositions.clear();
	}
}
