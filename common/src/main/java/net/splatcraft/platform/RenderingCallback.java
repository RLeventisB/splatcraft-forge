package net.splatcraft.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

@FunctionalInterface
public interface RenderingCallback
{
	void render(CallbackData data);
	record CallbackData(LevelRenderer levelRenderer,
	                    PoseStack poseStack,
	                    DeltaTracker tickCounter,
	                    Camera camera,
	                    GameRenderer gameRenderer,
	                    Matrix4f projectionMatrix,
	                    Matrix4f positionMatrix,
	                    Frustum frustum,
	                    MultiBufferSource consumers)
	{
	}
	enum RenderingStage
	{
		AFTER_SKY,
		AFTER_BLOCKS,
		AFTER_ENTITIES,
		AFTER_PARTICLES,
		AFTER_DEBUG
	}
}
