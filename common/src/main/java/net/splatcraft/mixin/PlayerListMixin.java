package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.splatcraft.data.InkColorGroup;
import net.splatcraft.data.InkColorRegistry;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.s2c.SendColorRegistryPacket;
import net.splatcraft.network.s2c.UpdateWeaponSettingsPacket;
import net.splatcraft.tileentities.SpawnPadTileEntity;
import net.splatcraft.util.ColorUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin
{
	@Unique
	private boolean splatcraft$canRespawn = true;
	@Inject(method = "respawn", at = @At(value = "HEAD"))
	public void getRespawnPosition(ServerPlayer player, boolean alive, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir)
	{
		BlockPos res = player.getRespawnPosition();
		
		if (res != null)
		{
			if (player.server.getLevel(player.getRespawnDimension()).getBlockEntity(res) instanceof SpawnPadTileEntity te)
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
	@WrapOperation(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
	public DimensionTransition respawn(ServerPlayer instance, boolean optional, DimensionTransition.PostDimensionTransition postDimensionTransition, Operation<DimensionTransition> original)
	{
		if (!splatcraft$canRespawn)
			return DimensionTransition.missingRespawnBlock(instance.getServer().overworld(), instance, postDimensionTransition);
		return original.call(instance, optional, postDimensionTransition);
	}
	@Inject(method = "reloadResources", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
	public void splatcraft$onDatapackReload(CallbackInfo ci)
	{
		SplatcraftPacketHandler.sendToAll(new UpdateWeaponSettingsPacket());
		SplatcraftPacketHandler.sendToAll(new SendColorRegistryPacket(InkColorRegistry.REGISTRY, InkColorGroup.getAllGroups()));
	}
}
