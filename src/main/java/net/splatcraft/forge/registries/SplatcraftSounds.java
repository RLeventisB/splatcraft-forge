package net.splatcraft.forge.registries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.common.util.ForgeSoundType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import net.splatcraft.forge.Splatcraft;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SplatcraftSounds
{

    private static final List<SoundEvent> sounds = new ArrayList<>();

    public static SoundEvent squidTransform;
    public static SoundEvent squidRevert;
    public static SoundEvent inkSubmerge;
    public static SoundEvent inkSurface;
    public static SoundEvent noInkMain;
    public static SoundEvent noInkSub;
    public static SoundEvent shooterShot;
    public static SoundEvent blasterShot;
    public static SoundEvent blasterDirect;
    public static SoundEvent blasterExplosion;
    public static SoundEvent rollerFling;
    public static SoundEvent rollerRoll;
    public static SoundEvent brushFling;
    public static SoundEvent brushRoll;
    public static SoundEvent chargerCharge;
    public static SoundEvent chargerReady;
    public static SoundEvent chargerShot;
    public static SoundEvent splatlingCharge;
    public static SoundEvent splatlingChargeSecondLevel;
    public static SoundEvent splatlingReady;
    public static SoundEvent splatlingShot;
    public static SoundEvent dualieShot;
    public static SoundEvent dualieDodge;
    public static SoundEvent slosherShot;
    public static SoundEvent subThrow;
    public static SoundEvent subDetonating;
    public static SoundEvent subDetonate;
    public static SoundEvent remoteUse;
    public static SoundEvent powerEggCanOpen;
    public static SoundEvent squidBumperPlace;
    public static SoundEvent squidBumperPop;
    public static SoundEvent squidBumperRespawning;
    public static SoundEvent squidBumperReady;
    public static SoundEvent squidBumperHit;
    public static SoundEvent squidBumperInk;
    public static SoundEvent squidBumperBreak;
    public static SoundEvent splatSwitchPoweredOn;
    public static SoundEvent splatSwitchPoweredOff;
    public static SoundEvent inkedBlockBreak;
    public static SoundEvent inkedBlockStep;
    public static SoundEvent inkedBlockSwim;
    public static SoundEvent inkedBlockPlace;
    public static SoundEvent inkedBlockHit;
    public static SoundEvent inkedBlockFall;
    public static SoundEvent superjumpStart;
    public static SoundEvent superjumpLand;
    public static final SoundType SOUND_TYPE_INK = new ForgeSoundType(1.0F, 1.0F, () -> SplatcraftSounds.inkedBlockBreak, () -> SplatcraftSounds.inkedBlockStep, () -> SplatcraftSounds.inkedBlockPlace, () -> SplatcraftSounds.inkedBlockHit, () -> SplatcraftSounds.inkedBlockFall);
    public static final SoundType SOUND_TYPE_SWIMMING = new ForgeSoundType(1.0F, 1.0F, () -> SplatcraftSounds.inkedBlockBreak, () -> SplatcraftSounds.inkedBlockSwim, () -> SplatcraftSounds.inkedBlockPlace, () -> SplatcraftSounds.inkedBlockHit, () -> SplatcraftSounds.inkedBlockFall);
    public static void initSounds()
    {
        inkedBlockBreak = createSoundEvent("block.inked_block.break");
        inkedBlockStep = createSoundEvent("block.inked_block.step");
        inkedBlockSwim = createSoundEvent("block.inked_block.swim");
        inkedBlockPlace = createSoundEvent("block.inked_block.place");
        inkedBlockHit = createSoundEvent("block.inked_block.hit");
        inkedBlockFall = createSoundEvent("block.inked_block.fall");

        squidTransform = createSoundEvent("squid_transform");
        squidRevert = createSoundEvent("squid_revert");
        inkSubmerge = createSoundEvent("ink_submerge");
        inkSurface = createSoundEvent("ink_surface");
        noInkMain = createSoundEvent("no_ink");
        noInkSub = createSoundEvent("no_ink_sub");
        shooterShot = createSoundEvent("shooter_firing");
        blasterShot = createSoundEvent("blaster_firing");
        blasterDirect = createSoundEvent("blaster_direct");
        blasterExplosion = createSoundEvent("blaster_explosion");
        rollerFling = createSoundEvent("roller_fling");
        rollerRoll = createSoundEvent("roller_roll");
        brushFling = createSoundEvent("brush_fling");
        brushRoll = createSoundEvent("brush_roll");
        chargerCharge = createSoundEvent("charger_charge");
        chargerReady = createSoundEvent("charger_ready");
        chargerShot = createSoundEvent("charger_shot");
        splatlingCharge = createSoundEvent("splatling_charge");
        splatlingChargeSecondLevel = createSoundEvent("splatling_charge_second_level");
        splatlingReady = createSoundEvent("splatling_ready");
        splatlingShot = createSoundEvent("splatling_firing");
        dualieShot = createSoundEvent("dualie_firing");
        dualieDodge = createSoundEvent("dualie_dodge");
        slosherShot = createSoundEvent("slosher_shot");
        subThrow = createSoundEvent("sub_throw");
        subDetonating = createSoundEvent("sub_detonating");
        subDetonate = createSoundEvent("sub_detonate");
        remoteUse = createSoundEvent("remote_use");
        powerEggCanOpen = createSoundEvent("power_egg_can_open");
        squidBumperPlace = createSoundEvent("squid_bumper_place");
        squidBumperPop = createSoundEvent("squid_bumper_pop");
        squidBumperRespawning = createSoundEvent("squid_bumper_respawning");
        squidBumperReady = createSoundEvent("squid_bumper_ready");
        squidBumperHit = createSoundEvent("squid_bumper_hit");
        squidBumperInk = createSoundEvent("squid_bumper_ink");
        squidBumperBreak = createSoundEvent("squid_bumper_break");
        splatSwitchPoweredOn = createSoundEvent("splat_switch_powered_on");
        splatSwitchPoweredOff = createSoundEvent("splat_switch_powered_off");

        superjumpStart = createSoundEvent("superjump_start");
        superjumpLand = createSoundEvent("superjump_land");
    }

    private static SoundEvent createSoundEvent(String id)
    {
        ResourceLocation loc = new ResourceLocation(Splatcraft.MODID, id);
        SoundEvent sound = new SoundEvent(loc).setRegistryName(loc);
        sounds.add(sound);
        return sound;
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event)
    {
        initSounds();

        IForgeRegistry<SoundEvent> registry = event.getRegistry();
        for (SoundEvent sound : sounds)
        {
            registry.register(sound);
        }
    }
}
