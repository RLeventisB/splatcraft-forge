package net.splatcraft.client.audio;

import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.RollerWeaponSettings;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;

public class RollerRollTickableSound extends MovingSoundInstance
{
    private final PlayerEntity player;
    private float distance = 0.0F;

    public RollerRollTickableSound(PlayerEntity player, boolean isBrush)
    {
        super(isBrush ? SplatcraftSounds.brushRoll : SplatcraftSounds.rollerRoll, SoundCategory.PLAYERS, player.getRandom());
        repeat = true;
        repeatDelay = 0;

        this.player = player;
        volume = 0;
        x = player.getX();
        y = player.getY();
        z = player.getZ();
    }

    @Override
    public boolean shouldAlwaysPlay()
    {
        return true;
    }

    @Override
    public void tick()
    {
        if (player.isAlive() && player.getActiveItem().getItem() instanceof RollerItem)
        {
            ItemStack roller = player.getActiveItem();
            RollerWeaponSettings rollerSettings = ((RollerItem) roller.getItem()).getSettings(roller);
            if (!WeaponBaseItem.enoughInk(player, roller.getItem(), Math.max(rollerSettings.rollData.inkConsumption(), rollerSettings.rollData.dashConsumption()), 7, false))
            {
                setDone();
                return;
            }

            x = (float) player.getX();
            y = (float) player.getY();
            z = (float) player.getZ();

            Vec3d motion = player.equals(ClientUtils.getClientPlayer()) ? player.getVelocity() : player.getPos().subtract(player.getLerpedPos(0));
            double vol = Math.max(Math.abs(player.prevHeadYaw - player.headYaw), motion.multiply(1, 0, 1).length()) * 3f;

            if (vol >= 0.01D)
            {
                distance = MathHelper.clamp(distance + 0.0025F, 0.0F, 1.0F);
                volume = (float) MathHelper.lerp(MathHelper.clamp(vol, 0.0F, 0.5F), 0.0F, 1F);
            }
            else
            {
                distance = 0.0F;
                volume = 0.0F;
            }
        }
        else setDone();
    }
}
