package net.splatcraft.client.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.player.Player;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.util.PlayerCharge;

public class ChargerChargingTickableSound extends AbstractTickableSoundInstance
{
    private final Player player;

    public ChargerChargingTickableSound(Player player, SoundEvent sound)
    {
        super(sound, SoundSource.PLAYERS, player.getRandom());
        this.attenuation = Attenuation.NONE;
        this.looping = true;
        this.delay = 0;

        this.player = player;
    }

    @Override
    public boolean canStartSilent()
    {
        return true;
    }

    @Override
    public void tick()
    {
        x = player.getX();
        y = player.getY();
        z = player.getZ();

        if (player.isAlive() && player.getUseItem().getItem() instanceof IChargeableWeapon && PlayerInfoCapability.hasCapability(player))
        {
            PlayerInfo info = PlayerInfoCapability.get(player);
            if (!info.isSquid() && PlayerCharge.chargeMatches(player, player.getUseItem()))
            {
                float charge = PlayerCharge.getChargeValue(player, player.getUseItem());
                float prevCharge = info.getPlayerCharge().prevCharge;

                if (charge >= info.getPlayerCharge().totalCharges && !isStopped())
                {
                    stop();
                    return;
                }

                pitch = (MathHelper.lerp(Minecraft.getInstance().getDeltaFrameTime(), prevCharge, charge) / info.getPlayerCharge().totalCharges) * 0.5f + 0.5f;
                return;
            }
        }
        stop();
    }
}
