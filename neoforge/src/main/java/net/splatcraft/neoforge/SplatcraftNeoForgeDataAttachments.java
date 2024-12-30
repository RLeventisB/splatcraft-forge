package net.splatcraft.neoforge;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;

import java.util.function.Supplier;

public class SplatcraftNeoForgeDataAttachments
{
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Splatcraft.MODID);
	public static final Supplier<AttachmentType<EntityInfo>> ENTITY_INFO = ATTACHMENT_TYPES.register(
		"entity_info", () -> AttachmentType.builder(() -> new EntityInfo()).serialize(EntityInfo.CODEC).copyOnDeath().build());
	public static final Supplier<AttachmentType<InkOverlayInfo>> INK_OVERLAY = ATTACHMENT_TYPES.register(
		"ink_overlay", () -> AttachmentType.builder(InkOverlayInfo::new).serialize(InkOverlayInfo.CODEC).build());
	public static final Supplier<AttachmentType<ChunkInk>> CHUNK_INK = ATTACHMENT_TYPES.register(
		"chunk_ink", () -> AttachmentType.builder(() -> new ChunkInk()).serialize(ChunkInk.CODEC).build());
	public static final Supplier<AttachmentType<SaveInfo>> SAVE_INFO = ATTACHMENT_TYPES.register(
		"save_info", () -> AttachmentType.builder(SaveInfo::new).serialize(SaveInfo.CODEC).build());
}
