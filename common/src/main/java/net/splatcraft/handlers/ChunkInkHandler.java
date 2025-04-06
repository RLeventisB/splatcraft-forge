package net.splatcraft.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.capabilities.chunkink.ChunkInk;
import net.splatcraft.data.capabilities.chunkink.ChunkInkCapability;
import net.splatcraft.items.BlockItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.DeleteInkPacket;
import net.splatcraft.network.s2c.IncrementalChunkBasedPacket;
import net.splatcraft.network.s2c.UpdateInkPacket;
import net.splatcraft.network.s2c.WatchInkPacket;
import net.splatcraft.platform.ModSide;
import net.splatcraft.platform.Services;
import net.splatcraft.platform.event.EventResult;
import net.splatcraft.platform.event.InteractionEvents;
import net.splatcraft.platform.event.TickEvents;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.InkBlockUtils;
import net.splatcraft.util.InkColor;
import net.splatcraft.util.RelativeBlockPos;

import java.util.*;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class ChunkInkHandler
{
	public static final HashMap<Level, HashMap<ChunkPos, List<IncrementalChunkBasedPacket>>> sharedPacket = new HashMap<>();
	private static final HashMap<Level, List<BlockPos>> INK_IGNORE_REMOVE = new HashMap<>();
	private static final HashMap<ChunkPos, HashMap<RelativeBlockPos, ChunkInk.BlockEntry>> INK_CACHE = new HashMap<>();
	private static final int MAX_DECAYABLE_PER_CHUNK = 3;
	private static final int MAX_DECAYABLE_CHUNKS = 10;
	public static void registerEvents()
	{
		Services.PLATFORM.registerListener(InteractionEvents.RightClickBlock.class, ChunkInkHandler::onBlockPlace);
		Services.PLATFORM.registerListener(InteractionEvents.BlockBreak.class, ChunkInkHandler::onBlockBreak);
		Services.PLATFORM.registerListener(TickEvents.ServerLevelBefore.class, ChunkInkHandler::onWorldTickStart);
		Services.PLATFORM.registerListener(TickEvents.ServerLevelAfter.class, ChunkInkHandler::onWorldTickEnd);
		
		if (Services.PLATFORM.getModSide().equals(ModSide.CLIENT))
			registerClientEvent();
	}
	@OnlyIn(Dist.CLIENT)
	private static void registerClientEvent()
	{
		Services.PLATFORM.registerListener(TickEvents.ClientLevelAfter.class, ChunkInkHandler::onClientWorldTickStart);
	}
	public static void addInkToRemove(Level world, BlockPos pos)
	{
		addIncrementalPacket(world, pos, DeleteInkPacket.class, DeleteInkPacket::new);
	}
	public static void addInkToUpdate(Level world, BlockPos pos)
	{
		addIncrementalPacket(world, pos, UpdateInkPacket.class, UpdateInkPacket::new);
	}
	public static <T extends IncrementalChunkBasedPacket> void addIncrementalPacket(Level world, BlockPos pos, Class<T> tClass, Function<ChunkPos, T> factory)
	{
		if (world.isClientSide)
			return;
		
		ChunkPos chunkPos = new ChunkPos(pos);
		HashMap<ChunkPos, List<IncrementalChunkBasedPacket>> chunkPackets = sharedPacket.computeIfAbsent(world, v -> new HashMap<>());
		List<IncrementalChunkBasedPacket> existingPacketsInBlock = chunkPackets.computeIfAbsent(chunkPos, v -> new ArrayList<>());
		T packet = null;
		for (var extraData : existingPacketsInBlock)
		{
			if (tClass.isAssignableFrom(extraData.getClass()))
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
	public static void onBlockUpdate(Level world, BlockPos pos, List<Direction> directions)
	{
		checkForInkRemoval(world, pos, Direction.values());
		directions.forEach(direction -> checkForInkRemoval(world, pos.relative(direction), new Direction[] {direction.getOpposite()}));
	}
	public static EventResult onBlockBreak(Player player, LevelAccessor level, BlockPos pos, BlockState state)
	{
		InkBlockUtils.clearBlock((Level)level, pos, true);
		return EventResult.pass();
	}
	private static void checkForInkRemoval(Level world, BlockPos pos, Direction[] directionsToCheck)
	{
		if (!SplatcraftGameRules.getLocalizedRule(world, pos, SplatcraftGameRules.BLOCK_DESTROY_INK))
			return;
		
		ChunkInk.BlockEntry inkBlock = InkBlockUtils.getInkBlock(world, pos);
		if (inkBlock != null && inkBlock.isInkedAny())
		{
			for (Direction dir : directionsToCheck)
			{
				if (inkBlock.isInked(dir.get3DDataValue()) && InkBlockUtils.isUninkable(world, pos, dir, true))
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
						ColorUtils.addInkDestroyParticle(world, pos, inkBlock.color(dir.get3DDataValue()));
					}
					InkBlockUtils.clearInk(world, pos, dir, false);
				}
			}
		}
	}
	//prevent foliage placement on ink if inkDestroysFoliage is on
	public static EventResult onBlockPlace(Player player, InteractionHand hand, Direction face, ItemStack stack, Level level, BlockPos pos)
	{
		Direction direction = face == null ? Direction.UP : face;
		if (SplatcraftGameRules.getLocalizedRule(player.level(), pos, SplatcraftGameRules.INK_DESTROYS_FOLIAGE) &&
			InkBlockUtils.isInked(player.level(), pos.relative(direction).below(), direction) &&
			player.getItemInHand(hand).getItem() instanceof BlockItem blockItem)
		{
			BlockPlaceContext context = blockItem.updatePlacementContext(new BlockPlaceContext(new UseOnContext(player, hand, new BlockHitResult(pos.getCenter(), face, pos, false))));
			if (context != null)
			{
				BlockState state = blockItem.getBlock().getStateForPlacement(context);
				if (state != null && InkBlockUtils.isBlockFoliage(state))
					return EventResult.interruptTrue();
			}
		}
		return EventResult.pass();
	}
	//Ink Decay
	public static void onWorldTickEnd(ServerLevel world)
	{
		if (world.players().isEmpty())
			return;
		
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
	public static void onWorldTickStart(ServerLevel world)
	{
		if (world.players().isEmpty())
			return;
		
		List<LevelChunk> chunks = StreamSupport.stream(world.getChunkSource().chunkMap.getChunks().spliterator(), false).map(ChunkHolder::getTickingChunk)
			.filter(Objects::nonNull).filter(ChunkInkCapability::hasAndNotEmpty).toList();
		int maxChunkCheck = Math.min(world.random.nextInt(MAX_DECAYABLE_CHUNKS), chunks.size());
		
		for (int i = 0; i < maxChunkCheck; i++)
		{
			LevelChunk chunk = chunks.get(world.random.nextInt(chunks.size()));
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
					if (InkBlockUtils.isInkedAny(world, clearPos.relative(dir)))
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
	@OnlyIn(Dist.CLIENT)
	public static void onClientWorldTickStart(ClientLevel world)
	{
		new ArrayList<>(INK_CACHE.keySet()).forEach(chunkPos ->
		{
			if (world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false) instanceof LevelChunk chunk)
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
	public static void sendChunkData(ServerGamePacketListenerImpl handler, Level world, LevelChunk chunk)
	{
		if (!ChunkInkCapability.hasAndNotEmpty(chunk))
			return;
		ChunkInk worldInk = ChunkInkCapability.get(chunk);
		SplatcraftPacketHandler.sendToPlayer(new WatchInkPacket(chunk.getPos(), worldInk.getInkInChunk()), handler.player);
	}
	@OnlyIn(Dist.CLIENT)
	public static void updateClientInkForChunk(Level world, LevelChunk chunk)
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
				world.sendBlockUpdated(pos, state, state, 0);
				world.getChunk(chunkPos.x, chunkPos.z).setUnsaved(true);
			});
			INK_CACHE.remove(chunkPos);
		}
	}
	public static void markInkInChunkForUpdate(ChunkPos pos, HashMap<RelativeBlockPos, ChunkInk.BlockEntry> map)
	{
		INK_CACHE.put(pos, map);
	}
	public static void addBlocksToIgnoreRemoveInk(Level world, Collection<BlockPos> positions)
	{
		List<BlockPos> blocks = INK_IGNORE_REMOVE.computeIfAbsent(world, v -> new ArrayList<>());
		blocks.addAll(positions);
		INK_IGNORE_REMOVE.put(world, blocks);
	}
	@OnlyIn(Dist.CLIENT)
	public static class Render
	{
		public static final ResourceLocation INKED_BLOCK_LOCATION = Splatcraft.identifierOf("block/inked_block");
		public static final BlockFaceUV defaultUv = new BlockFaceUV(new float[] {0, 0, 1, 1}, 0);
		private static BlockColor splatcraftColorProvider;
		private static TextureAtlasSprite inkedBlockSprite;
		public static BlockColor getSplatcraftColorProvider()
		{
			if (splatcraftColorProvider == null)
			{
				splatcraftColorProvider = (state, view, pos, tint) ->
				{
					switch (tint)
					{
						case 0: // the actual ink
							ChunkInk.BlockEntry ink = InkBlockUtils.getInkBlock((Level) view, pos);
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
		public static TextureAtlasSprite getInkedBlockSprite()
		{
			if (inkedBlockSprite == null)
				inkedBlockSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(INKED_BLOCK_LOCATION);
			return inkedBlockSprite;
		}
		public static TextureAtlasSprite getGlitterSprite()
		{
			return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Splatcraft.identifierOf("block/glitter"));
		}
		public static TextureAtlasSprite getPermanentInkSprite()
		{
			return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Splatcraft.identifierOf("block/permanent_ink_overlay"));
		}
	}
}