package net.splatcraft.handlers;

import com.google.common.reflect.TypeToken;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.ChunkEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.utils.value.IntValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.json.JsonUnbakedModel;
import net.minecraft.client.render.model.json.ModelElementFace;
import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.worldink.ChunkInk;
import net.splatcraft.data.capabilities.worldink.ChunkInkCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.DeleteInkPacket;
import net.splatcraft.network.s2c.IncrementalChunkBasedPacket;
import net.splatcraft.network.s2c.UpdateInkPacket;
import net.splatcraft.network.s2c.WatchInkPacket;
import net.splatcraft.registries.SplatcraftBlocks;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.util.*;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class ChunkInkHandler
{
	public static final HashMap<World, HashMap<ChunkPos, List<IncrementalChunkBasedPacket>>> sharedPacket = new HashMap<>();
	private static final HashMap<World, List<BlockPos>> INK_IGNORE_REMOVE = new HashMap<>();
	private static final HashMap<ChunkPos, HashMap<RelativeBlockPos, ChunkInk.BlockEntry>> INK_CACHE = new HashMap<>();
	private static final int MAX_DECAYABLE_PER_CHUNK = 3;
	private static final int MAX_DECAYABLE_CHUNKS = 10;
	public static void registerEvents()
	{
		InteractionEvent.RIGHT_CLICK_BLOCK.register(ChunkInkHandler::onBlockPlace);
		BlockEvent.BREAK.register(ChunkInkHandler::onBlockBreak);
		TickEvent.SERVER_LEVEL_PRE.register(ChunkInkHandler::onWorldTickStart);
		TickEvent.SERVER_LEVEL_POST.register(ChunkInkHandler::onWorldTickEnd);
		ClientTickEvent.CLIENT_LEVEL_PRE.register(ChunkInkHandler::onClientWorldTickStart);
		
		ChunkEvent.LOAD_DATA.register(ChunkInkHandler::onChunkDataLoad);
		ChunkEvent.LOAD_DATA.register(ChunkInkCapability::onChunkDataRead);
		ChunkEvent.SAVE_DATA.register(ChunkInkCapability::onChunkDataSave);
	}
	public static void addInkToRemove(World world, BlockPos pos)
	{
		addIncrementalPacket(world, pos, DeleteInkPacket.class, DeleteInkPacket::new);
	}
	public static void addInkToUpdate(World world, BlockPos pos)
	{
		addIncrementalPacket(world, pos, UpdateInkPacket.class, UpdateInkPacket::new);
	}
	public static <T extends IncrementalChunkBasedPacket> void addIncrementalPacket(World world, BlockPos pos, Class<T> tClass, Function<ChunkPos, T> factory)
	{
		if (world.isClient)
			return;
		
		ChunkPos chunkPos = CommonUtils.getChunkPos(pos);
		HashMap<ChunkPos, List<IncrementalChunkBasedPacket>> chunkPackets = sharedPacket.getOrDefault(world, new HashMap<>());
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
		packet.add(world, pos);
		chunkPackets.put(chunkPos, existingPacketsInBlock);
		sharedPacket.put(world, chunkPackets);
	}
	//Ink Removal
	public static void onBlockUpdate(World world, BlockPos pos, List<Direction> directions)
	{
		checkForInkRemoval(world, pos);
		directions.forEach(direction -> checkForInkRemoval(world, pos.offset(direction)));
	}
	public static EventResult onBlockBreak(World level, BlockPos pos, BlockState state, ServerPlayerEntity player, @Nullable IntValue xp)
	{
		InkBlockUtils.clearBlock(level, pos, true);
		return EventResult.pass();
	}
	private static void checkForInkRemoval(World world, BlockPos pos)
	{
		ChunkInk.BlockEntry inkBlock = InkBlockUtils.getInkBlock(world, pos);
		if (inkBlock != null && inkBlock.isInkedAny())
		{
			for (int i = 0; i < 6; i++)
			{
				if (inkBlock.isInked(i) && InkBlockUtils.isUninkable(world, pos, Direction.byId(i), true))
				{
					List<BlockPos> blockPos = INK_IGNORE_REMOVE.get(world);
					if (INK_IGNORE_REMOVE.containsKey(world) && blockPos.contains(pos))
					{
						blockPos.remove(pos);
						if (blockPos.isEmpty())
							INK_IGNORE_REMOVE.remove(world);
						else
							INK_IGNORE_REMOVE.put(world, blockPos);
					}
					else
					{
						ColorUtils.addInkDestroyParticle(world, pos, inkBlock.color(i));
					}
					InkBlockUtils.clearInk(world, pos, i, false);
				}
			}
		}
	}
	//prevent foliage placement on ink if inkDestroysFoliage is on
	public static EventResult onBlockPlace(PlayerEntity player, Hand hand, BlockPos pos, Direction face)
	{
		Direction direction = face == null ? Direction.UP : face;
		if (SplatcraftGameRules.getLocalizedRule(player.getWorld(), pos, SplatcraftGameRules.INK_DESTROYS_FOLIAGE) &&
			InkBlockUtils.isInked(player.getWorld(), pos.offset(direction).down(), direction) &&
			player.getStackInHand(hand).getItem() instanceof net.splatcraft.items.BlockItem blockItem)
		{
			ItemPlacementContext context = blockItem.getPlacementContext(new ItemPlacementContext(new ItemUsageContext(player, hand, new BlockHitResult(pos.toCenterPos(), face, pos, false))));
			if (context != null)
			{
				BlockState state = blockItem.getBlock().getPlacementState(context);
				if (state != null && InkBlockUtils.isBlockFoliage(state))
					EventResult.interruptTrue();
			}
		}
		return EventResult.pass();
	}
	//Ink Decay
	public static void onWorldTickEnd(ServerWorld world)
	{
		if (world.getPlayers().isEmpty())
			return;
		
		if (sharedPacket.isEmpty())
			return;
		HashMap<World, HashMap<ChunkPos, List<IncrementalChunkBasedPacket>>> clonedPackets;
		synchronized (sharedPacket)
		{
			clonedPackets = new HashMap<>(sharedPacket);
			sharedPacket.clear();
		}
		
		for (var levelPackets : clonedPackets.entrySet())
		{
			for (var chunkPackets : levelPackets.getValue().entrySet())
			{
				WorldChunk chunk = levelPackets.getKey().getChunk(chunkPackets.getKey().x, chunkPackets.getKey().z);
				chunk.setNeedsSaving(true);
				for (var packet : chunkPackets.getValue())
				{
					SplatcraftPacketHandler.sendToTrackers(packet, chunk);
				}
			}
		}
	}
	public static void onWorldTickStart(ServerWorld world)
	{
		if (world.getPlayers().isEmpty())
			return;
		
		List<WorldChunk> chunks = StreamSupport.stream(world.getChunkManager().chunkLoadingManager.entryIterator().spliterator(), false).map(ChunkHolder::getWorldChunk)
			.filter(Objects::nonNull).filter(chunk -> !ChunkInkCapability.hasAndNotEmpty(world, chunk)).toList();
		int maxChunkCheck = Math.min(world.random.nextInt(MAX_DECAYABLE_CHUNKS), chunks.size());
		
		for (int i = 0; i < maxChunkCheck; i++)
		{
			WorldChunk chunk = chunks.get(world.random.nextInt(chunks.size()));
			ChunkInk worldInk = ChunkInkCapability.getOrCreate(world, chunk);
			HashMap<RelativeBlockPos, ChunkInk.BlockEntry> decayableInk = new HashMap<>(worldInk.getInkInChunk());
			
			int blockCount = 0;
			while (!decayableInk.isEmpty() && blockCount < MAX_DECAYABLE_PER_CHUNK)
			{
				RelativeBlockPos pos = decayableInk.keySet().toArray(new RelativeBlockPos[] {})[world.random.nextInt(decayableInk.size())];
				BlockPos clearPos = pos.toAbsolute(chunk.getPos());
				
				if (!SplatcraftGameRules.getLocalizedRule(world, clearPos, SplatcraftGameRules.INK_DECAY) ||
					world.random.nextFloat() >= SplatcraftGameRules.getIntRuleValue(world, SplatcraftGameRules.INK_DECAY_RATE) * 0.001f ||
					decayableInk.get(pos).inmutable)
				{
					decayableInk.remove(pos);
					continue;
				}
				
				int adjacentInk = 0;
				for (Direction dir : Direction.values())
					if (InkBlockUtils.isInkedAny(world, clearPos.offset(dir)))
						adjacentInk++;
				
				if (adjacentInk <= 0 || world.random.nextInt(adjacentInk * 2) == 0)
				{
					InkBlockUtils.clearInk(world, clearPos, InkBlockUtils.getRandomInkedFace(world, clearPos), false);
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
	public static void onClientWorldTickStart(ClientWorld world)
	{
		new ArrayList<>(INK_CACHE.keySet()).forEach(chunkPos ->
		{
			if (world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) instanceof WorldChunk chunk)
			{
				updateClientInkForChunk(world, chunk);
			}
		});
	}
	/*@SubscribeEvent
	public static void onPistonPush(PistonEvent.Pre event)
	{
		if (!(event.getWorld() instanceof World world) || event.getStructureHelper() == null)
			return;

		// lol get one lined (this is worse)
		HashMap<BlockPos, ChunkInk.BlockEntry> inkToPush = new HashMap<>(event.getStructureHelper().getToPush().stream().map(v -> Map.entry(v, InkBlockUtils.getInkBlock(level, v))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
		for (BlockPos pos : event.getStructureHelper().getToPush())
		{
			pos = pos.offset(event.getDirection());
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
	}*/
	public static void onChunkWatch(ServerPlayerEntity player, World world, WorldChunk chunk)
	{
		if (!ChunkInkCapability.has(world, chunk))
			return;
		ChunkInk worldInk = ChunkInkCapability.get(world, chunk);
		if (!worldInk.isntEmpty())
			return;
		SplatcraftPacketHandler.sendToPlayer(new WatchInkPacket(chunk.getPos(), worldInk.getInkInChunk()), player);
	}
	public static void onChunkDataLoad(Chunk chunk, World world, NbtElement nbt)
	{
		if (!ChunkInkCapability.has(world, chunk))
			return;
		ChunkInk worldInk = ChunkInkCapability.get(world, chunk);
		if (!worldInk.isntEmpty())
			return;
		SplatcraftPacketHandler.sendToDim(new WatchInkPacket(chunk.getPos(), worldInk.getInkInChunk()), world.getRegistryKey());
	}
	@Environment(EnvType.CLIENT)
	public static void updateClientInkForChunk(World world, WorldChunk chunk)
	{
		ChunkInk chunkInk = ChunkInkCapability.getOrCreate(world, chunk);
		ChunkPos chunkPos = chunk.getPos();
		
		if (INK_CACHE.containsKey(chunkPos))
		{
			INK_CACHE.get(chunkPos).forEach((relativePos, entry) ->
			{
				BlockPos pos = relativePos.toAbsolute(chunkPos);
				entry.apply(chunkInk, relativePos);
				
				pos = new BlockPos(pos.getX() + chunkPos.x * 16, pos.getY(), pos.getZ() + chunkPos.z * 16);
				BlockState state = world.getBlockState(pos);
				world.updateListeners(pos, state, state, 0);
			});
			INK_CACHE.remove(chunkPos);
		}
	}
	public static void markInkInChunkForUpdate(ChunkPos pos, HashMap<RelativeBlockPos, ChunkInk.BlockEntry> map)
	{
		INK_CACHE.put(pos, map);
	}
	public static void addBlocksToIgnoreRemoveInk(World world, Collection<BlockPos> positions)
	{
		List<BlockPos> blocks;
		if (INK_IGNORE_REMOVE.containsKey(world))
		{
			blocks = INK_IGNORE_REMOVE.get(world);
		}
		else
		{
			blocks = new ArrayList<>();
		}
		blocks.addAll(positions);
		INK_IGNORE_REMOVE.put(world, blocks);
	}
	@Environment(EnvType.CLIENT)
	public static class Render
	{
		public static final Identifier INKED_BLOCK_LOCATION = Splatcraft.identifierOf("block/inked_block");
		private static BlockColorProvider splatcraftColorProvider, inkedBlockColorProvider;
		private static Sprite inkedBlockSprite;
		private static BakedModel model;
		private static BakedQuad[] glitterQuads;
		private static BakedQuad[] permaInkQuads;
		public static BlockColorProvider getSplatcraftColorProvider()
		{
			if (splatcraftColorProvider == null)
			{
				splatcraftColorProvider = (state, view, pos, tint) ->
				{
					switch (tint)
					{
						case 0: // the actual ink
							ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock((World) view, pos);
							int index = 0;
							InkColor color = InkColor.INVALID;
							if (ink != null && ink.isInked(index))
								color = ColorUtils.getColorLockedIfConfig(ink.color(index));
							return color.getColorWithAlpha(255);
//                            Arrays.fill(output, ColorARGB.toABGR(color, 255));
						case 1: // glitter
							return (0xFFFFFFFF);
						case 2: // permanent ink overlay
							return (0xFFFFFFFF);
					}
					return 0;
				};
			}
			return splatcraftColorProvider;
		}
		public static List<BakedQuad> getInkedBlockQuad(Direction direction, Random rnd, RenderLayer renderType)
		{
			if (model == null)
			{
				model = MinecraftClient.getInstance().getBakedModelManager().getBlockModels().getModel(SplatcraftBlocks.inkedBlock.get().getDefaultState());
			}
			return model.getQuads(SplatcraftBlocks.inkedBlock.get().getDefaultState(), direction, rnd);
		}
		public static Sprite getInkedBlockSprite()
		{
			if (inkedBlockSprite == null)
				inkedBlockSprite = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(INKED_BLOCK_LOCATION);
			return inkedBlockSprite;
		}
		public static Sprite getGlitterSprite()
		{
			return MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(Splatcraft.identifierOf("block/glitter"));
		}
		public static Sprite getPermanentInkSprite()
		{
			return MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(Splatcraft.identifierOf("block/permanent_ink_overlay"));
		}
		/*public static boolean splatcraft$renderInkedBlock(ChunkRendererRegion region, BlockPos pos, VertexConsumer
			consumer, MatrixStack.Entry matrix, BakedQuad quad, float[] brightness, int[] lightmap, int f2, boolean f3)
		{
			ChunkInk worldInk = ChunkInkCapability.get(((BlockRenderMixin.ChunkRegionAccessor) region).getWorld(), pos);

			int index = quad.getFace().getId();
			RelativeBlockPos offset = RelativeBlockPos.fromAbsolute(pos);
			ChunkInk.BlockEntry ink = worldInk.getInk(offset);
			if (ink == null)
				return false;

			if (!worldInk.isInked(offset, index))
			{
				if (ink.inmutable)
				{
					splatcraft$putBulkData(getPermanentInkSprite(), consumer, matrix, quad, brightness, 1, 1, 1, lightmap, f2, f3, false);
					return false;
				}
				return false;
			}

			float[] rgb = ColorUtils.hexToRGB(ink.color(index));
			Sprite sprite = null;

			InkBlockUtils.InkType type = ink.type(index);
			if (type != InkBlockUtils.InkType.CLEAR)
				sprite = getInkedBlockSprite();

			splatcraft$putBulkData(sprite, consumer, matrix, quad, brightness, rgb[0], rgb[1], rgb[2], lightmap, f2, f3, type == InkBlockUtils.InkType.GLOWING);
			if (type == InkBlockUtils.InkType.GLOWING)
				splatcraft$putBulkData(getGlitterSprite(), consumer, matrix, quad, brightness, 1, 1, 1, lightmap, f2, f3, true);

			if (ink.inmutable)
				splatcraft$putBulkData(getPermanentInkSprite(), consumer, matrix, quad, brightness, 1, 1, 1, lightmap, f2, f3, false);

			return true;
		}

		static void splatcraft$putBulkData(Sprite sprite, VertexConsumer consumer, MatrixStack.Entry getMatrices, BakedQuad bakedQuad, float[] p_85998_, float r, float g, float b, int[] p_86002_, int packedOverlay, boolean p_86004_, boolean emissive)
		{
			float[] afloat = new float[]{p_85998_[0], p_85998_[1], p_85998_[2], p_85998_[3]};
			int[] aint1 = bakedQuad.getVertexData();
			Vec3i vec3i = bakedQuad.getFace().getVector();
			Matrix4f matrix4f = getMatrices.getPositionMatrix();
			Vector3f vector3f = getMatrices.getNormalMatrix().transform(new Vector3f((float) vec3i.getX(), (float) vec3i.getY(), (float) vec3i.getZ()));
			int j = aint1.length / 8;
			MemoryStack memorystack = MemoryStack.stackPush();

			try
			{
				ByteBuffer bytebuffer = memorystack.malloc(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL.getVertexSizeByte());
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

					int l = consumer.applyBakedDiffuseLighting(emissive ? LightmapTextureManager.pack(15, 15) : p_86002_[k], bytebuffer);
					float f9 = bytebuffer.getFloat(16);
					float f10 = bytebuffer.getFloat(20);

					Vector4f vector4f = (new Vector4f(f, f1, f2, 1.0F));

					Direction.Axis axis = bakedQuad.getFace().getAxis();

					float texU = sprite == null ? f9 : sprite.getMinU() + (axis.equals(Direction.Axis.X) ? vector4f.z() : vector4f.x()) * (sprite.getMaxU() - sprite.getMinU());
					float texV = sprite == null ? f10 : sprite.getMinV() + (axis.equals(Direction.Axis.Y) ? vector4f.z() : vector4f.y()) * (sprite.getMaxV() - sprite.getMinV());

					vector4f = matrix4f.transform(vector4f);
					consumer.applyBakedNormals(vector3f, bytebuffer, getMatrices.getNormalMatrix());
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
		}*/
		public static BakedQuad[] getGlitterQuad()
		{
			if (glitterQuads == null)
			{
				ModelElementTexture uv = new ModelElementTexture(new float[] {0, 0, 16, 16}, 0);
				ArrayList<BakedQuad> quads = new ArrayList<>(6);
				for (int i = 0; i < 6; i++)
				{
					Direction direction = Direction.byId(i);
					quads.add(JsonUnbakedModel.QUAD_FACTORY.bake(
						new Vector3f(0),
						new Vector3f(16),
						new ModelElementFace(direction, 1, "splatcraft:block/glitter", uv),
						getGlitterSprite(),
						direction,
						new InkFaceBakeSettings(),
						null,
						false));
				}
				glitterQuads = quads.toArray(new BakedQuad[6]);
			}
			return glitterQuads;
		}
		public static BakedQuad[] getPermaInkQuads()
		{
			if (permaInkQuads == null)
			{
				ModelElementTexture uv = new ModelElementTexture(new float[] {0, 0, 16, 16}, 0);
				ArrayList<BakedQuad> quads = new ArrayList<>(6);
				for (int i = 0; i < 6; i++)
				{
					Direction direction = Direction.byId(i);
					quads.add(JsonUnbakedModel.QUAD_FACTORY.bake(
						new Vector3f(0),
						new Vector3f(16),
						new ModelElementFace(direction, 2, "splatcraft:block/permanent_ink_overlay", uv),
						getPermanentInkSprite(),
						direction,
						new InkFaceBakeSettings(),
						null,
						false));
				}
				permaInkQuads = quads.toArray(new BakedQuad[6]);
			}
			return permaInkQuads;
		}
		private static class InkFaceBakeSettings implements ModelBakeSettings
		{
			@Override
			public AffineTransformation getRotation()
			{
				return AffineTransformation.identity();
			}
			public boolean isUvLocked()
			{
				return true;
			}
		}
	}
}