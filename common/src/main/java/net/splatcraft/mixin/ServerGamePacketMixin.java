package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.util.PlayerCooldown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerGamePacketMixin
{
    //Hijacking move packet to prevent players from being kicked out for flying
    //Please forge make an event for this or something >_>
    // hello i come from future forge didn't do anything
    @WrapOperation(method = "onPlayerMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;isInTeleportationState()Z"))
    public boolean isChangingDimOrSuperjumping(ServerPlayerEntity player, Operation<Boolean> original)
    {
        return original.call(player) || PlayerCooldown.getPlayerCooldown(player) instanceof SuperJumpCommand.SuperJump;
    }

    @WrapOperation(method = "onPlayerMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;isUsingRiptide()Z"))
    public boolean isSpinninggOrSuperJumping(ServerPlayerEntity player, Operation<Boolean> original)
    {
        return original.call(player) || PlayerCooldown.getPlayerCooldown(player) instanceof SuperJumpCommand.SuperJump;
    }
}
