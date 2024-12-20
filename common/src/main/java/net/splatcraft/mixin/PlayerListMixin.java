package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TeleportTarget;
import net.splatcraft.network.s2c.UpdateWeaponSettingsPacket;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.ColorUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerManager.class)
public abstract class PlayerListMixin
{
    @Unique
    private boolean splatcraft$canRespawn = true;

    @Shadow
    public abstract void sendToAll(Packet<?> packet);

    @Inject(method = "respawnPlayer", at = @At(value = "HEAD"))
    public void getRespawnPosition(ServerPlayerEntity player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayerEntity> cir)
    {
        BlockPos res = player.getSpawnPointPosition();

        if (res != null)
        {
            if (player.server.getWorld(player.getSpawnPointDimension()).getBlockEntity(res) instanceof SpawnPadTileEntity te)
            {
                // todo: forg- why are there two forge metods
//                player.reviveCaps();
                splatcraft$canRespawn = ColorUtils.colorEquals(player, te);
//                player.invalidateCaps();
                return;
            }
        }
        splatcraft$canRespawn = true;
    }

    @WrapOperation(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;getRespawnTarget(ZLnet/minecraft/world/TeleportTarget$PostDimensionTransition;)Lnet/minecraft/world/TeleportTarget;"))
    public TeleportTarget respawn(ServerPlayerEntity instance, boolean optional, TeleportTarget.PostDimensionTransition postDimensionTransition, Operation<TeleportTarget> original)
    {
        if (!splatcraft$canRespawn)
            return TeleportTarget.missingSpawnBlock(instance.getServer().getOverworld(), instance, postDimensionTransition);
        return original.call(instance, optional, postDimensionTransition);
    }

    @Inject(method = "onDataPacksReloaded", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;sendToAll(Lnet/minecraft/network/packet/Packet;)V"))
    public void splatcraft$onDatapackReload(CallbackInfo ci)
    {
        sendToAll(new CustomPayloadS2CPacket(new UpdateWeaponSettingsPacket()));
    }
}
