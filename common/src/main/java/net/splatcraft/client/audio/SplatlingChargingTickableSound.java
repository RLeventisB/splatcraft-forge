package net.splatcraft.client.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.data.capabilities.entityinfo.EntityInfo;
import net.splatcraft.data.capabilities.entityinfo.EntityInfoCapability;
import net.splatcraft.items.weapons.IChargeableWeapon;
import net.splatcraft.util.PlayerCharge;
import org.jetbrains.annotations.Nullable;

public class SplatlingChargingTickableSound extends MovingSoundInstance
{
    private static final int maxFadeTime = 30;
    private final PlayerEntity player;
    private final SoundEvent soundEvent;
    private int fadeTime = -1;
    private boolean isFadeIn = false;
    @Nullable
    private Boolean playingSecondLevel = null;

    public SplatlingChargingTickableSound(PlayerEntity player, SoundEvent sound)
    {
        super(sound, SoundCategory.PLAYERS, player.getRandom());
        attenuationType = AttenuationType.NONE;
        repeat = true;
        repeatDelay = 0;

        this.player = player;
        soundEvent = sound;
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

                if (playingSecondLevel == null)
                    playingSecondLevel = charge > 1;

                if (!isFadeIn && fadeTime == 0)
                {
                    setDone();
                    return;
                }
                else if (fadeTime > maxFadeTime)
                    fadeTime = -1;
                else if (fadeTime > 0)
                {
                    fadeTime += isFadeIn ? 1 : -1;
                    volume = fadeTime / (float) maxFadeTime;
                }

                pitch = (MathHelper.lerp(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true), prevCharge, charge) / info.getPlayerCharge().totalCharges) * 0.5f + 0.5f;
                return;
            }
        }
        setDone();
    }

    public SoundEvent getSoundEvent()
    {
        return soundEvent;
    }

    public void fadeOut()
    {
        fadeTime = maxFadeTime;
        isFadeIn = false;
    }

    public void fadeIn()
    {
        fadeTime = 0;
        isFadeIn = true;
    }
}
