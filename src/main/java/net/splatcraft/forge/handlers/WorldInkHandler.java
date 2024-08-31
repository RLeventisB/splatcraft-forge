package net.splatcraft.forge.handlers;

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
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.Splatcraft;
import net.splatcraft.forge.data.capabilities.worldink.WorldInk;
import net.splatcraft.forge.data.capabilities.worldink.WorldInkCapability;
import net.splatcraft.forge.mixin.BlockRenderMixin;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.s2c.WatchInkPacket;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import net.splatcraft.forge.util.ColorUtils;
import net.splatcraft.forge.util.InkBlockUtils;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

@Mod.EventBusSubscriber
public class WorldInkHandler
{
	@SubscribeEvent
	public static void renderInk(RenderLevelStageEvent event)
	{
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS)
		{
			"why".isEmpty();
		}
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
	private static void checkForInkRemoval(Level level, BlockPos pos)
	{
		if (InkBlockUtils.isInked(level, pos) && InkBlockUtils.isUninkable(level, pos))
		{
			ColorUtils.addInkDestroyParticle(level, pos, InkBlockUtils.getInk(level, pos).color());
			InkBlockUtils.clearInk(level, pos, true);
		}
	}
	@SubscribeEvent //prevent foliage placement on ink if inkDestroysFoliage is on
	public static void onBlockPlace(PlayerInteractEvent.RightClickBlock event)
	{
		if (SplatcraftGameRules.getLocalizedRule(event.getLevel(), event.getPos(), SplatcraftGameRules.INK_DESTROYS_FOLIAGE) &&
			InkBlockUtils.isInked(event.getLevel(), event.getPos().relative(event.getFace() == null ? Direction.UP : event.getFace()).below()) &&
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
		if (event.phase == TickEvent.Phase.START && !event.level.players().isEmpty())
		{
			if (event.level instanceof ServerLevel level)
			{
				List<LevelChunk> chunks = StreamSupport.stream(level.getChunkSource().chunkMap.getChunks().spliterator(), false).map(ChunkHolder::getTickingChunk)
					.filter(Objects::nonNull).filter(chunk -> !WorldInkCapability.get(chunk).getInkInChunk().isEmpty()).toList();
				int maxChunkCheck = Math.min(level.random.nextInt(MAX_DECAYABLE_CHUNKS), chunks.size());
				
				for (int i = 0; i < maxChunkCheck; i++)
				{
					LevelChunk chunk = chunks.get(level.random.nextInt(chunks.size()));
					WorldInk worldInk = WorldInkCapability.get(chunk);
					HashMap<BlockPos, WorldInk.Entry> decayableInk = new HashMap<>(worldInk.getInkInChunk());
					
					int blockCount = 0;
					while (!decayableInk.isEmpty() && blockCount < MAX_DECAYABLE_PER_CHUNK)
					{
						BlockPos pos = decayableInk.keySet().toArray(new BlockPos[] {})[level.random.nextInt(decayableInk.size())];
						BlockPos clearPos = pos.offset(chunk.getPos().x * 16, 0, chunk.getPos().z * 16);
						
						if (!SplatcraftGameRules.getLocalizedRule(level, clearPos, SplatcraftGameRules.INK_DECAY) ||
							level.random.nextFloat() >= SplatcraftGameRules.getIntRuleValue(level, SplatcraftGameRules.INK_DECAY_RATE) * 0.001f || //TODO make localized int rules
							decayableInk.get(pos).equals(worldInk.getPermanentInk(pos)))
						{
							decayableInk.remove(pos);
							continue;
						}
						
						int adjacentInk = 0;
						for (Direction dir : Direction.values())
							if (InkBlockUtils.isInked(level, clearPos.relative(dir)))
								adjacentInk++;
						
						if (!(adjacentInk <= 0 || level.random.nextInt(adjacentInk * 2) == 0))
						{
							decayableInk.remove(pos);
							continue;
						}
						
						InkBlockUtils.clearInk(level, clearPos, false);
						decayableInk.remove(pos);
						
						blockCount++;
					}
				}
			}
			else if (event.level.isClientSide)
			{
				new ArrayList<>(INK_CACHE.keySet()).forEach(chunkPos ->
				{
					if (event.level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) instanceof LevelChunk chunk)
						updateClientInkForChunk(event.level, chunk);
				});
			}
		}
	}
	@SubscribeEvent
	public static void onPistonPush(PistonEvent.Pre event)
	{
		if (!(event.getLevel() instanceof Level level) || event.getStructureHelper() == null)
			return;
		
		HashMap<BlockPos, WorldInk.Entry> inkToPush = new HashMap<>();
		event.getStructureHelper().getToPush().forEach(pos -> inkToPush.put(pos, InkBlockUtils.getInk(level, pos)));
		event.getStructureHelper().getToPush().forEach(pos ->
		{
			pos = pos.relative(event.getDirection());
			if (inkToPush.get(pos) == null)
				InkBlockUtils.clearInk(level, pos, true);
			else InkBlockUtils.inkBlock(level, pos, inkToPush.get(pos).color(), 0, inkToPush.get(pos).type());
		});
	}
	@SubscribeEvent
	public static void onChunkWatch(ChunkWatchEvent.Watch event)
	{
		if (event.getLevel().getChunk(event.getPos().x, event.getPos().z, ChunkStatus.FULL, false) instanceof LevelChunk chunk)
		{
			WorldInk worldInk = WorldInkCapability.get(chunk);
			if (!worldInk.getInkInChunk().isEmpty())
				SplatcraftPacketHandler.sendToPlayer(new WatchInkPacket(chunk.getPos(), worldInk.getInkInChunk()), event.getPlayer());
		}
	}
	@SubscribeEvent
	public static void onChunkDataLoad(ChunkDataEvent.Load event)
	{
		if (event.getChunk() instanceof LevelChunk chunk)
		{
			WorldInk worldInk = WorldInkCapability.get(chunk);
			if (!worldInk.getInkInChunk().isEmpty())
			{
				SplatcraftPacketHandler.sendToDim(new WatchInkPacket(chunk.getPos(), worldInk.getInkInChunk()), chunk.getLevel().dimension());
			}
		}
	}
	private static final HashMap<ChunkPos, HashMap<BlockPos, WorldInk.Entry>> INK_CACHE = new HashMap<>();
	@OnlyIn(Dist.CLIENT)
	public static void updateClientInkForChunk(Level level, LevelChunk chunk)
	{
		WorldInk worldInk = WorldInkCapability.get(chunk);
		ChunkPos chunkPos = chunk.getPos();
		
		if (INK_CACHE.containsKey(chunkPos))
		{
			INK_CACHE.get(chunkPos).forEach((pos, entry) ->
			{
				if (entry == null || entry.type() == null)
				{
					worldInk.clearInk(pos);
				}
				else
				{
					worldInk.ink(pos, entry.color(), entry.type());
				}
				
				pos = new BlockPos(pos.getX() + chunkPos.x * 16, pos.getY(), pos.getZ() + chunkPos.z * 16);
				BlockState state = level.getBlockState(pos);
				level.sendBlockUpdated(pos, state, state, 0);
			});
			INK_CACHE.remove(chunkPos);
		}
	}
	public static void markInkInChunkForUpdate(ChunkPos pos, HashMap<BlockPos, WorldInk.Entry> map)
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
			WorldInk worldInk = WorldInkCapability.get(((BlockRenderMixin.ChunkRegionAccessor) region).getLevel(), pos);
			
			if (!worldInk.isInked(pos))
				return false;
			
			WorldInk.Entry ink = worldInk.getInk(pos);
			
			float[] rgb = ColorUtils.hexToRGB(ink.color());
			TextureAtlasSprite sprite = null;
			
			if (ink.type() != InkBlockUtils.InkType.CLEAR)
				sprite = getInkedBlockSprite();
			
			splatcraft$putBulkData(sprite, consumer, pose, quad, brightness, rgb[0], rgb[1], rgb[2], lightmap, f2, f3, ink.type() == InkBlockUtils.InkType.GLOWING);
			if (ink.type() == InkBlockUtils.InkType.GLOWING)
				splatcraft$putBulkData(getGlitterSprite(), consumer, pose, quad, brightness, 1, 1, 1, lightmap, f2, f3, true);
			
			return true;
		}
		static void splatcraft$putBulkData(TextureAtlasSprite sprite, VertexConsumer consumer, PoseStack.Pose pose, BakedQuad bakedQuad, float[] p_85998_, float r, float g, float b, int[] p_86002_, int packedOverlay, boolean p_86004_, boolean emissive)
		{
			float[] afloat = new float[] {p_85998_[0], p_85998_[1], p_85998_[2], p_85998_[3]};
			int[] aint = new int[] {p_86002_[0], p_86002_[1], p_86002_[2], p_86002_[3]};
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
					
					Direction.Axis axis = bakedQuad.getDirection().getAxis(); //Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation(Splatcraft.MODID, "blocks/inked_block"));
					
					float texU = sprite == null ? f9 : sprite.getU0() + (axis.equals(Direction.Axis.X) ? vector4f.z() : vector4f.x()) * (sprite.getU1() - sprite.getU0());
					float texV = sprite == null ? f10 : sprite.getV0() + (axis.equals(Direction.Axis.Y) ? vector4f.z() : vector4f.y()) * (sprite.getV1() - sprite.getV0());

					/* TODO fix ink rendering bleeding over into other textures (ink a spawn pad, lectern, campfire, or grate ramp for example).
					    either get this to work or find a way to make a custom texture atlas for ink
						if(sprite != null)
					{
						float width = sprite.getU1() - sprite.getU0();
						float height = sprite.getV1() - sprite.getV0();
						float uLength = ((axis.equals(Direction.Axis.X) ? vector4f.z() : vector4f.x())*(width));
						float vLength = ((axis.equals(Direction.Axis.Y) ? vector4f.z() : vector4f.y())*(height));

						if(uLength > width)
						{
							if((uLength / width) % 2 == 1)
								uLength = width - uLength % width;
							else uLength = uLength % width;
						}

						if(vLength > height)
						{
							if((vLength / height) % 2 == 1)
								vLength = height - vLength % height;
							else vLength = vLength % height;
						}

						texU = sprite.getU0() + uLength;
						texV = sprite.getV0() + vLength;
					}
					 */
					
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
