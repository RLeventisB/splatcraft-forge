package net.splatcraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3d;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.PlayerSetSquidS2CPacket;
import net.splatcraft.network.s2c.UpdatePlayerInfoPacket;
import net.splatcraft.registries.SplatcraftAttributes;
import net.splatcraft.registries.SplatcraftGameRules;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.ColorUtils;
import net.splatcraft.util.PlayerCooldown;
import org.jetbrains.annotations.Nullable;

public class SuperJumpCommand
{
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("superjump").requires(commandSource -> commandSource.hasPermission(2)).then(Commands.argument("location", Vec3Argument.vec3()).executes(context ->
            {
                Vec3d target = Vec3Argument.getVec3(context, "location");
                return executeLocation(context, new Vec3d(target.x(), target.y(), target.z()));
            })).then(Commands.argument("target", EntityArgument.entity()).executes(context ->
                executeLocation(context, EntityArgument.getEntity(context, "target").position())))
            .executes(SuperJumpCommand::executeSpawn));
    }

    private static int executeLocation(CommandContext<CommandSourceStack> context, Vec3d target) throws CommandSyntaxException
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
        if (player.getRespawnDimension().equals(player.getWorld().dimension()))
        {
            BlockPos targetPos = getSpawnPadPos(player);
            if (targetPos == null)
            {
                targetPos = new BlockPos(player.getWorld().getLevelData().getXSpawn(), player.getWorld().getLevelData().getYSpawn(), player.getWorld().getLevelData().getZSpawn());
            }

            superJump(player, new Vec3d(targetPos.getX(), targetPos.getY() + blockHeight(targetPos, player.level()), targetPos.getZ()), global);
            return true;
        }

        return false;
    }

    @Nullable
    public static BlockPos getSpawnPadPos(ServerPlayer player)
    {
        BlockPos targetPos = player.getRespawnPosition();
        if (targetPos == null || player.getWorld().getBlockEntity(targetPos) instanceof SpawnPadTileEntity spawnpad && !ColorUtils.colorEquals(player, spawnpad))
            return null;

        return targetPos;
    }

    public static boolean superJump(ServerPlayer player, Vec3d target)
    {
        return superJump(player, target, SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.blockPosition(), SplatcraftGameRules.GLOBAL_SUPERJUMPING));
    }

    public static boolean superJump(ServerPlayer player, Vec3d target, boolean global)
    {
        return superJump(player, target,
            (int) player.getAttribute(SplatcraftAttributes.superJumpTravelTime.get()).getValue(),
            (int) player.getAttribute(SplatcraftAttributes.superJumpWindupTime.get()).getValue(),
            player.getAttribute(SplatcraftAttributes.superJumpHeight.get()).getValue(),
            global);
    }

    public static boolean superJump(ServerPlayer player, Vec3d target, int windupTime, int travelTime, double jumpHeight, boolean global)
    {
        if (!global && !canSuperJumpTo(player, target))
            return false;

        PlayerCooldown.setPlayerCooldown(player, new SuperJump(player.position(), target, windupTime, travelTime, jumpHeight, player.noPhysics, player.getAbilities().invulnerable));

        PlayerInfo info = PlayerInfoCapability.get(player);
        if (!info.isSquid())
        {
            info.setIsSquid(true);
            SplatcraftPacketHandler.sendToTrackers(new PlayerSetSquidS2CPacket(player.getUUID(), info.isSquid()), player);
        }

        SplatcraftPacketHandler.sendToPlayer(new UpdatePlayerInfoPacket(player), player);

        return true;
    }

    public static boolean canSuperJumpTo(Player player, Vec3d target)
    {
        int jumpLimit = SplatcraftGameRules.getIntRuleValue(player.getWorld(), SplatcraftGameRules.SUPERJUMP_DISTANCE_LIMIT);
        if (Stage.targetsOnSameStage(player.getWorld(), player.position(), target) || jumpLimit < 0 || player.position().distanceTo(target) <= jumpLimit)
        {
            PlayerCooldown cooldown = PlayerCooldown.getPlayerCooldown(player);
            return !(cooldown instanceof SuperJump);
        }
        return false;
    }

    public static double blockHeight(BlockPos block, Level level)
    {
        VoxelShape shape = level.getBlockState(block).getShape(level, block);
        if (shape.isEmpty())
            return 0;
        else
            return shape.bounds().getYsize();
    }

    public static class SuperJump extends PlayerCooldown
    {
        final int travelTime;
        final int windupTime;
        final double height;
        final Vec3d target;
        Vec3d source;
        boolean hadPhysics, canStart, hadInvulnerability;

        public SuperJump(Vec3d source, Vec3d target, int travelTime, int windupTime, double height, boolean hadPhysics, boolean hadInvulnerability)
        {
            super(ItemStack.EMPTY, travelTime + windupTime, -1, InteractionHand.MAIN_HAND, false, false, false, false);
            this.target = target;
            this.source = source;
            this.hadPhysics = hadPhysics;
            this.hadInvulnerability = hadInvulnerability;
            this.travelTime = travelTime;
            this.windupTime = windupTime;
            this.height = height;
        }

        public SuperJump(NbtCompound nbt)
        {
            this(new Vec3d(nbt.getDouble("SourceX"), nbt.getDouble("SourceY"), nbt.getDouble("SourceZ")),
                new Vec3d(nbt.getDouble("TargetX"), nbt.getDouble("TargetY"), nbt.getDouble("TargetZ")),
                nbt.getInt("TravelTime"), nbt.getInt("WindupTime"),
                nbt.getDouble("Height"), nbt.getBoolean("CanClip"), nbt.getBoolean("HadInvulnerability"));
            setTime(nbt.getFloat("TimeLeft"));
        }

        public static double getSuperJumpYPos(double progress, double startY, double endY, double arcHeight)
        {
            return arcHeight * Math.sin(progress * Math.PI) + ((endY - startY) * (progress) + startY);
        }

        @Override
        public void tick(Player player)
        {
            if (!canStart)
            {
                if (!player.onGround())
                {
                    player.getAbilities().flying = false;
                    setTime(getTime() + 1);
                    return;
                }
                source = player.position();
                canStart = true;
            }
            if (getTime() > getTravelTime()) // windup
            {

            }
            else
            {
                if (getTime() == getTravelTime())
                {
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.superjumpStart, SoundSource.PLAYERS, 0.8F, 1);
                }
                float progress = getSuperJumpProgress(0);
                float oldProgress = getSuperJumpProgress(1);

                // i put () in every coordinate because java is doing magic bullshit again and somewhere in the code target is being set as source and i question how the fuck does that happen
                // NEVERMIND SOURCE AND TARGET WERE REVERSED WHAT THE HELL
                Vec3d nextPos = new Vec3d(MathHelper.lerp(progress, source.x(), target.x()), getSuperJumpYPos(progress, source.y(), target.y(), getHeight()), MathHelper.lerp(progress, source.z(), target.z()));
                Vec3d oldPos = new Vec3d(MathHelper.lerp(oldProgress, source.x(), target.x()), getSuperJumpYPos(oldProgress, source.y(), target.y(), getHeight()), MathHelper.lerp(oldProgress, source.z(), target.z()));
                // just in case setDeltaMovement had some weird application or something
                player.setDeltaMovement(nextPos.subtract(oldPos));
                player.setPos(oldPos);
                player.getAbilities().invulnerable = true;
            }
            player.getAbilities().flying = true;
            player.noPhysics = true;
            player.fallDistance = 0;
        }

        @Override
        public void onEnd(Player player)
        {
            player.setPos(target);
            player.noPhysics = hadPhysics;
            player.getAbilities().invulnerable = hadInvulnerability;
            player.getAbilities().flying = false;
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.superjumpLand, SoundSource.PLAYERS, 0.8F, 1);
        }

        @Override
        public boolean preventWeaponUse()
        {
            return true;
        }

        @Override
        public NbtCompound writeNBT(NbtCompound nbt)
        {
            nbt.putDouble("TargetX", target.x);
            nbt.putDouble("TargetY", target.y);
            nbt.putDouble("TargetZ", target.z);
            nbt.putDouble("SourceX", source.x);
            nbt.putDouble("SourceY", source.y);
            nbt.putDouble("SourceZ", source.z);
            nbt.putDouble("Height", height);
            nbt.putBoolean("SuperJump", true);
            nbt.putBoolean("CanClip", hadPhysics);
            nbt.putBoolean("HadInvulnerability", hadInvulnerability);
            nbt.putFloat("TimeLeft", getTime());
            nbt.putInt("WindupTime", getWindupTime());
            nbt.putInt("TravelTime", getTravelTime());
            return nbt;
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