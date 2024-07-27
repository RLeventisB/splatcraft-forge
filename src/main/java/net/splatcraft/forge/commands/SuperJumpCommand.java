package net.splatcraft.forge.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.forge.network.s2c.UpdatePlayerInfoPacket;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.CommonUtils;
import net.splatcraft.forge.util.PlayerCooldown;
import org.jetbrains.annotations.NotNull;

public class SuperJumpCommand
{
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("superjump").requires(commandSource -> commandSource.hasPermission(2))
			.then(Commands.argument("to", BlockPosArgument.blockPos())
				.executes(context ->
				{
					BlockPos target = BlockPosArgument.getSpawnablePos(context, "to");
					return execute(context, new Vec3(target.getX() + .5d, target.getY(), target.getZ() + .5d), 80);
				})
				.then(Commands.argument("duration", IntegerArgumentType.integer(1))
					.executes(context ->
					{
						BlockPos target = BlockPosArgument.getSpawnablePos(context, "to");
						return execute(context, new Vec3(target.getX() + .5d, target.getY(), target.getZ() + .5d), IntegerArgumentType.getInteger(context, "duration"));
					})
				))
			.then(Commands.argument("target", EntityArgument.entity())
				.executes(context -> execute(context, EntityArgument.getEntity(context, "target").position(), 80))
				.then(Commands.argument("duration", IntegerArgumentType.integer(0))
					.executes(context -> execute(context, EntityArgument.getEntity(context, "target").position(), IntegerArgumentType.getInteger(context, "duration")))))
		
		);
	}
	private static int execute(CommandContext<CommandSourceStack> context, Vec3 target, int duration) throws CommandSyntaxException
	{
		ServerPlayer player = context.getSource().getPlayerOrException();
		
		PlayerCooldown.setPlayerCooldown(player, new SuperJump(player.getInventory().selected, target, player.position(), duration, player.noPhysics));

//		player.displayClientMessage(Component.literal("pchoooooo"), false); // me too
		SplatcraftPacketHandler.sendToPlayer(new UpdatePlayerInfoPacket(player), player);
		
		return 0;
	}
	public static class SuperJump extends PlayerCooldown
	{
		final Vec3 target, origin;
		boolean oldNoClip;
		public final static double PrepareFraction = 0.75, SquidPortion = 0.2;
		public SuperJump(int slotIndex, Vec3 target, Vec3 from, int duration, boolean couldClip)
		{
			super(ItemStack.EMPTY, duration, slotIndex, InteractionHand.MAIN_HAND, false, false, true, false);
			this.origin = from;
			this.target = target;
			this.oldNoClip = couldClip;
		}
		public SuperJump(CompoundTag nbt)
		{
			super(ItemStack.EMPTY, nbt.getInt("MaxTime"), nbt.getInt("SlotIndex"), InteractionHand.MAIN_HAND, false, false, true, false);
			setTime(nbt.getInt("Time"));
			this.origin = new Vec3(nbt.getDouble("TargetX"), nbt.getDouble("TargetY"), nbt.getDouble("TargetZ"));
			this.target = new Vec3(nbt.getDouble("OriginX"), nbt.getDouble("OriginY"), nbt.getDouble("OriginZ"));
			this.oldNoClip = nbt.getBoolean("CouldClip");
			fromNbt(nbt);
		}
		@Override
		public void tick(Player player)
		{
			player.getAbilities().flying = false;
			PlayerInfo info = PlayerInfoCapability.get(player);
			
			if (getTime() == 1) // landed
			{
				player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.superjumpLand, SoundSource.PLAYERS, 0.8F, 1);
				
				player.moveTo(target);
				player.noPhysics = oldNoClip;
				player.setInvulnerable(false);
				PlayerCooldown.setPlayerCooldown(player, null);
			}
			else if (getTime() > getMaxTime() * SuperJump.PrepareFraction) // preparing jump
			{
				if (!player.onGround())
				{
					setTime(getTime() + 1);
					return;
				}
				double d0 = target.x - player.position().x;
				double d1 = target.y - player.position().y;
				double d2 = target.z - player.position().z;
				double d3 = Math.sqrt(d0 * d0 + d2 * d2);
				player.setXRot(CommonUtils.lerpRotation(0.2f, player.getXRot(), Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * Mth.RAD_TO_DEG)))));
				player.setYRot(CommonUtils.lerpRotation(0.2f, player.getYRot(), Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * Mth.RAD_TO_DEG) - 90.0F)));
				player.setYHeadRot(player.getYRot());
				if (!info.isSquid())
				{
					info.setIsSquid(true);
					if (!player.level().isClientSide())
					{
						SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUUID(), info.isSquid()), player);
					}
				}
			}
			else // actually jumping
			{
				if (getTime() == (int) (getMaxTime() * SuperJump.PrepareFraction))
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.superjumpStart, SoundSource.PLAYERS, 0.8F, 1);
				
				player.stopFallFlying();
				player.noPhysics = true;
				player.fallDistance = 0;
				player.setInvulnerable(true);
				
				double progress = (double) getTime() / getMaxTime() * SuperJump.PrepareFraction;
				double nextProgress = (double) (getTime() - 1) / getMaxTime() * SuperJump.PrepareFraction;
				
				// serious question why does this use first parameter as the interpolation value
				Vec3 position = getSuperJumpPosition(progress);
				player.setDeltaMovement(getSuperJumpPosition(nextProgress).subtract(player.position()));
				
				if (!player.level().isClientSide())
				{
					player.moveTo(position);
//						player.setXRot((float) Math.sin(progress * Math.PI) * 40f);
				}
			}
			if (getTime() == (int) (getMaxTime() * SuperJump.SquidPortion)) // mid jump conversion
			{
				info.setIsSquid(false);
				if (!player.level().isClientSide())
				{
					SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUUID(), info.isSquid()), player);
				}
			}
		}
		@NotNull
		public Vec3 getSuperJumpPosition(double progress)
		{
			final double jumpHeight = 28;
			final double actualJumpCoeficient = 4 * jumpHeight;
			
			return new Vec3(Mth.lerp(progress, target.x, origin.x), Mth.lerp(progress, target.y, origin.y) - actualJumpCoeficient * progress * progress + actualJumpCoeficient * progress, Mth.lerp(progress, target.z, origin.z));
		}
		@Override
		public CompoundTag writeNBT(CompoundTag nbt)
		{
			nbt.putInt("Time", getTime());
			nbt.putInt("MaxTime", getMaxTime());
			nbt.putInt("SlotIndex", getSlotIndex());
			
			nbt.putDouble("TargetX", target.x);
			nbt.putDouble("TargetY", target.y);
			nbt.putDouble("TargetZ", target.z);
			
			nbt.putDouble("OriginX", origin.x);
			nbt.putDouble("OriginY", origin.y);
			nbt.putDouble("OriginZ", origin.z);
			
			nbt.putBoolean("SuperJump", true);
			nbt.putBoolean("CouldClip", oldNoClip);
			
			return nbt;
		}
	}
}
