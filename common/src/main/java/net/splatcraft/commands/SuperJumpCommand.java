package net.splatcraft.commands;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.Splatcraft;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.Stage;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.network.s2c.UpdateEntityActionOnlyPacket;
import net.splatcraft.platform.Components;
import net.splatcraft.registries.SplatcraftAttributes;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.action.*;
import net.splatcraft.util.structs.InkColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;

public class SuperJumpCommand
{
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("superjump").requires(commandSource -> commandSource.hasPermission(2)).then(Commands.argument("location", Vec3Argument.vec3()).executes(context ->
			{
				Vec3 target = Vec3Argument.getVec3(context, "location");
				return executeLocation(context, target);
			})).then(Commands.argument("target", EntityArgument.entity()).executes(context ->
				executeLocation(context, EntityArgument.getEntity(context, "target").position())))
			.executes(SuperJumpCommand::executeSpawn));
	}
	private static int executeLocation(CommandContext<CommandSourceStack> context, Vec3 target) throws CommandSyntaxException
	{
		ServerPlayer player = context.getSource().getPlayerOrException();
		superJump(player, target, true);
		
		return 0;
	}
	private static int executeSpawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		ServerPlayer player = context.getSource().getPlayerOrException();
		superJumpToSpawn(player, true);
		
		return 0;
	}
	public static boolean superJumpToSpawn(ServerPlayer player, boolean global)
	{
		if (player.getRespawnDimension().equals(player.level().dimensionType()))
		{
			BlockPos targetPos = getSpawnPadPos(player);
			if (targetPos == null)
			{
				targetPos = player.level().getLevelData().getSpawnPos();
			}
			
			superJump(player, new Vec3(targetPos.getX(), targetPos.getY() + blockHeight(targetPos, player.level()), targetPos.getZ()), global);
			return true;
		}
		
		return false;
	}
	@Nullable
	public static BlockPos getSpawnPadPos(ServerPlayer player)
	{
		BlockPos targetPos = player.getRespawnPosition();
		if (targetPos == null || player.level().getBlockEntity(targetPos) instanceof SpawnPadTileEntity spawnpad && !ColorUtils.colorEquals(player, spawnpad))
			return null;
		
		return targetPos;
	}
	public static boolean superJump(ServerPlayer player, Vec3 target)
	{
		return superJump(player, target, SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.GLOBAL_SUPERJUMPING));
	}
	public static boolean superJump(ServerPlayer player, Vec3 target, boolean global)
	{
		return superJump(player, target,
			(int) player.getAttributeValue(SplatcraftAttributes.superJumpWindupTime),
			(int) player.getAttributeValue(SplatcraftAttributes.superJumpTravelTime),
			player.getAttributeValue(SplatcraftAttributes.superJumpHeight),
			global);
	}
	public static boolean superJump(ServerPlayer player, Vec3 target, int windupTime, int travelTime, double jumpHeight, boolean global)
	{
		if (!global && !canSuperJumpTo(player, target))
			return false;
		
		EntityAction.setEntityAction(player, new SuperJump(player.position(), target, windupTime, travelTime, jumpHeight, player.noPhysics, player.getAbilities().invulnerable));
		
		if (Components.SQUID_INFO.hasChangedAfterUpdateOrCreate(player, info -> info.setSquid(true)))
		{
			SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUUID(), true), player);
		}
		
		SplatcraftPacketHandler.sendToPlayer(UpdateEntityActionOnlyPacket.create(player), player);
		
		return true;
	}
	public static boolean canSuperJumpTo(LivingEntity entity, Vec3 target)
	{
		int jumpLimit = SplatcraftGameRules.getIntRuleValue(entity.level(), SplatcraftGameRules.SUPERJUMP_DISTANCE_LIMIT);
		if (jumpLimit < 0 ||
			entity.position().distanceTo(target) <= jumpLimit ||
			Stage.targetsOnSameStage(entity.level(), entity.position(), target))
		{
			return !EntityAction.hasSpecificEntityAction(entity, SuperJump.class);
		}
		return false;
	}
	public static double blockHeight(BlockPos block, Level world)
	{
		VoxelShape shape = world.getBlockState(block).getCollisionShape(world, block);
		if (shape.isEmpty())
			return 0;
		else
			return shape.bounds().getYsize();
	}
	public static class SuperJump extends EntityActionWithTime implements ActionThatSetsSquid, RenderableEntityAction
	{
		public static final byte DEFAULT_ICON = 0;
		public static final byte JETPACK_ICON = 1;
		private static final ResourceLocation DEFAULT_ICON_TEXTURE = Splatcraft.identifierOf("textures/entity/superjump_marker.png");
		private static final ResourceLocation JETPACK_ICON_TEXTURE = Splatcraft.identifierOf("textures/entity/special/inkjet_start_icon.png");
		public static Codec<SuperJump> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Vec3.CODEC.fieldOf("start").forGetter(v -> v.start),
			Vec3.CODEC.fieldOf("end").forGetter(v -> v.end),
			Codec.INT.fieldOf("windup_time").forGetter(v -> v.windupTime),
			Codec.INT.fieldOf("travel_time").forGetter(v -> v.travelTime),
			Codec.DOUBLE.fieldOf("jump_height").forGetter(v -> v.height),
			Codec.BOOL.fieldOf("had_physics").forGetter(v -> v.hadPhysics),
			Codec.BOOL.fieldOf("had_invulnerability").forGetter(v -> v.hadInvulnerability),
			Codec.BOOL.fieldOf("can_start").forGetter(v -> v.canStart),
			Codec.BOOL.fieldOf("sets_squid").forGetter(v -> v.setsSquid),
			Codec.BYTE.fieldOf("recall_icon_id").forGetter(v -> v.recallIconId),
			getTimeCodec()
		).apply(inst, SuperJump::new));
		final Vec3 end;
		final int travelTime;
		final int windupTime;
		final double height;
		final byte recallIconId;
		Vec3 start;
		final boolean hadPhysics, hadInvulnerability, setsSquid;
		boolean canStart;
		public SuperJump(Vec3 start, Vec3 end, int windupTime, int travelTime, double height, boolean hadPhysics, boolean hadInvulnerability)
		{
			this(start, end, windupTime, travelTime, height, hadPhysics, hadInvulnerability, true, false, DEFAULT_ICON);
		}
		public SuperJump(Vec3 start, Vec3 end, int windupTime, int travelTime, double height, boolean hadPhysics, boolean hadInvulnerability, boolean setsSquid)
		{
			this(start, end, windupTime, travelTime, height, hadPhysics, hadInvulnerability, setsSquid, false, DEFAULT_ICON);
		}
		public SuperJump(Vec3 start, Vec3 end, int windupTime, int travelTime, double height, boolean hadPhysics, boolean hadInvulnerability, boolean setsSquid, boolean skipGroundCheck, byte recallIconId)
		{
			super(travelTime + windupTime);
			this.end = end;
			this.start = start;
			this.hadPhysics = hadPhysics;
			this.hadInvulnerability = hadInvulnerability;
			this.travelTime = travelTime;
			this.windupTime = windupTime;
			this.height = height;
			this.setsSquid = setsSquid;
			this.recallIconId = recallIconId;
			canStart = skipGroundCheck;
		}
		public SuperJump(Vec3 start, Vec3 end, int windupTime, int travelTime, double height, boolean hadPhysics, boolean hadInvulnerability, boolean canStart, boolean setsSquid, byte recallIconId, float time)
		{
			super(time, travelTime + windupTime);
			this.end = end;
			this.start = start;
			this.hadPhysics = hadPhysics;
			this.hadInvulnerability = hadInvulnerability;
			this.travelTime = travelTime;
			this.windupTime = windupTime;
			this.height = height;
			this.canStart = canStart;
			this.setsSquid = setsSquid;
			this.recallIconId = recallIconId;
		}
		public static double getSuperJumpYPos(double progress, double startY, double endY, double arcHeight)
		{
			return arcHeight * Math.sin(progress * Math.PI) + ((endY - startY) * (progress) + startY);
		}
		@Override
		public ActionEndResult tick(LivingEntity entity)
		{
			if (!canStart)
			{
				if (!entity.onGround())
				{
					if (entity instanceof Player player)
						player.getAbilities().flying = false;
					setTime(getTime() + 1);
					return null;
				}
				start = entity.position();
				canStart = true;
			}
			if (getTime() > getTravelTime()) // windup
			{
			
			}
			else
			{
				if (getTime() == getTravelTime())
				{
					WeaponHandler.disableMovedQuickly(entity, getTravelTime() + 10);
					entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.superjumpStart, SoundSource.PLAYERS, 0.8F, 1);
				}
				float progress = getSuperJumpProgress(0);
				float oldProgress = getSuperJumpProgress(1);
				
				// i put () in every coordinate because java is doing magic bullshit again and somewhere in the code target is being set as source and i question how the fuck does that happen
				// NEVERMIND SOURCE AND TARGET WERE REVERSED WHAT THE HELL
				Vec3 nextPos = new Vec3(Mth.lerp(progress, start.x, end.x), getSuperJumpYPos(progress, start.y, end.y, getHeight()), Mth.lerp(progress, start.z, end.z));
				Vec3 oldPos = new Vec3(Mth.lerp(oldProgress, start.x, end.x), getSuperJumpYPos(oldProgress, start.y, end.y, getHeight()), Mth.lerp(oldProgress, start.z, end.z));
				// just in case setVelocity had some weird application or something
				entity.setDeltaMovement(nextPos.subtract(oldPos));
				entity.setPos(oldPos);
				if (entity instanceof Player player)
					player.getAbilities().invulnerable = true;
			}
			if (entity instanceof Player player)
				player.getAbilities().flying = true;
			entity.noPhysics = true;
			entity.fallDistance = -100f;
			return ActionEndResult.dontEnd(this);
		}
		@Override
		public void beforeForcedEnd(LivingEntity entity)
		{
			entity.setPos(end);
			entity.noPhysics = hadPhysics;
			if (entity instanceof Player player)
			{
				player.getAbilities().invulnerable = hadInvulnerability;
				player.getAbilities().flying = false;
			}
			entity.fallDistance = -100f;
			entity.setDeltaMovement(0, 0, 0);
			entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.superjumpLand, SoundSource.PLAYERS, 0.8F, 1);
			WeaponHandler.forceLastGroundedPos(entity);
		}
		@Override
		public boolean canMove()
		{
			return false;
		}
		@Override
		public boolean preventWeaponUse()
		{
			return true;
		}
		public int getTravelTime()
		{
			return travelTime;
		}
		public int getWindupTime()
		{
			return windupTime;
		}
		public float getSuperJumpProgress(float partialTick)
		{
			return 1f - Mth.clamp((getTime() + partialTick) / (float) getTravelTime(), 0, 1);
		}
		public Optional<Boolean> isSquid(LivingEntity entity)
		{
			if (setsSquid)
				return Optional.of(getSuperJumpProgress(0) < 0.8f);
			return Optional.empty();
		}
		public double getHeight()
		{
			return height;
		}
		@Override
		public EntitySlot getItemSlot()
		{
			return super.getItemSlot();
		}
		@OnlyIn(Dist.CLIENT)
		@Override
		public void renderExtra(@NotNull PoseStack poseStack, @NotNull MultiBufferSource provider, LivingEntity entity, float partialTicks)
		{
			ResourceLocation iconLocation = switch (recallIconId)
			{
				case 0 -> DEFAULT_ICON_TEXTURE;
				case 1 -> JETPACK_ICON_TEXTURE;
				default -> null;
			};
			
			if (iconLocation == null)
				return;
			
			Quaternionf quaternion = new Quaternionf();
			Camera camera = ClientUtils.getClient().gameRenderer.getMainCamera();
			SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ.setRotation(quaternion, camera, partialTicks);
			InkColor entityColor = ColorUtils.getEntityColor(entity);
			if (entityColor.isInvalid())
			{
				return;
			}
			
			int color = ColorUtils.makeBrighter(entityColor);
			final int grayedOutColor = FastColor.ARGB32.color(128, 128, 128);
			final float size = 0.4f;
			
			VertexConsumer consumer = provider.getBuffer(RenderType.armorCutoutNoCull(iconLocation));
			Vector3f quadCenter = end.toVector3f().add(0, 0.5f, 0).sub(camera.getPosition().toVector3f());
			
			float progress = Math.min(1, getTime() / travelTime);
			float progressWithNegative = progress * 2f - 1;
			Vector3f top = new Vector3f(0, size, 0);
			Vector3f split = new Vector3f(0, size * progressWithNegative, 0);
			Vector3f bottom = new Vector3f(0, -size, 0);
			int splitUvHeight = (int) (16 * progress);
			
			consumer.addVertex(split.add(new Vector3f(size, 0f, 0f), new Vector3f()).rotate(quaternion).add(quadCenter)).setColor(color).setUv(0, 1f - progress).setUv1(0, 16 - splitUvHeight).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
			consumer.addVertex(bottom.add(new Vector3f(size, 0f, 0f), new Vector3f()).rotate(quaternion).add(quadCenter)).setColor(color).setUv(0, 1).setUv1(0, 16).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
			consumer.addVertex(bottom.add(new Vector3f(-size, 0, 0f), new Vector3f()).rotate(quaternion).add(quadCenter)).setColor(color).setUv(1, 1).setUv1(16, 16).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
			consumer.addVertex(split.add(new Vector3f(-size, 0f, 0f), new Vector3f()).rotate(quaternion).add(quadCenter)).setColor(color).setUv(1, 1f - progress).setUv1(16, 16 - splitUvHeight).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
			
			consumer.addVertex(top.add(new Vector3f(size, 0f, 0f), new Vector3f()).rotate(quaternion).add(quadCenter)).setColor(grayedOutColor).setUv(0, 0).setUv1(0, 0).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
			consumer.addVertex(split.add(new Vector3f(size, 0f, 0f), new Vector3f()).rotate(quaternion).add(quadCenter)).setColor(grayedOutColor).setUv(0, 1f - progress).setUv1(0, splitUvHeight).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
			consumer.addVertex(split.add(new Vector3f(-size, 0, 0f), new Vector3f()).rotate(quaternion).add(quadCenter)).setColor(grayedOutColor).setUv(1, 1f - progress).setUv1(16, splitUvHeight).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
			consumer.addVertex(top.add(new Vector3f(-size, 0f, 0f), new Vector3f()).rotate(quaternion).add(quadCenter)).setColor(grayedOutColor).setUv(1, 0).setUv1(16, 0).setNormal(1, 0, 0).setLight(LightTexture.FULL_BRIGHT);
		}
	}
}