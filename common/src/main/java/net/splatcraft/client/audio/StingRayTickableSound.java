package net.splatcraft.client.audio;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.entities.StingRayBeamEntity;
import net.splatcraft.registries.SplatcraftSounds;
import net.splatcraft.util.action.EntityAction;
import net.splatcraft.util.action.specials.StingRayAction;

import java.util.concurrent.CompletableFuture;

public class StingRayTickableSound extends MovingSoundInstanceButTheIdCanBeChanged
{
	private final StingRayBeamEntity beam;
	private int oldState = 0;
	public StingRayTickableSound(StingRayBeamEntity beam)
	{
		super(SplatcraftSounds.stingRayStart.getLocation(), SoundSource.PLAYERS, beam.getRandom());
		repeat = true;
		repeatDelay = 0;
		
		volume = 0.4f;
		x = beam.getX();
		y = beam.getY();
		z = beam.getZ();
		this.beam = beam;
	}
	@Override
	public boolean canStartSilent()
	{
		return true;
	}
	@Override
	public void tick()
	{
		if (beam != null && beam.isAlive())
		{
			updateSound();
			
			try
			{
				Vec3 cameraPos = Minecraft.getInstance().cameraEntity.getEyePosition();
				Vec3 closestRelativePoint = StingRayBeamEntity.getClosestPoint(beam.getLookAngle(), cameraPos.subtract(beam.position()));
				x = closestRelativePoint.x + cameraPos.x;
				y = closestRelativePoint.y + cameraPos.y;
				z = closestRelativePoint.z + cameraPos.z;
			}
			catch (Exception ignored)
			{
				x = beam.getX();
				y = beam.getY();
				z = beam.getZ();
			}
		}
		else
			setDone();
	}
	private void updateSound()
	{
		int state = beam.getState();
		
		if (beam.getOwner() instanceof LivingEntity entity)
		{
			EntityAction.getSpecificEntityActionOptional(entity, StingRayAction.class).ifPresent(action ->
			{
				if (action.getTime() <= 20f)
				{
					pitch = 0.75f + (action.getTime() / 20f) * 0.25f;
					volume = 0.3f + (action.getTime() / 20f) * 0.1f;
				}
			});
		}
		
		if (state != oldState)
		{
			try
			{
				SoundManager soundManager = Minecraft.getInstance().getSoundManager();
				if (soundManager == null)
					return;
				
				id = switch (state)
				{
					case 0 -> SplatcraftSounds.stingRayStart.getLocation();
					case 1 -> SplatcraftSounds.stingRayBeamUse.getLocation();
					case 2 -> SplatcraftSounds.stingRayShockwave.getLocation();
					default -> null;
				};
				WeighedSoundEvents weightedSoundSet = soundManager.getSoundEvent(id);
				if (weightedSoundSet == null)
					return;
				
				sound = weightedSoundSet.getSound(random);
				
				// todo: uhh maybe we could've played 3 sounds at the same time and activate them accordingly
				// because i think this leaks memory since its my second time doing openal things (and i hated them
				// back im nonogame)
				reassignSound(soundManager);
			}
			catch (Exception ignored)
			{
			
			}
		}
		
		oldState = state;
	}
	private void reassignSound(SoundManager soundManager)
	{
		SoundEngine soundSystem = soundManager.soundEngine;
		CompletableFuture<ChannelAccess.ChannelHandle> completableFuture = soundSystem.channelAccess.createHandle(sound.shouldStream() ? Library.Pool.STREAMING : Library.Pool.STATIC);
		ChannelAccess.ChannelHandle sourceManager = completableFuture.join();
		ChannelAccess.ChannelHandle old = soundSystem.instanceToChannel.put(this, sourceManager);
		if (old != null)
			old.execute(Channel::stop);
		
		if (sound.shouldStream())
		{
			soundSystem.soundBuffers.getCompleteBuffer(sound.getPath()).thenAccept((soundx) ->
			{
				sourceManager.execute((source) ->
				{
					source.attachStaticBuffer(soundx);
					source.play();
				});
			});
		}
		else
		{
			soundSystem.soundBuffers.getStream(sound.getPath(), true).thenAccept((stream) ->
			{
				sourceManager.execute((source) ->
				{
					source.attachBufferStream(stream);
					source.play();
				});
			});
		}
	}
}
