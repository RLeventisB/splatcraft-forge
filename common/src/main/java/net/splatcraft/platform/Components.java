package net.splatcraft.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayInfo;
import net.splatcraft.data.capabilities.saveinfo.SaveInfo;

public class Components
{
	public static final ComponentData<LivingEntity, EntityInfo> ENTITY_INFO = new ComponentData<>(Splatcraft.identifierOf("entity_info"));
	public static final ComponentData<LivingEntity, InkOverlayInfo> INK_OVERLAY = new ComponentData<>(Splatcraft.identifierOf("ink_overlay"));
	public static final ComponentData<ChunkAccess, ChunkInk> CHUNK_INK = new ComponentData<>(Splatcraft.identifierOf("chunk_ink"));
	public static final ComponentData<MinecraftServer, SaveInfo> SAVE_INFO = new ComponentData<>(Splatcraft.identifierOf("save_info"));
}
