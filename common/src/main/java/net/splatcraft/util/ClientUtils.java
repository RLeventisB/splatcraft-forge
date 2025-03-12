package net.splatcraft.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.handlers.DataHandler;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.remotes.TurfScannerItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.PlayerSetSquidC2SPacket;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import org.joml.Vector3f;

import java.util.*;

public class ClientUtils
{
	@OnlyIn(Dist.CLIENT)
	protected static final TreeMap<UUID, InkColor> clientColors = new TreeMap<>();
	@OnlyIn(Dist.CLIENT)
	public static final DataHandler.WeaponStatsListener.ReseteableMemoizedPredicate<Stage, Pair<Vec3, Vec2>[]> matchStartCameraPosProvider =
		new DataHandler.WeaponStatsListener.ReseteableMemoizedPredicate<>((stage) ->
		{
			ClientLevel world = getClient().level;
			// if the current world isn't the same as the stage's world, do nothing, we are the client, and thus we cant
			// retrieve other worlds :(
			if (world.dimension() != stage.worldKey)
				return new Pair[0];
			
			// gets the stage's (horizontal) center and find the highest y
			float stageCenterX = (stage.cornerA.getX() + stage.cornerB.getX()) / 2f;
			float stageCenterZ = (stage.cornerA.getZ() + stage.cornerB.getZ()) / 2f;
			int minY = Math.min(stage.cornerA.getY(), stage.cornerB.getY());
			int maxY = Math.max(stage.cornerA.getY(), stage.cornerB.getY());
			ArrayList<Pair<Vec3, Vec2>> posAndRotations = new ArrayList<>();
			
			Vec3 stageFloorCenter = new Vec3(
				stageCenterX,
				Optional.ofNullable(TurfScannerItem.getTopSolidOrLiquidBlock(stage.cornerA.getY(), stage.cornerB.getY(), world, minY, maxY)).map(v -> (float) v.getY()).orElse((float) Math.min(stage.cornerA.getY(), stage.cornerB.getY())) + 2,
				stageCenterZ
			);
			// rotation is handled outside of this
			posAndRotations.add(Pair.of(stageFloorCenter, null));
			
			// spawn pads
			Map<InkColor, List<SpawnPadTileEntity>> spawnPadPositions = stage.getSpawnPads(world);
			
			// put the current client's spawn pad as the first!!! this breaks if there are multiple spawn pads of the same color tho
			InkColor clientPlayerColor = getClientPlayerColor(getClientPlayer().getUUID());
			List<SpawnPadTileEntity> clientSpawnPads = spawnPadPositions.get(clientPlayerColor);
			
			if (clientSpawnPads != null)
			{
				spawnPadPositions.remove(clientPlayerColor);
				
				SpawnPadTileEntity randomClientPad = Util.getRandom(clientSpawnPads, world.random);
				
				addPadToList(randomClientPad, stageFloorCenter, posAndRotations);
			}
			
			for (Map.Entry<InkColor, List<SpawnPadTileEntity>> spawnPadEntrySet : spawnPadPositions.entrySet())
			{
				SpawnPadTileEntity randomPad = Util.getRandom(spawnPadEntrySet.getValue(), world.random);
				
				addPadToList(randomPad, stageFloorCenter, posAndRotations);
			}
			
			return posAndRotations.toArray(Pair[]::new);
		}
		);
	@OnlyIn(Dist.CLIENT)
	public static Pair<UUID, Vector3f> killCamData;
	private static void addPadToList(SpawnPadTileEntity randomPad, Vec3 stageFloorCenter, ArrayList<Pair<Vec3, Vec2>> posAndRotations)
	{
		Vec3 spawnPadCenter = randomPad.getSuperJumpPos().add(0, 1, 0);
		Vec3 dirCenterToPad = spawnPadCenter.subtract(stageFloorCenter).normalize();
		Vec3 lookPosition = spawnPadCenter.subtract(dirCenterToPad.scale(3));
		float pitch = (float) (Mth.atan2(dirCenterToPad.y, dirCenterToPad.horizontalDistance()) * Mth.RAD_TO_DEG);
		float yaw = (float) (Mth.atan2(dirCenterToPad.x, dirCenterToPad.z) * Mth.RAD_TO_DEG);
		
		posAndRotations.add(Pair.of(lookPosition, new Vec2(-pitch, -yaw)));
	}
	@OnlyIn(Dist.CLIENT)
	public static void resetClientColors()
	{
		clientColors.clear();
	}
	@OnlyIn(Dist.CLIENT)
	public static InkColor getClientPlayerColor(UUID player)
	{
		return clientColors.getOrDefault(player, InkColor.INVALID);
	}
	@OnlyIn(Dist.CLIENT)
	public static void setClientPlayerColor(UUID player, InkColor color)
	{
		clientColors.put(player, color);
	}
	@OnlyIn(Dist.CLIENT)
	public static void putClientColors(TreeMap<UUID, InkColor> map)
	{
		clientColors.putAll(map);
	}
	@OnlyIn(Dist.CLIENT)
	public static LocalPlayer getClientPlayer()
	{
		return Minecraft.getInstance().player;
	}
	public static boolean showDurabilityBar(ItemStack stack)
	{
		return (SplatcraftConfig.get("splatcraft.inkIndicator").equals(SplatcraftConfig.InkIndicator.BOTH) || SplatcraftConfig.get("splatcraft.inkIndicator").equals(SplatcraftConfig.InkIndicator.DURABILITY)) &&
			getClientPlayer().getItemInHand(InteractionHand.MAIN_HAND).equals(stack) && getDurabilityForDisplay() > 0;
	}
	public static double getDurabilityForDisplay()
	{
		Player player = getClientPlayer();
		
		if (!SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.REQUIRE_INK_TANK))
		{
			return 0;
		}
		
		ItemStack chestpiece = player.getItemBySlot(EquipmentSlot.CHEST);
		if (chestpiece.getItem() instanceof InkTankItem item)
		{
			return InkTankItem.getInkAmount(chestpiece) / item.capacity;
		}
		return 1;
	}
	public static boolean shouldRenderSide(BlockEntity te, Direction direction)
	{
		if (te.getLevel() == null)
			return false;
		
		BlockPos tePos = te.getBlockPos();
		
		Vector3f lookVec = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
		Vec3 blockVec = Vec3.atBottomCenterOf(tePos).add(lookVec.x(), lookVec.y(), lookVec.z());
		
		Vec3 directionVec3d = blockVec.subtract(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()).normalize();
		Vector3f directionVec = new Vector3f((float) directionVec3d.x, (float) directionVec3d.y, (float) directionVec3d.z);
		if (lookVec.dot(directionVec) > 0)
		{
			if (direction == null) return true;
			BlockState offset = te.getLevel().getBlockState(tePos.relative(direction));
			return offset.equals(Blocks.BARRIER.defaultBlockState()) || !offset.isSolid() || !offset.isRedstoneConductor(te.getLevel(), tePos.relative(direction));
		}
		
		return false;
	}
	public static void setSquid(EntityInfo cap, boolean newSquid)
	{
		if (cap.isSquid() == newSquid)
		{
			return;
		}
		cap.setIsSquid(newSquid);
		if (!newSquid)
			cap.flagSquidCancel();
		SplatcraftPacketHandler.sendToServer(new PlayerSetSquidC2SPacket(newSquid));
	}
	@OnlyIn(Dist.CLIENT)
	public static Minecraft getClient()
	{
		return Minecraft.getInstance();
	}
	@OnlyIn(Dist.CLIENT)
	public static Pair<Vec3, Vec2>[] getMatchIntroData(Stage stage)
	{
		return matchStartCameraPosProvider.apply(stage);
	}
}