package net.splatcraft;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.splatcraft.data.capabilities.structs.*;
import net.splatcraft.platform.ComponentData;
import net.splatcraft.platform.Components;

import java.util.function.Supplier;

public class SplatcraftNeoForgeDataAttachments
{
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Splatcraft.MODID);
	public static final Supplier<AttachmentType<EntityInfo>> ENTITY_INFO = ATTACHMENT_TYPES.register(
		"entity_info", () -> AttachmentType.<EntityInfo>builder(() -> null).serialize(EntityInfo.CODEC).copyOnDeath().build());
	public static final Supplier<AttachmentType<WeaponInfo>> WEAPON_INFO = ATTACHMENT_TYPES.register(
		"weapon_info", () -> AttachmentType.<WeaponInfo>builder(() -> null).serialize(WeaponInfo.CODEC).copyOnDeath().build());
	public static final Supplier<AttachmentType<SquidInfo>> SQUID_INFO = ATTACHMENT_TYPES.register(
		"squid_info", () -> AttachmentType.<SquidInfo>builder(() -> null).serialize(SquidInfo.CODEC).copyOnDeath().build());
	public static final Supplier<AttachmentType<PlayerInfo>> PLAYER_INFO = ATTACHMENT_TYPES.register(
		"player_info", () -> AttachmentType.<PlayerInfo>builder(() -> null).serialize(PlayerInfo.CODEC).copyOnDeath().build());
	public static final Supplier<AttachmentType<InkOverlayData>> INK_OVERLAY = ATTACHMENT_TYPES.register(
		"ink_overlay", () -> AttachmentType.<InkOverlayData>builder(() -> null).serialize(InkOverlayData.CODEC).build());
	public static final Supplier<AttachmentType<ChunkInk>> CHUNK_INK = ATTACHMENT_TYPES.register(
		"chunk_ink", () -> AttachmentType.<ChunkInk>builder(() -> null).serialize(ChunkInk.CODEC).build());
	public static final Supplier<AttachmentType<SaveInfo>> SAVE_INFO = ATTACHMENT_TYPES.register(
		"save_info", () -> AttachmentType.<SaveInfo>builder(() -> null).serialize(SaveInfo.CODEC).build());
	public static void initializeExecutors()
	{
		ComponentData.registerExecutor(Components.ENTITY_INFO,
			entity -> entity.getExistingData(ENTITY_INFO),
			(entity, info) -> entity.setData(ENTITY_INFO, info),
			entity -> entity.removeData(ENTITY_INFO),
			EntityInfo::new
		);
		ComponentData.registerExecutor(Components.WEAPON_INFO,
			entity -> entity.getExistingData(WEAPON_INFO),
			(entity, info) -> entity.setData(WEAPON_INFO, info),
			entity -> entity.removeData(WEAPON_INFO),
			WeaponInfo::new
		);
		ComponentData.registerExecutor(Components.SQUID_INFO,
			entity -> entity.getExistingData(SQUID_INFO),
			(entity, info) -> entity.setData(SQUID_INFO, info),
			entity -> entity.removeData(SQUID_INFO),
			SquidInfo::new
		);
		ComponentData.registerExecutor(Components.PLAYER_INFO,
			player -> player.getExistingData(PLAYER_INFO),
			(player, info) -> player.setData(PLAYER_INFO, info),
			player -> player.removeData(PLAYER_INFO),
			PlayerInfo::new
		);
		ComponentData.registerExecutor(Components.INK_OVERLAY,
			entity -> entity.getExistingData(INK_OVERLAY),
			(entity, info) -> entity.setData(INK_OVERLAY, info),
			entity -> entity.removeData(INK_OVERLAY),
			InkOverlayData::new
		);
		ComponentData.registerExecutor(Components.CHUNK_INK,
			chunk -> chunk.getExistingData(CHUNK_INK),
			(chunk, chunkInk) -> chunk.setData(CHUNK_INK, chunkInk),
			entity -> entity.removeData(CHUNK_INK),
			ChunkInk::new
		);
		ComponentData.registerExecutor(Components.SAVE_INFO,
			server -> server.overworld().getExistingData(SAVE_INFO),
			(server, info) -> server.overworld().setData(SAVE_INFO, info),
			server -> server.overworld().removeData(SAVE_INFO),
			SaveInfo::new
		);
	}
}
