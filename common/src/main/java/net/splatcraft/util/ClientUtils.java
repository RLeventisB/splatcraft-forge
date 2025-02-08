package net.splatcraft.util;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
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
	@Environment(EnvType.CLIENT)
	protected static final TreeMap<UUID, InkColor> clientColors = new TreeMap<>();
	@Environment(EnvType.CLIENT)
	public static final DataHandler.WeaponStatsListener.ReseteableMemoizedPredicate<Stage, Pair<Vec3d, Vec2f>[]> matchStartCameraPosProvider =
		new DataHandler.WeaponStatsListener.ReseteableMemoizedPredicate<>((stage) ->
		{
			ClientWorld world = getClient().world;
			// if the current world isn't the same as the stage's world, do nothing, we are the client, and thus we cant
			// retrieve other worlds :(
			if (world.getRegistryKey() != stage.worldKey)
				return new Pair[0];
			
			// gets the stage's (horizontal) center and find the highest y
			float stageCenterX = (stage.cornerA.getX() + stage.cornerB.getX()) / 2f;
			float stageCenterZ = (stage.cornerA.getZ() + stage.cornerB.getZ()) / 2f;
			int minY = Math.min(stage.cornerA.getY(), stage.cornerB.getY());
			int maxY = Math.max(stage.cornerA.getY(), stage.cornerB.getY());
			ArrayList<Pair<Vec3d, Vec2f>> posAndRotations = new ArrayList<>();
			
			Vec3d stageFloorCenter = new Vec3d(
				stageCenterX,
				Optional.ofNullable(TurfScannerItem.getTopSolidOrLiquidBlock(stage.cornerA.getY(), stage.cornerB.getY(), world, minY, maxY)).map(v -> (float) v.getY()).orElse((float) Math.min(stage.cornerA.getY(), stage.cornerB.getY())) + 2,
				stageCenterZ
			);
			// rotation is handled outside of this
			posAndRotations.add(Pair.of(stageFloorCenter, null));
			
			// spawn pads
			Map<InkColor, List<SpawnPadTileEntity>> spawnPadPositions = stage.getSpawnPads(world);
			
			// put the current client's spawn pad as the first!!! this breaks if there are multiple spawn pads of the same color tho
			InkColor clientPlayerColor = getClientPlayerColor(getClientPlayer().getUuid());
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
	@Environment(EnvType.CLIENT)
	public static Pair<UUID, Vector3f> killCamData;
	private static void addPadToList(SpawnPadTileEntity randomPad, Vec3d stageFloorCenter, ArrayList<Pair<Vec3d, Vec2f>> posAndRotations)
	{
		Vec3d spawnPadCenter = randomPad.getSuperJumpPos().add(0, 1, 0);
		Vec3d dirCenterToPad = spawnPadCenter.subtract(stageFloorCenter).normalize();
		Vec3d lookPosition = spawnPadCenter.subtract(dirCenterToPad.multiply(3));
		float pitch = (float) (MathHelper.atan2(dirCenterToPad.y, dirCenterToPad.horizontalLength()) * MathHelper.DEGREES_PER_RADIAN);
		float yaw = (float) (MathHelper.atan2(dirCenterToPad.x, dirCenterToPad.z) * MathHelper.DEGREES_PER_RADIAN);
		
		posAndRotations.add(Pair.of(lookPosition, new Vec2f(-pitch, -yaw)));
	}
	@Environment(EnvType.CLIENT)
	public static void resetClientColors()
	{
		clientColors.clear();
	}
	@Environment(EnvType.CLIENT)
	public static InkColor getClientPlayerColor(UUID player)
	{
		return clientColors.getOrDefault(player, InkColor.INVALID);
	}
	@Environment(EnvType.CLIENT)
	public static void setClientPlayerColor(UUID player, InkColor color)
	{
		clientColors.put(player, color);
	}
	@Environment(EnvType.CLIENT)
	public static void putClientColors(TreeMap<UUID, InkColor> map)
	{
		clientColors.putAll(map);
	}
	@Environment(EnvType.CLIENT)
	public static ClientPlayerEntity getClientPlayer()
	{
		return MinecraftClient.getInstance().player;
	}
	public static boolean showDurabilityBar(ItemStack stack)
	{
		return (SplatcraftConfig.get("splatcraft.inkIndicator").equals(SplatcraftConfig.InkIndicator.BOTH) || SplatcraftConfig.get("splatcraft.inkIndicator").equals(SplatcraftConfig.InkIndicator.DURABILITY)) &&
			getClientPlayer().getStackInHand(Hand.MAIN_HAND).equals(stack) && getDurabilityForDisplay() > 0;
	}
	public static double getDurabilityForDisplay()
	{
		PlayerEntity player = getClientPlayer();
		
		if (!SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.getBlockPos(), SplatcraftGameRules.REQUIRE_INK_TANK))
		{
			return 0;
		}
		
		ItemStack chestpiece = player.getEquippedStack(EquipmentSlot.CHEST);
		if (chestpiece.getItem() instanceof InkTankItem item)
		{
			return InkTankItem.getInkAmount(chestpiece) / item.capacity;
		}
		return 1;
	}
	public static boolean shouldRenderSide(BlockEntity te, Direction direction)
	{
		if (te.getWorld() == null)
			return false;
		
		BlockPos tePos = te.getPos();
		
		Vector3f lookVec = MinecraftClient.getInstance().gameRenderer.getCamera().getHorizontalPlane();
		Vec3d blockVec = Vec3d.ofBottomCenter(tePos).add(lookVec.x(), lookVec.y(), lookVec.z());
		
		Vec3d directionVec3d = blockVec.subtract(MinecraftClient.getInstance().gameRenderer.getCamera().getPos()).normalize();
		Vector3f directionVec = new Vector3f((float) directionVec3d.x, (float) directionVec3d.y, (float) directionVec3d.z);
		if (lookVec.dot(directionVec) > 0)
		{
			if (direction == null) return true;
			BlockState offset = te.getWorld().getBlockState(tePos.offset(direction));
			return offset.equals(Blocks.BARRIER.getDefaultState()) || !offset.isSolid() || !offset.isSolidBlock(te.getWorld(), tePos.offset(direction));
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
	@Environment(EnvType.CLIENT)
	public static MinecraftClient getClient()
	{
		return MinecraftClient.getInstance();
	}
	@Environment(EnvType.CLIENT)
	public static Pair<Vec3d, Vec2f>[] getMatchIntroData(Stage stage)
	{
		return matchStartCameraPosProvider.apply(stage);
	}
}