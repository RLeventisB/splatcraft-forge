package net.splatcraft.mixin;

import com.google.common.collect.ImmutableList;
import net.neoforged.neoforge.client.ClientHooks;
import net.splatcraft.client.renderer.SplatcraftRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ClientHooks.class)
public class AddParticleRenderTypeMixin
{
	@ModifyVariable(method = "makeParticleRenderTypeComparator", at = @At(value = "HEAD"), argsOnly = true)
	private static List splatcraft$addCustomRenderTypes(List value)
	{
		if (value == null)
		{
			return null;
		}
		List modifiableList = new ArrayList(value);
		modifiableList.addLast(SplatcraftRenderTypes.getInkFrontRendertype());
		return ImmutableList.copyOf(modifiableList);
	}
}
