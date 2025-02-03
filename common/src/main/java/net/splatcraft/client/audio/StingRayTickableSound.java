package net.splatcraft.client.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
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
		super(SplatcraftSounds.stingRayStart.getId(), SoundCategory.PLAYERS, beam.getRandom());
		repeat = true;
		repeatDelay = 0;
		
		volume = 0.4f;
		x = beam.getX();
		y = beam.getY();
		z = beam.getZ();
		this.beam = beam;
	}
	@Override
	public boolean shouldAlwaysPlay()
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
				Vec3d cameraPos = MinecraftClient.getInstance().cameraEntity.getEyePos();
				Vec3d closestRelativePoint = StingRayBeamEntity.getClosestPoint(beam.getRotationVector(), cameraPos.subtract(beam.getPos()));
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
				SoundManager soundManager = MinecraftClient.getInstance().getSoundManager();
				if (soundManager == null)
					return;
				
				id = switch (state)
				{
					case 0 -> SplatcraftSounds.stingRayStart.getId();
					case 1 -> SplatcraftSounds.stingRayBeamUse.getId();
					case 2 -> SplatcraftSounds.stingRayShockwave.getId();
					default -> null;
				};
				WeightedSoundSet weightedSoundSet = soundManager.get(id);
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
		SoundSystem soundSystem = soundManager.soundSystem;
		CompletableFuture<Channel.SourceManager> completableFuture = soundSystem.channel.createSource(sound.isStreamed() ? SoundEngine.RunMode.STREAMING : SoundEngine.RunMode.STATIC);
		Channel.SourceManager sourceManager = completableFuture.join();
		Channel.SourceManager old = soundSystem.sources.put(this, sourceManager);
		if (old != null)
			old.run(Source::stop);
		
		if (sound.isStreamed())
		{
			soundSystem.soundLoader.loadStatic(sound.getLocation()).thenAccept((soundx) ->
			{
				sourceManager.run((source) ->
				{
					source.setBuffer(soundx);
					source.play();
				});
			});
		}
		else
		{
			soundSystem.soundLoader.loadStreamed(sound.getLocation(), true).thenAccept((stream) ->
			{
				sourceManager.run((source) ->
				{
					source.setStream(stream);
					source.play();
				});
			});
		}
	}
}
