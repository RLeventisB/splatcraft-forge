package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.network.s2c.UpdateEntityInfoPacket;
import net.splatcraft.registries.SplatcraftAttributes;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.PlayerCooldown;
import org.jetbrains.annotations.Nullable;

public class SuperJumpCommand
{
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
	{
		dispatcher.register(CommandManager.literal("superjump").requires(commandSource -> commandSource.hasPermissionLevel(2)).then(CommandManager.argument("location", Vec3ArgumentType.vec3()).executes(context ->
			{
				Vec3d target = Vec3ArgumentType.getVec3(context, "location");
				return executeLocation(context, target);
			})).then(CommandManager.argument("target", EntityArgumentType.entity()).executes(context ->
				executeLocation(context, EntityArgumentType.getEntity(context, "target").getPos())))
			.executes(SuperJumpCommand::executeSpawn));
	}
	private static int executeLocation(CommandContext<ServerCommandSource> context, Vec3d target) throws CommandSyntaxException
	{
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		superJump(player, target, true);
		
		return 0;
	}
	private static int executeSpawn(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
		superJumpToSpawn(player, true);
		
		return 0;
	}
	public static boolean superJumpToSpawn(ServerPlayerEntity player, boolean global)
	{
		if (player.getSpawnPointDimension().equals(player.getWorld().getDimension()))
		{
			BlockPos targetPos = getSpawnPadPos(player);
			if (targetPos == null)
			{
				targetPos = player.getWorld().getLevelProperties().getSpawnPos();
			}
			
			superJump(player, new Vec3d(targetPos.getX(), targetPos.getY() + blockHeight(targetPos, player.getWorld()), targetPos.getZ()), global);
			return true;
		}
		
		return false;
	}
	@Nullable
	public static BlockPos getSpawnPadPos(ServerPlayerEntity player)
	{
		BlockPos targetPos = player.getSpawnPointPosition();
		if (targetPos == null || player.getWorld().getBlockEntity(targetPos) instanceof SpawnPadTileEntity spawnpad && !ColorUtils.colorEquals(player, spawnpad))
			return null;
		
		return targetPos;
	}
	public static boolean superJump(ServerPlayerEntity player, Vec3d target)
	{
		return superJump(player, target, SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.getBlockPos(), SplatcraftGameRules.GLOBAL_SUPERJUMPING));
	}
	public static boolean superJump(ServerPlayerEntity player, Vec3d target, boolean global)
	{
		return superJump(player, target,
			(int) player.getAttributeValue(SplatcraftAttributes.superJumpTravelTime),
			(int) player.getAttributeValue(SplatcraftAttributes.superJumpWindupTime),
			player.getAttributeValue(SplatcraftAttributes.superJumpHeight),
			global);
	}
	public static boolean superJump(ServerPlayerEntity player, Vec3d target, int windupTime, int travelTime, double jumpHeight, boolean global)
	{
		if (!global && !canSuperJumpTo(player, target))
			return false;
		
		PlayerCooldown.setPlayerCooldown(player, new SuperJump(player.getPos(), target, windupTime, travelTime, jumpHeight, player.noClip, player.getAbilities().invulnerable));
		
		EntityInfo info = EntityInfoCapability.get(player);
		if (!info.isSquid())
		{
			info.setIsSquid(true);
			SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUuid(), info.isSquid()), player);
		}
		
		SplatcraftPacketHandler.sendToPlayer(new UpdateEntityInfoPacket(player), player);
		
		return true;
	}
	public static boolean canSuperJumpTo(PlayerEntity player, Vec3d target)
	{
		int jumpLimit = SplatcraftGameRules.getIntRuleValue(player.getWorld(), SplatcraftGameRules.SUPERJUMP_DISTANCE_LIMIT);
		if (Stage.targetsOnSameStage(player.getWorld(), player.getPos(), target) || jumpLimit < 0 || player.getPos().distanceTo(target) <= jumpLimit)
		{
			PlayerCooldown cooldown = PlayerCooldown.getPlayerCooldown(player);
			return !(cooldown instanceof SuperJump);
		}
		return false;
	}
	public static double blockHeight(BlockPos block, World world)
	{
		VoxelShape shape = world.getBlockState(block).getCollisionShape(world, block);
		if (shape.isEmpty())
			return 0;
		else
			return shape.getBoundingBox().getLengthY();
	}
	public static class SuperJump extends PlayerCooldown
	{
		public static Codec<SuperJump> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Vec3d.CODEC.fieldOf("start").forGetter(v -> v.start),
			Vec3d.CODEC.fieldOf("end").forGetter(v -> v.end),
			Codec.INT.fieldOf("travel_time").forGetter(v -> v.travelTime),
			Codec.INT.fieldOf("windup_time").forGetter(v -> v.windupTime),
			Codec.DOUBLE.fieldOf("jump_height").forGetter(v -> v.height),
			Codec.BOOL.fieldOf("had_physics").forGetter(v -> v.hadPhysics),
			Codec.BOOL.fieldOf("had_invulnerability").forGetter(v -> v.hadInvulnerability),
			Codec.BOOL.fieldOf("can_start").forGetter(v -> v.canStart)
		).apply(inst, SuperJump::new));
		final Vec3d end;
		final int travelTime;
		final int windupTime;
		final double height;
		Vec3d start;
		boolean hadPhysics, hadInvulnerability, canStart;
		public SuperJump(Vec3d start, Vec3d end, int travelTime, int windupTime, double height, boolean hadPhysics, boolean hadInvulnerability)
		{
			super(ItemStack.EMPTY, travelTime + windupTime, -1, Hand.MAIN_HAND, false, false, false, false);
			this.end = end;
			this.start = start;
			this.hadPhysics = hadPhysics;
			this.hadInvulnerability = hadInvulnerability;
			this.travelTime = travelTime;
			this.windupTime = windupTime;
			this.height = height;
		}
		public SuperJump(Vec3d start, Vec3d end, int travelTime, int windupTime, double height, boolean hadPhysics, boolean hadInvulnerability, boolean canStart)
		{
			this(start, end, travelTime, windupTime, height, hadPhysics, hadInvulnerability);
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
				if (!entity.isOnGround())
				{
					if (entity instanceof PlayerEntity player)
						player.getAbilities().flying = false;
					setTime(getTime() + 1);
					return;
				}
				start = entity.getPos();
				canStart = true;
			}
			if (getTime() > getTravelTime()) // windup
			{
			
			}
			else
			{
				if (getTime() == getTravelTime())
				{
					entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.superjumpStart, SoundCategory.PLAYERS, 0.8F, 1);
				}
				float progress = getSuperJumpProgress(0);
				float oldProgress = getSuperJumpProgress(1);
				
				// i put () in every coordinate because java is doing magic bullshit again and somewhere in the code target is being set as source and i question how the fuck does that happen
				// NEVERMIND SOURCE AND TARGET WERE REVERSED WHAT THE HELL
				Vec3d nextPos = new Vec3d(MathHelper.lerp(progress, start.x, end.x), getSuperJumpYPos(progress, start.y, end.y, getHeight()), MathHelper.lerp(progress, start.z, end.z));
				Vec3d oldPos = new Vec3d(MathHelper.lerp(oldProgress, start.x, end.x), getSuperJumpYPos(oldProgress, start.y, end.y, getHeight()), MathHelper.lerp(oldProgress, start.z, end.z));
				// just in case setVelocity had some weird application or something
				entity.setVelocity(nextPos.subtract(oldPos));
				entity.setPosition(oldPos);
				if (entity instanceof PlayerEntity player)
					player.getAbilities().invulnerable = true;
			}
			if (entity instanceof PlayerEntity player)
				player.getAbilities().flying = true;
			entity.noClip = true;
			entity.fallDistance = 0;
		}
		@Override
		public void onEnd(LivingEntity entity)
		{
			entity.setPosition(end);
			entity.noClip = hadPhysics;
			if (entity instanceof PlayerEntity player)
			{
				player.getAbilities().invulnerable = hadInvulnerability;
				player.getAbilities().flying = false;
			}
			entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.superjumpLand, SoundCategory.PLAYERS, 0.8F, 1);
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
			return 1f - MathHelper.clamp((getTime() + add) / (float) getTravelTime(), 0, 1);
		}
		public boolean isSquid()
		{
			return getSuperJumpProgress(0) > 0.2f;
		}
		public double getHeight()
		{
			return height;
		}
	}
}