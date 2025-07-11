package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.data.EntitySlot;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.network.s2c.UpdateEntityInfoPacket;
import net.splatcraft.registries.SplatcraftAttributes;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.EntityActionWithTime;
import org.jetbrains.annotations.Nullable;

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
		
		EntityInfo info = EntityInfoCapability.get(player);
		if (!info.isSquid())
		{
			info.setIsSquid(true);
			SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUUID(), info.isSquid()), player);
		}
		
		SplatcraftPacketHandler.sendToPlayer(new UpdateEntityInfoPacket(player), player);
		
		return true;
	}
	public static boolean canSuperJumpTo(LivingEntity entity, Vec3 target)
	{
		int jumpLimit = SplatcraftGameRules.getIntRuleValue(entity.level(), SplatcraftGameRules.SUPERJUMP_DISTANCE_LIMIT);
		if (Stage.targetsOnSameStage(entity.level(), entity.position(), target) || jumpLimit < 0 || entity.position().distanceTo(target) <= jumpLimit)
		{
			EntityAction action = EntityAction.getEntityAction(entity);
			return !(action instanceof SuperJump);
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
	public static class SuperJump extends EntityActionWithTime
	{
		public static Codec<SuperJump> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Vec3.CODEC.fieldOf("start").forGetter(v -> v.start),
			Vec3.CODEC.fieldOf("end").forGetter(v -> v.end),
			Codec.INT.fieldOf("windup_time").forGetter(v -> v.windupTime),
			Codec.INT.fieldOf("travel_time").forGetter(v -> v.travelTime),
			Codec.DOUBLE.fieldOf("jump_height").forGetter(v -> v.height),
			Codec.BOOL.fieldOf("had_physics").forGetter(v -> v.hadPhysics),
			Codec.BOOL.fieldOf("had_invulnerability").forGetter(v -> v.hadInvulnerability),
			Codec.BOOL.fieldOf("can_start").forGetter(v -> v.canStart),
			getTimeCodec()
		).apply(inst, SuperJump::new));
		final Vec3 end;
		final int travelTime;
		final int windupTime;
		final double height;
		Vec3 start;
		final boolean hadPhysics, hadInvulnerability;
		boolean canStart;
		public SuperJump(Vec3 start, Vec3 end, int windupTime, int travelTime, double height, boolean hadPhysics, boolean hadInvulnerability)
		{
			this(start, end, windupTime, travelTime, height, hadPhysics, hadInvulnerability, false);
		}
		public SuperJump(Vec3 start, Vec3 end, int windupTime, int travelTime, double height, boolean hadPhysics, boolean hadInvulnerability, boolean skipGroundCheck)
		{
			super(travelTime + windupTime);
			this.end = end;
			this.start = start;
			this.hadPhysics = hadPhysics;
			this.hadInvulnerability = hadInvulnerability;
			this.travelTime = travelTime;
			this.windupTime = windupTime;
			this.height = height;
			canStart = skipGroundCheck;
		}
		public SuperJump(Vec3 start, Vec3 end, int windupTime, int travelTime, double height, boolean hadPhysics, boolean hadInvulnerability, boolean canStart, float time)
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
		}
		public static double getSuperJumpYPos(double progress, double startY, double endY, double arcHeight)
		{
			return arcHeight * Math.sin(progress * Math.PI) + ((endY - startY) * (progress) + startY);
		}
		@Override
		public void tick(LivingEntity entity)
		{
			if (!canStart)
			{
				if (!entity.onGround())
				{
					if (entity instanceof Player player)
						player.getAbilities().flying = false;
					setTime(getTime() + 1);
					return;
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
		}
		@Override
		public boolean canEnd(LivingEntity entity)
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
			return true;
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
		public float getSuperJumpProgress(float add)
		{
			return 1f - Mth.clamp((getTime() + add) / (float) getTravelTime(), 0, 1);
		}
		public boolean isSquid()
		{
			return getSuperJumpProgress(0) > 0.2f;
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
	}
}