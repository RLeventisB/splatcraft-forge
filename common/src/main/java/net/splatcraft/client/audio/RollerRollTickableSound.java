package net.splatcraft.client.audio;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.items.weapons.RollerItem;
import net.splatcraft.items.weapons.WeaponBaseItem;
import net.splatcraft.items.weapons.settings.RollerWeaponSettings;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.ClientUtils;

public class RollerRollTickableSound extends AbstractTickableSoundInstance
{
    private final Player player;
    private float distance = 0.0F;

    public RollerRollTickableSound(Player player, boolean isBrush)
    {
        super(isBrush ? SplatcraftSounds.brushRoll : SplatcraftSounds.rollerRoll, SoundSource.PLAYERS, player.getRandom());
        looping = true;
        delay = 0;

        this.player = player;
        volume = 0;
        x = player.getX();
        y = player.getY();
        z = player.getZ();
    }

    @Override
    public boolean canStartSilent()
    {
        return true;
    }

    @Override
    public void tick()
    {
        if (player.isAlive() && player.getUseItem().getItem() instanceof RollerItem)
        {
            ItemStack roller = player.getUseItem();
            RollerWeaponSettings rollerSettings = ((RollerItem) roller.getItem()).getSettings(roller);
            if (!WeaponBaseItem.enoughInk(player, roller.getItem(), Math.max(rollerSettings.rollData.inkConsumption(), rollerSettings.rollData.dashConsumption()), 7, false))
            {
                stop();
                return;
            }

            x = (float) player.getX();
            y = (float) player.getY();
            z = (float) player.getZ();

            Vec3 motion = player.equals(ClientUtils.getClientPlayer()) ? player.getDeltaMovement() : player.position().subtract(player.getPosition(0));
            double vol = Math.max(Math.abs(player.yHeadRotO - player.yHeadRot), motion.multiply(1, 0, 1).length()) * 3f;

            if (vol >= 0.01D)
            {
                distance = Mth.clamp(distance + 0.0025F, 0.0F, 1.0F);
                volume = (float) Mth.lerp(Mth.clamp(vol, 0.0F, 0.5F), 0.0F, 1F);
            }
            else
            {
                distance = 0.0F;
                volume = 0.0F;
            }
        }
        else stop();
    }
}
