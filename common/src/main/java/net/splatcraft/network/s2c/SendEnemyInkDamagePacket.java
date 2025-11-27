package net.splatcraft.network.s2c;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.splatcraft.client.audio.EnemyInkTickableSound;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

public class SendEnemyInkDamagePacket extends PlayS2CPacket
{
	public static final Type<? extends CustomPacketPayload> ID = CommonUtils.createIdFromClass(SendEnemyInkDamagePacket.class);
	public static EnemyInkTickableSound sound;
	final float health;
	public SendEnemyInkDamagePacket(float health)
	{
		this.health = health;
	}
	public static SendEnemyInkDamagePacket decode(RegistryFriendlyByteBuf buffer)
	{
		return new SendEnemyInkDamagePacket(buffer.readFloat());
	}
	@Override
	public @NotNull Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
	@Override
	public void encode(RegistryFriendlyByteBuf buffer)
	{
		buffer.writeFloat(health);
	}
	@OnlyIn(Dist.CLIENT)
	@Override
	public void execute()
	{
		Player target = ClientUtils.getClientPlayer();
		if (target != null)
		{
			target.setHealth(health);
			if (sound == null || sound.isStopped())
			{
				sound = new EnemyInkTickableSound(target, SplatcraftSounds.enemyInkDamage);
				synchronized (this)
				{
					Minecraft.getInstance().getSoundManager().play(sound);
				}
			}
			sound.makeLouder();
		}
	}
}
