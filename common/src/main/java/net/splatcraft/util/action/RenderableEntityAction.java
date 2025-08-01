package net.splatcraft.util.action;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public interface RenderableEntityAction extends EntityAction
{
	@OnlyIn(Dist.CLIENT)
	void renderExtra(@NotNull PoseStack poseStack, @NotNull MultiBufferSource provider, LivingEntity entity, float partialTicks);
}
