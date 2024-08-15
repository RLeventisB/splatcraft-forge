package net.splatcraft.forge.handlers;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInk;
import net.splatcraft.forge.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.forge.mixin.BlockRenderMixin;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.s2c.DeleteInkPacket;
import net.splatcraft.forge.network.s2c.IncrementalChunkBasedPacket;
import net.splatcraft.forge.network.s2c.UpdateInkPacket;
import net.splatcraft.forge.network.s2c.WatchInkPacket;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.CommonUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.RelativeBlockPos;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Mod.EventBusSubscriber
public class ChunkInkHandler
{
    public static final HashMap<Level, HashMap<ChunkPos, List<IncrementalChunkBasedPacket>>> sharedPacket = new HashMap<>();

    public static void addInkToRemove(Level level, BlockPos pos)
    {
        addIncrementalPacket(level, pos, DeleteInkPacket.class, DeleteInkPacket::new);
    }

    public static void addInkToUpdate(Level level, BlockPos pos)
    {
        addIncrementalPacket(level, pos, UpdateInkPacket.class, UpdateInkPacket::new);
    }

    public static <T extends IncrementalChunkBasedPacket> void addIncrementalPacket(Level level, BlockPos pos, Class<T> tClass, Function<ChunkPos, T> factory)
    {
        if (level.isClientSide)
            return;

        ChunkPos chunkPos = CommonUtils.getChunkPos(pos);
        HashMap<ChunkPos, List<IncrementalChunkBasedPacket>> chunkPackets = sharedPacket.getOrDefault(level, new HashMap<>());
        List<IncrementalChunkBasedPacket> existingPacketsInBlock = chunkPackets.getOrDefault(chunkPos, new ArrayList<>());
        TypeToken<T> token = TypeToken.of(tClass);
        T packet = null;
        for (var extraData : existingPacketsInBlock)
        {
            if (token.isSupertypeOf(TypeToken.of(extraData.getClass())))
                packet = (T) extraData;
        }
        if (packet == null)
        {
            packet = factory.apply(chunkPos);
            existingPacketsInBlock.add(packet);
        }
        packet.add(level, pos);
        chunkPackets.put(chunkPos, existingPacketsInBlock);
        sharedPacket.put(level, chunkPackets);
    }

    @SubscribeEvent //Ink Removal
    public static void onBlockUpdate(BlockEvent.NeighborNotifyEvent event)
    {
        if (event.getLevel() instanceof Level level)
        {
            checkForInkRemoval(level, event.getPos());
            event.getNotifiedSides().forEach(direction -> checkForInkRemoval(level, event.getPos().relative(direction)));
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        ChunkInk.BlockEntry inkBlock = InkBlockUtils.getInkBlock(event.getPlayer().level(), event.getPos());
        if (inkBlock != null)
        {
            InkBlockUtils.clearBlock(event.getPlayer().level(), event.getPos(), true);
        }
    }

    private static void checkForInkRemoval(Level level, BlockPos pos)
    {
        ChunkInk.BlockEntry inkBlock = InkBlockUtils.getInkBlock(level, pos);
        if (inkBlock != null && inkBlock.isInkedAny())
        {
            for (int i = 0; i < 6; i++)
            {
                if (inkBlock.isInked(i) && InkBlockUtils.isUninkable(level, pos, Direction.from3DDataValue(i)))
                {
                    ColorUtils.addInkDestroyParticle(level, pos, inkBlock.color(i));
                    inkBlock.clear(i);
                }
            }
        }
    }

    @SubscribeEvent //prevent foliage placement on ink if inkDestroysFoliage is on
    public static void onBlockPlace(PlayerInteractEvent.RightClickBlock event)
    {
        Direction direction = event.getFace() == null ? Direction.UP : event.getFace();
        if (SplatcraftGameRules.getLocalizedRule(event.getLevel(), event.getPos(), SplatcraftGameRules.INK_DESTROYS_FOLIAGE) &&
                InkBlockUtils.isInked(event.getLevel(), event.getPos().relative(direction).below(), direction) &&
                event.getItemStack().getItem() instanceof BlockItem blockItem)
        {
            BlockPlaceContext context = blockItem.updatePlacementContext(new BlockPlaceContext(event.getLevel(), event.getEntity(), event.getHand(), event.getItemStack(), event.getHitVec()));
            if (context != null)
            {
                BlockState state = blockItem.getBlock().getStateForPlacement(context);
                if (state != null && InkBlockUtils.isBlockFoliage(state))
                    event.setCanceled(true);
            }
        }
    }

    private static final int MAX_DECAYABLE_PER_CHUNK = 3;
    private static final int MAX_DECAYABLE_CHUNKS = 10;

    @SubscribeEvent //Ink Decay
    public static void onWorldTick(TickEvent.LevelTickEvent event)
    {
        if (!event.level.players().isEmpty())
        {
            if (event.phase == TickEvent.Phase.START)
            {
                if (event.level instanceof ServerLevel level)
                {
                    List<LevelChunk> chunks = StreamSupport.stream(level.getChunkSource().chunkMap.getChunks().spliterator(), false).map(ChunkHolder::getTickingChunk)
                            .filter(Objects::nonNull).filter(chunk -> !ChunkInkCapability.get(chunk).getInkInChunk().isEmpty()).toList();
                    int maxChunkCheck = Math.min(level.random.nextInt(MAX_DECAYABLE_CHUNKS), chunks.size());

                    for (int i = 0; i < maxChunkCheck; i++)
                    {
                        LevelChunk chunk = chunks.get(level.random.nextInt(chunks.size()));
                        ChunkInk worldInk = ChunkInkCapability.get(chunk);
                        HashMap<RelativeBlockPos, ChunkInk.BlockEntry> decayableInk = new HashMap<>(worldInk.getInkInChunk());

                        int blockCount = 0;
                        while (!decayableInk.isEmpty() && blockCount < MAX_DECAYABLE_PER_CHUNK)
                        {
                            RelativeBlockPos pos = decayableInk.keySet().toArray(new RelativeBlockPos[]{})[level.random.nextInt(decayableInk.size())];
                            BlockPos clearPos = pos.toAbsolute(chunk.getPos());

                            if (!SplatcraftGameRules.getLocalizedRule(level, clearPos, SplatcraftGameRules.INK_DECAY) ||
                                    level.random.nextFloat() >= SplatcraftGameRules.getIntRuleValue(level, SplatcraftGameRules.INK_DECAY_RATE) * 0.001f || //TODO make localized int rules
                                    decayableInk.get(pos).inmutable)
                            {
                                decayableInk.remove(pos);
                                continue;
                            }

                            int adjacentInk = 0;
                            for (Direction dir : Direction.values())
                                if (InkBlockUtils.isInkedAny(level, clearPos.relative(dir)))
                                    adjacentInk++;

                            if (adjacentInk <= 0 || level.random.nextInt(adjacentInk * 2) == 0)
                            {
                                InkBlockUtils.clearInk(level, clearPos, InkBlockUtils.getRandomInkedFace(level, clearPos), false);
                                decayableInk.remove(pos);
                                blockCount++;
                            }
                            else
                            {
                                decayableInk.remove(pos);
                            }
                        }
                    }
                }
                else if (event.level.isClientSide)
                {
                    new ArrayList<>(INK_CACHE.keySet()).forEach(chunkPos ->
                    {
                        if (event.level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) instanceof LevelChunk chunk)
                        {
                            updateClientInkForChunk(event.level, chunk);
                        }
                    });
                }
            }
            else if (event.phase == TickEvent.Phase.END && !event.level.isClientSide)
            {
                if (sharedPacket.isEmpty())
                    return;
                HashMap<Level, HashMap<ChunkPos, List<IncrementalChunkBasedPacket>>> clonedPackets;
                synchronized (sharedPacket)
                {
                    clonedPackets = new HashMap<>(sharedPacket);
                    sharedPacket.clear();
                }

                for (var levelPackets : clonedPackets.entrySet())
                {
                    for (var chunkPackets : levelPackets.getValue().entrySet())
                    {
                        LevelChunk chunk = levelPackets.getKey().getChunk(chunkPackets.getKey().x, chunkPackets.getKey().z);
                        chunk.setUnsaved(true);
                        for (var packet : chunkPackets.getValue())
                        {
                            SplatcraftPacketHandler.sendToTrackers(packet, chunk);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPistonPush(PistonEvent.Pre event)
    {
        if (!(event.getLevel() instanceof Level level) || event.getStructureHelper() == null)
            return;

        // lol get one lined (this is worse)
        HashMap<BlockPos, ChunkInk.BlockEntry> inkToPush = new HashMap<>(event.getStructureHelper().getToPush().stream().map(v -> Map.entry(v, InkBlockUtils.getInkBlock(level, v))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        for (BlockPos pos : event.getStructureHelper().getToPush())
        {
            pos = pos.relative(event.getDirection());
            if (inkToPush.get(pos) == null)
                InkBlockUtils.clearBlock(level, pos, true);
            else
            {
                ChunkInk.BlockEntry toCopy = inkToPush.get(pos);
                InkBlockUtils.inkBlock(level, pos, -1, Direction.UP, InkBlockUtils.InkType.NORMAL, 0);
                ChunkInk.BlockEntry newEntry = InkBlockUtils.getInkBlock(level, pos);
                for (byte i = 0; i < 6; i++)
                {
                    newEntry.entries[i] = toCopy.entries[i];
                }
                newEntry.inmutable = toCopy.inmutable;
            }
        }
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event)
    {
        if (event.getLevel().getChunk(event.getPos().x, event.getPos().z, ChunkStatus.FULL, false) instanceof LevelChunk chunk)
        {
            ChunkInk worldInk = ChunkInkCapability.get(chunk);
            if (!worldInk.getInkInChunk().isEmpty())
                SplatcraftPacketHandler.sendToPlayer(new WatchInkPacket(chunk.getPos(), worldInk.getInkInChunk()), event.getPlayer());
        }
    }

    @SubscribeEvent
    public static void onChunkDataLoad(ChunkDataEvent.Load event)
    {
        if (event.getChunk() instanceof LevelChunk chunk)
        {
            ChunkInk worldInk = ChunkInkCapability.get(chunk);
            if (!worldInk.getInkInChunk().isEmpty())
            {
                SplatcraftPacketHandler.sendToDim(new WatchInkPacket(chunk.getPos(), worldInk.getInkInChunk()), chunk.getLevel().dimension());
            }
        }
    }

    private static final HashMap<ChunkPos, HashMap<RelativeBlockPos, ChunkInk.BlockEntry>> INK_CACHE = new HashMap<>();

    @OnlyIn(Dist.CLIENT)
    public static void updateClientInkForChunk(Level level, LevelChunk chunk)
    {
        ChunkInk chunkInk = ChunkInkCapability.get(chunk);
        ChunkPos chunkPos = chunk.getPos();

        if (INK_CACHE.containsKey(chunkPos))
        {
            INK_CACHE.get(chunkPos).forEach((relativePos, entry) ->
            {
                BlockPos pos = relativePos.toAbsolute(chunkPos);
                entry.apply(chunkInk, relativePos);

                pos = new BlockPos(pos.getX() + chunkPos.x * 16, pos.getY(), pos.getZ() + chunkPos.z * 16);
                BlockState state = level.getBlockState(pos);
                level.sendBlockUpdated(pos, state, state, 0);
            });
            INK_CACHE.remove(chunkPos);
        }
    }

    public static void markInkInChunkForUpdate(ChunkPos pos, HashMap<RelativeBlockPos, ChunkInk.BlockEntry> map)
    {
        INK_CACHE.put(pos, map);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Render
    {
        public static TextureAtlasSprite getInkedBlockSprite()
        {
            return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(Splatcraft.MODID, "block/inked_block"));
        }

        public static TextureAtlasSprite getGlitterSprite()
        {
            return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(Splatcraft.MODID, "block/glitter"));
        }

        public static TextureAtlasSprite getPermanentInkSprite()
        {
            return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(Splatcraft.MODID, "block/permanent_ink_overlay"));
        }

        public static boolean splatcraft$renderInkedBlock(RenderChunkRegion region, BlockPos pos, VertexConsumer
                consumer, PoseStack.Pose pose, BakedQuad quad, float[] brightness, int[] lightmap, int f2, boolean f3)
        {
            ChunkInk worldInk = ChunkInkCapability.get(((BlockRenderMixin.ChunkRegionAccessor) region).getLevel(), pos);

            int index = quad.getDirection().get3DDataValue();
            RelativeBlockPos relative = RelativeBlockPos.fromAbsolute(pos);
            ChunkInk.BlockEntry ink = worldInk.getInk(relative);
            if (ink == null)
                return false;

            if (!worldInk.isInked(relative, index))
            {
                if (ink.inmutable)
                {
                    splatcraft$putBulkData(getPermanentInkSprite(), consumer, pose, quad, brightness, 1, 1, 1, lightmap, f2, f3, false);
                    return false;
                }
                return false;
            }

            float[] rgb = ColorUtils.hexToRGB(ink.color(index));
            TextureAtlasSprite sprite = null;

            InkBlockUtils.InkType type = ink.type(index);
            if (type != InkBlockUtils.InkType.CLEAR)
                sprite = getInkedBlockSprite();

            splatcraft$putBulkData(sprite, consumer, pose, quad, brightness, rgb[0], rgb[1], rgb[2], lightmap, f2, f3, type == InkBlockUtils.InkType.GLOWING);
            if (type == InkBlockUtils.InkType.GLOWING)
                splatcraft$putBulkData(getGlitterSprite(), consumer, pose, quad, brightness, 1, 1, 1, lightmap, f2, f3, true);

            if (ink.inmutable)
                splatcraft$putBulkData(getPermanentInkSprite(), consumer, pose, quad, brightness, 1, 1, 1, lightmap, f2, f3, false);

            return true;
        }

        static void splatcraft$putBulkData(TextureAtlasSprite sprite, VertexConsumer consumer, PoseStack.Pose pose, BakedQuad bakedQuad, float[] p_85998_, float r, float g, float b, int[] p_86002_, int packedOverlay, boolean p_86004_, boolean emissive)
        {
            float[] afloat = new float[]{p_85998_[0], p_85998_[1], p_85998_[2], p_85998_[3]};
            int[] aint1 = bakedQuad.getVertices();
            Vec3i vec3i = bakedQuad.getDirection().getNormal();
            Matrix4f matrix4f = pose.pose();
            Vector3f vector3f = pose.normal().transform(new Vector3f((float) vec3i.getX(), (float) vec3i.getY(), (float) vec3i.getZ()));
            int j = aint1.length / 8;
            MemoryStack memorystack = MemoryStack.stackPush();

            try
            {
                ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
                IntBuffer intbuffer = bytebuffer.asIntBuffer();

                for (int k = 0; k < j; ++k)
                {
                    intbuffer.clear();
                    intbuffer.put(aint1, k * 8, 8);
                    float f = bytebuffer.getFloat(0);
                    float f1 = bytebuffer.getFloat(4);
                    float f2 = bytebuffer.getFloat(8);
                    float f3;
                    float f4;
                    float f5;

                    if (emissive)
                        afloat[k] = Math.min(1, afloat[k] + 0.5f);

                    if (p_86004_)
                    {
                        float f6 = (float) (bytebuffer.get(12) & 255) / 255.0F;
                        float f7 = (float) (bytebuffer.get(13) & 255) / 255.0F;
                        float f8 = (float) (bytebuffer.get(14) & 255) / 255.0F;

                        f3 = f6 * afloat[k] * r;
                        f4 = f7 * afloat[k] * g;
                        f5 = f8 * afloat[k] * b;
                    }
                    else
                    {
                        f3 = afloat[k] * r;
                        f4 = afloat[k] * g;
                        f5 = afloat[k] * b;
                    }

                    int l = consumer.applyBakedLighting(emissive ? LightTexture.pack(15, 15) : p_86002_[k], bytebuffer);
                    float f9 = bytebuffer.getFloat(16);
                    float f10 = bytebuffer.getFloat(20);

                    Vector4f vector4f = (new Vector4f(f, f1, f2, 1.0F));

                    Direction.Axis axis = bakedQuad.getDirection().getAxis();

                    float texU = sprite == null ? f9 : sprite.getU0() + (axis.equals(Direction.Axis.X) ? vector4f.z() : vector4f.x()) * (sprite.getU1() - sprite.getU0());
                    float texV = sprite == null ? f10 : sprite.getV0() + (axis.equals(Direction.Axis.Y) ? vector4f.z() : vector4f.y()) * (sprite.getV1() - sprite.getV0());

                    vector4f = matrix4f.transform(vector4f);
                    consumer.applyBakedNormals(vector3f, bytebuffer, pose.normal());
                    consumer.vertex(vector4f.x(), vector4f.y(), vector4f.z(), f3, f4, f5, 1.0F, texU, texV, packedOverlay, l, vector3f.x(), vector3f.y(), vector3f.z());
                }
            }
            catch (Throwable throwable1)
            {
                try
                {
                    memorystack.close();
                }
                catch (Throwable throwable)
                {
                    throwable1.addSuppressed(throwable);
                }

                throw throwable1;
            }

            memorystack.close();
        }
    }
}
