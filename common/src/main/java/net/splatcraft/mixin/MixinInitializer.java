package net.splatcraft.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.splatcraft.platform.Services;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MixinInitializer implements IMixinConfigPlugin
{
	public boolean sodiumInstalled, createInstalled, isOnFabric, isOnNeoForge, isOnClient;
	@Override
	public void onLoad(String mixinPackage)
	{
		MixinExtrasBootstrap.init();
		isOnFabric = Objects.equals(Services.PLATFORM.getPlatformName(), "Fabric");
		isOnNeoForge = Objects.equals(Services.PLATFORM.getPlatformName(), "NeoForge");
		isOnClient = Services.PLATFORM.isClientSide();
		try
		{
			sodiumInstalled = Services.PLATFORM.anyModThat(v -> v.modId().contains("sodium") || v.modId().contains("rubidium") || v.modId().contains("embeddium"));
			createInstalled = Services.PLATFORM.anyModThat(v -> v.modId().contains("create"));
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
