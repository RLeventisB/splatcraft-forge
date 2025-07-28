package net.splatcraft.util;

import com.google.common.collect.ImmutableList;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.data.Stage;
import net.splatcraft.handlers.SquidFormHandler;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.remotes.TurfScannerItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.PlayerSetSquidC2SPacket;
import net.splatcraft.registries.SplatcraftComponents;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.structs.InkColor;
import org.apache.logging.log4j.util.TriConsumer;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

public class ClientUtils
{
	@OnlyIn(Dist.CLIENT)
	public static final CommonUtils.ReseteableMemoizedFunction<Stage, MatchCameraPositions> matchStartCameraPosProvider =
		CommonUtils.memoizeResetable((stage) ->
			{
				ClientLevel world = getClient().level;
				// if the current world isn't the same as the stage's world, do nothing, we are the client, and thus we cant
				// retrieve other worlds :(
				if (world.dimension() != stage.worldKey)
					return MatchCameraPositions.INVALID;
				
				// gets the stage's (horizontal) center and find the highest y
				AABB bounds = stage.getBounds();
				int stageCenterX = (int) bounds.getCenter().x;
				int stageCenterZ = (int) bounds.getCenter().z;
				int minY = (int) bounds.minY;
				int maxY = (int) bounds.maxY;
				
				CameraPosition stageFloorCenter = new CameraPosition(new Vec3(
					stageCenterX,
					Optional.ofNullable(TurfScannerItem.getTopSolidOrLiquidBlock(stageCenterX, stageCenterZ, world, minY, maxY))
						.map(v -> (float) v.getY()).orElse((float) minY) + 2,
					stageCenterZ
				), 0.0f, 90.0f);
				
				CameraPosition stageTopCenter = new CameraPosition(
					stageFloorCenter.position.add(0, 25, 0),
					0.0f, 90.0f
				);
				
				ImmutableList.Builder<CameraPosition> positions = ImmutableList.builder();
				// spawn pads
				Map<InkColor, List<SpawnPadTileEntity>> spawnPadPositions = stage.getSpawnPads(world);
				
				// put the current client's spawn pad as the first!!! this breaks if there are multiple spawn pads of the same color tho
				InkColor clientPlayerColor = ColorUtils.getEntityColor(getClientPlayer());
				List<SpawnPadTileEntity> clientSpawnPads = spawnPadPositions.get(clientPlayerColor);
				
				if (clientSpawnPads != null)
				{
					spawnPadPositions.remove(clientPlayerColor);
					
					SpawnPadTileEntity randomClientPad = Util.getRandom(clientSpawnPads, world.random);
					
					addPadToList(randomClientPad, stageFloorCenter, positions);
				}
				
				for (Map.Entry<InkColor, List<SpawnPadTileEntity>> spawnPadEntrySet : spawnPadPositions.entrySet())
				{
					SpawnPadTileEntity randomPad = Util.getRandom(spawnPadEntrySet.getValue(), world.random);
					
					addPadToList(randomPad, stageFloorCenter, positions);
				}
				
				return new MatchCameraPositions(stageFloorCenter, stageTopCenter, positions.build());
			}
		);
	@OnlyIn(Dist.CLIENT)
	public static Pair<UUID, Vector2f> killCamData;
	private static void addPadToList(SpawnPadTileEntity spawnPad, CameraPosition stageFloorCenter, ImmutableList.Builder<CameraPosition> posAndRotations)
	{
		Vec3 spawnPadCenter = spawnPad.getSuperJumpPos().add(0, 3, 0);
		Vec3 dirCenterToPad = spawnPadCenter.subtract(stageFloorCenter.position).normalize();
		Vec3 lookPosition = spawnPadCenter.subtract(dirCenterToPad.scale(2)).subtract(0, 2, 0);
		float pitch = (float) (Mth.atan2(dirCenterToPad.y, dirCenterToPad.horizontalDistance()) * Mth.RAD_TO_DEG);
		float yaw = (float) (Mth.atan2(dirCenterToPad.x, dirCenterToPad.z) * Mth.RAD_TO_DEG);
		
		posAndRotations.add(new CameraPosition(lookPosition, -yaw, -pitch));
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
		if (chestpiece.has(SplatcraftComponents.TANK_DATA))
		{
			return InkTankItem.getInkPercentage(chestpiece);
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
	public static void setSquid(LivingEntity entity, boolean newSquid)
	{
		setSquid(entity, newSquid, false);
	}
	public static void setSquid(LivingEntity entity, boolean newSquid, boolean checkChargeStorage)
	{
		SquidFormHandler.setSquid(entity, newSquid);
		
		SplatcraftPacketHandler.sendToServer(new PlayerSetSquidC2SPacket(newSquid, checkChargeStorage));
	}
	@OnlyIn(Dist.CLIENT)
	public static Minecraft getClient()
	{
		return Minecraft.getInstance();
	}
	@OnlyIn(Dist.CLIENT)
	public static MatchCameraPositions getMatchIntroData(Stage stage)
	{
		return matchStartCameraPosProvider.apply(stage);
	}
	public record MatchCameraPositions(CameraPosition floorStart, CameraPosition birdsEye,
	                                   List<CameraPosition> spawnPads)
	{
		public static final MatchCameraPositions INVALID = new MatchCameraPositions(CameraPosition.INVALID, CameraPosition.INVALID, List.of());
	}
	public record CameraPosition(Vec3 position, float yaw, float pitch)
	{
		public static final CameraPosition INVALID = new CameraPosition(null, Float.NaN, Float.NaN);
		public static CameraPosition lerp(CameraPosition pos1, CameraPosition pos2, float value)
		{
			return new CameraPosition(pos1.position.lerp(pos2.position, value), Mth.rotLerp(value, pos1.yaw, pos2.yaw), Mth.rotLerp(value, pos1.pitch, pos2.pitch));
		}
		public static CameraPosition lerp(CameraPosition pos1, CameraPosition pos2, float positionValue, float rotationValue)
		{
			return new CameraPosition(pos1.position.lerp(pos2.position, positionValue), Mth.rotLerp(rotationValue, pos1.yaw, pos2.yaw), Mth.rotLerp(rotationValue, pos1.pitch, pos2.pitch));
		}
		public static CameraPosition from(Player player)
		{
			return new CameraPosition(player.getEyePosition(), player.getYRot(), player.getXRot());
		}
		public static CameraPosition from(Player player, float partialTicks)
		{
			return new CameraPosition(player.getEyePosition(partialTicks), player.getViewYRot(partialTicks), player.getViewXRot(partialTicks));
		}
		public CameraPosition withPitch(float pitch)
		{
			return new CameraPosition(position, yaw, pitch);
		}
		public CameraPosition withYaw(float yaw)
		{
			return new CameraPosition(position, yaw, pitch);
		}
		public void applyTransformations(TriConsumer<Double, Double, Double> positionConsumer, BiConsumer<Float, Float> rotationConsumer)
		{
			if (position != null)
				positionConsumer.accept(position.x, position.y, position.z);
			if (!Float.isNaN(yaw) && !Float.isNaN(pitch))
				rotationConsumer.accept(yaw, pitch);
		}
		public CameraPosition add(double x, double y, double z)
		{
			return new CameraPosition(position.add(x, y, z), yaw, pitch);
		}
	}
}