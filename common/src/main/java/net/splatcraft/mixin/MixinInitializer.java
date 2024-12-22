package net.splatcraft.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import dev.architectury.platform.Platform;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinInitializer implements IMixinConfigPlugin
{
	public boolean sodiumInstalled, createInstalled, isOnFabric, isOnNeoForge;
	@Override
	public void onLoad(String mixinPackage)
	{
		MixinExtrasBootstrap.init();
		isOnFabric = Platform.isFabric();
		isOnNeoForge = Platform.isNeoForge();
		try
		{
//			sodiumInstalled = Platform.getMods().stream().anyMatch(v -> v.getModId().contains("sodium") || v.getModId().contains("rubidium"));
//			createInstalled = Platform.getMods().stream().anyMatch(v -> v.getModId().contains("create"));
		}
		catch (Exception ignored)
		{
		}
	}
	@Override
	public String getRefMapperConfig()
	{
		return "";
	}
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
		if (mixinClassName.contains("Forge"))
		{
			return isOnNeoForge;
		}
		if (mixinClassName.contains("Fabric"))
		{
			return isOnFabric;
		}
		if (mixinClassName.startsWith("net.splatcraft.mixin.compat"))
		{
			if (mixinClassName.contains("Sodium"))
			{
				return sodiumInstalled;
			}
			if (mixinClassName.contains("Create"))
			{
				return createInstalled;
			}
		}
		return true;
	}
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
	{
	
	}
	@Override
	public List<String> getMixins()
	{
		return List.of();
	}
	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
	{
	
	}
	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
	{
	
	}
}
