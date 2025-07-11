package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.splatcraft.commands.SuperJumpCommand;
import net.splatcraft.handlers.WeaponHandler;
import net.splatcraft.util.action.EntityAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketMixin
{
	//Hijacking move packet to prevent players from being kicked out for flying
	//Please forge make an event for this or something >_>
	// hello i come from future forge didn't do anything
	@WrapOperation(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isChangingDimension()Z"))
	public boolean isChangingDimOrSuperjumping(ServerPlayer player, Operation<Boolean> original)
	{
		return splatcraft$disableMovedQuickly(player, original);
	}
	@Unique
	private static boolean splatcraft$disableMovedQuickly(ServerPlayer player, Operation<Boolean> original)
	{
		return original.call(player) || EntityAction.hasSpecificEntityAction(player, SuperJumpCommand.SuperJump.class) || WeaponHandler.hasMovedQuicklyDisabled(player);
	}
	@WrapOperation(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isAutoSpinAttack()Z"))
	public boolean isSpinningOrSuperJumping(ServerPlayer player, Operation<Boolean> original)
	{
		return splatcraft$disableMovedQuickly(player, original);
	}
}
