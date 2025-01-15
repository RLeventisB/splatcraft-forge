package net.splatcraft.handlers;

import com.google.common.reflect.TypeToken;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.ChunkEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.utils.value.IntValue;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColorProvider;
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
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
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
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
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
		ClientTickEvent.CLIENT_LEVEL_POST.register(ChunkInkHandler::onClientWorldTickStart);
		
		ChunkEvent.LOAD_DATA.register(ChunkInkCapability::tryReadLegacyData);
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
		HashMap<ChunkPos, List<IncrementalChunkBasedPacket>> chunkPackets = sharedPacket.computeIfAbsent(world, v -> new HashMap<>());
		List<IncrementalChunkBasedPacket> existingPacketsInBlock = chunkPackets.computeIfAbsent(chunkPos, v -> new ArrayList<>());
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
			.filter(Objects::nonNull).filter(chunk -> ChunkInkCapability.hasAndNotEmpty(chunk)).toList();
		int maxChunkCheck = Math.min(world.random.nextInt(MAX_DECAYABLE_CHUNKS), chunks.size());
		
		for (int i = 0; i < maxChunkCheck; i++)
		{
			WorldChunk chunk = chunks.get(world.random.nextInt(chunks.size()));
			ChunkInk worldInk = ChunkInkCapability.get(chunk);
			HashMap<RelativeBlockPos, ChunkInk.BlockEntry> decayableInk = new HashMap<>(worldInk.getInkInChunk());
			
			int blockCount = 0;
			while (!decayableInk.isEmpty() && blockCount < MAX_DECAYABLE_PER_CHUNK)
			{
				RelativeBlockPos pos = decayableInk.keySet().toArray(new RelativeBlockPos[] {})[world.random.nextInt(decayableInk.size())];
				BlockPos clearPos = pos.toAbsolute(chunk.getPos());
				
				if (!SplatcraftGameRules.getLocalizedRule(world, clearPos, SplatcraftGameRules.INK_DECAY) ||
					world.random.nextFloat() >= SplatcraftGameRules.getIntRuleValue(world, SplatcraftGameRules.INK_DECAY_RATE) * 0.001f ||
					decayableInk.get(pos).immutable)
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
	public static void sendChunkData(ServerPlayNetworkHandler handler, World world, WorldChunk chunk)
	{
		if (!ChunkInkCapability.hasAndNotEmpty(chunk))
			return;
		ChunkInk worldInk = ChunkInkCapability.get(chunk);
		handler.send(SplatcraftPacketHandler.CHANNEL.toPacket(NetworkManager.Side.S2C, new WatchInkPacket(chunk.getPos(), worldInk.getInkInChunk()), world.getRegistryManager()), null);
	}
	@Environment(EnvType.CLIENT)
	public static void updateClientInkForChunk(World world, WorldChunk chunk)
	{
		ChunkPos chunkPos = chunk.getPos();
		if (INK_CACHE.containsKey(chunkPos))
		{
			ChunkInk chunkInk = ChunkInkCapability.get(chunk);
			
			INK_CACHE.get(chunkPos).forEach((relativePos, entry) ->
			{
				BlockPos pos = relativePos.toAbsolute(chunkPos);
				entry.apply(chunkInk, relativePos);
				BlockState state = world.getBlockState(pos);
				world.updateListeners(pos, state, state, 0);
				world.getChunk(chunkPos.x, chunkPos.z).setNeedsSaving(true);
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
		public static final ModelElementTexture defaultUv = new ModelElementTexture(new float[] {0, 0, 1, 1}, 0);
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
		public static BakedModel getInkedBakedModel()
		{
			if (model == null)
			{
				model = MinecraftClient.getInstance().getBakedModelManager().getBlockModels().getModel(SplatcraftBlocks.inkedBlock.get().getDefaultState());
			}
			return model;
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