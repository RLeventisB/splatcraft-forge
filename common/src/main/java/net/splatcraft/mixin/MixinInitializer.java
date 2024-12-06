package net.splatcraft.forge.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinInitializer implements IMixinConfigPlugin
{
    public boolean sodiumInstalled, createInstalled;

    @Override
    public void onLoad(String mixinPackage)
    {
        MixinExtrasBootstrap.init();
//        try
        {
//            Class.forName("me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", false, null);
            sodiumInstalled = LoadingModList.get().getModFileById("rubidium") != null;
            createInstalled = LoadingModList.get().getModFileById("create") != null;
        }
//        catch (ClassNotFoundException ignored)
//        {
//            sodiumInstalled = false;
//        }
    }

    @Override
    public String getRefMapperConfig()
    {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        if (mixinClassName.startsWith("net.splatcraft.forge.mixin.compat"))
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
