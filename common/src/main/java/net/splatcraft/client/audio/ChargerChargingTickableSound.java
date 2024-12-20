package net.splatcraft.client.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.data.capabilities.playerinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.util.PlayerCharge;

public class ChargerChargingTickableSound extends MovingSoundInstance
{
    private final PlayerEntity player;

    public ChargerChargingTickableSound(PlayerEntity player, SoundEvent sound)
    {
        super(sound, SoundCategory.PLAYERS, player.getRandom());
        attenuationType = AttenuationType.NONE;
        repeat = true;
        repeatDelay = 0;

        this.player = player;
    }

    @Override
    public boolean shouldAlwaysPlay()
    {
        return true;
    }

    @Override
    public void tick()
    {
        x = player.getX();
        y = player.getY();
        z = player.getZ();

        if (player.isAlive() && player.getActiveItem().getItem() instanceof IChargeableWeapon && EntityInfoCapability.hasCapability(player))
        {
            EntityInfo info = EntityInfoCapability.get(player);
            if (!info.isSquid() && PlayerCharge.chargeMatches(player, player.getActiveItem()))
            {
                float charge = PlayerCharge.getChargeValue(player, player.getActiveItem());
                float prevCharge = info.getPlayerCharge().prevCharge;

                if (charge >= info.getPlayerCharge().totalCharges && !isDone())
                {
                    setDone();
                    return;
                }

                pitch = (MathHelper.lerp(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true), prevCharge, charge) / info.getPlayerCharge().totalCharges) * 0.5f + 0.5f;
                return;
            }
        }
        setDone();
    }
}
