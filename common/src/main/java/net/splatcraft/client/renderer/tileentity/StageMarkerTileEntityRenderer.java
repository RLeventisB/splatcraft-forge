package net.splatcraft.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.splatcraft.Splatcraft;
import net.splatcraft.blocks.ColoredBarrierBlock;
import net.splatcraft.blocks.StageBarrierBlock;
import net.splatcraft.blocks.StageMarkerBlock;
import net.splatcraft.client.gui.StageMarkerEditorScreen;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.tileentities.StageMarkerTileEntity;
import net.splatcraft.util.ClientUtils;
import net.splatcraft.util.ColorUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

public class StageMarkerTileEntityRenderer implements BlockEntityRenderer<StageMarkerTileEntity>
{
	public StageMarkerTileEntityRenderer(BlockEntityRendererProvider.Context context)
	{
	}
	private static boolean shouldRenderSide(StageMarkerTileEntity te, Direction side)
	{
		BlockPos relativePos = te.getBlockPos().relative(side);
		BlockState relativeState = te.getLevel().getBlockState(relativePos);
		
		if (!ClientUtils.shouldRenderSide(te, side)) return false;
		
		if (relativeState.getBlock() instanceof ColoredBarrierBlock block && te.getLevel().getBlockState(te.getBlockPos()).getBlock() instanceof ColoredBarrierBlock coloredBlock)
			return block.canAllowThrough(relativePos, ClientUtils.getClientPlayer()) !=
				coloredBlock.canAllowThrough(te.getBlockPos(), ClientUtils.getClientPlayer());
		
		return !(relativeState.getBlock() instanceof StageBarrierBlock);
	}
	@Override
	public void render(StageMarkerTileEntity marker, float partialTicks, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int combinedLight, int combinedOverlay)
	{
		Block block = marker.getBlockState().getBlock();
		boolean isEditing = (ClientUtils.getClient().screen instanceof StageMarkerEditorScreen screen && screen.marker == marker) || ClientUtils.getClientPlayer().isHolding(SplatcraftItems.stageMarker.get());
		if (!(block instanceof StageMarkerBlock) || (!marker.isActive() && !isEditing))
		{
			return;
		}
		
		switch (marker.getMarkerType())
		{
			case SPLAT_ZONE:
				
				if (isEditing)
				{
					VertexConsumer builder = buffer.getBuffer(RenderType.LINES);
					Vec3i min = marker.getRelativeMinBoundPos();
					Vec3i max = marker.getRelativeMaxBoundPos().offset(1, 1, 1);
					LevelRenderer.renderLineBox(poseStack, builder, min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ(), 0, 0, 1, 1.0F);
				}
				else
				{
					ResourceLocation textureLoc = Splatcraft.identifierOf("block/splat_zone_gradient");
					
					List<List<Vector3f>> renderingCorners = marker.getRenderingCorners();
					VertexConsumer outlineBuffer = buffer.getBuffer(RenderType.lines());
					poseStack.pushPose();
					poseStack.translate(marker.getOffset().getX(), marker.getOffset().getY(), marker.getOffset().getZ());
					int color = marker.getCurrentColor().map(ColorUtils::makeBrighter).orElse(0xFFDDDDDD);
					
					for (List<Vector3f> list : renderingCorners)
					{
						for (int i = 0; i < list.size(); i++)
						{
							Vector3f vec = list.get(i);
							
							Vector3f outlineNext = list.get((i + 1) % list.size());
							
							outlineBuffer.addVertex(poseStack.last(), vec.x, vec.y, vec.z).setColor(color).setNormal(1, 0, 1);
							outlineBuffer.addVertex(poseStack.last(), outlineNext.x, outlineNext.y, outlineNext.z).setColor(color).setNormal(0, 1, 0);
						}
					}
					
					float barrierAlpha = (float) Math.clamp(ClientUtils.getClientPlayer().position().subtract(Vec3.atCenterOf(marker.getOffsetedPosition())).horizontalDistanceSqr() / 100f - 1f, 0f, 1f);
					
					TextureAtlasSprite sprite = ClientUtils.getClient().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(textureLoc);
					VertexConsumer barrierBuffer = buffer.getBuffer(Minecraft.useShaderTransparency() ? RenderType.translucentMovingBlock() : RenderType.translucent());
					
					float u0 = sprite.getU0();
					float u1 = sprite.getU1();
					float v0 = sprite.getV1();
					float v1 = sprite.getV0();
					
					for (List<Vector3f> list : renderingCorners)
					{
						for (int i = 0; i < list.size(); i++)
						{
							int barrierColor = FastColor.ARGB32.color((int) ((barrierAlpha * 0.7f + 0.3f) * 255), color);
							
							Vector3f vec = list.get(i);
							Vector3f next = list.get((i + 1) % list.size());
							
							renderGlowLine(poseStack, barrierBuffer, vec.x, vec.y + 0.5f, vec.z, next.x, next.y + 0.5f, next.z, 0.5f, u0, u1, (v1 + v0) / 2, v1, barrierColor);
							
							if (barrierAlpha <= 0)
								continue;
							
							float ticks = (marker.getLevel().getGameTime() + partialTicks) / 10f;
							float height = 2f / Mth.HALF_PI;
							for (int j = 0; j < 5; j++)
							{
								float mod10Tick = (ticks % 10f) / 10f;
								float yAdd = height * j * Mth.HALF_PI;
								int lineColor;
								if (j == 4)
								{
									float dissapearProgress = Mth.sin(mod10Tick * Mth.HALF_PI);
									lineColor = FastColor.ARGB32.color((int) (170 * ((1f - dissapearProgress) * barrierAlpha)), barrierColor);
									yAdd += dissapearProgress * height;
								}
								else
								{
									lineColor = FastColor.ARGB32.color((int) (170 * barrierAlpha), barrierColor);
									yAdd += mod10Tick * height * Mth.HALF_PI;
								}
								
								renderGlowLine(poseStack, barrierBuffer, vec.x, vec.y + yAdd, vec.z, next.x, next.y + yAdd, next.z, 0.15f, u0, u1, v0, v1, lineColor);
							}
							
/*
							Vector3f next = list.get((i + 1) % list.size());
							Vector3f nextNext = list.get((i + 2) % list.size());
							Vector3f nextBut3 = list.get((i + 3) % list.size());
							
							barrierBuffer.addVertex(poseStack.last(), vec.x, vec.y, vec.z)
								.setColor(barrierColor).setUv(sprite.getU0(), v1).setUv2(0, 240).setNormal(1, 0, 0);
							barrierBuffer.addVertex(poseStack.last(), next.x, next.y, next.z)
								.setColor(barrierColor).setUv(sprite.getU0(), v2).setUv2(0, 240).setNormal(1, 0, 0);
							barrierBuffer.addVertex(poseStack.last(), nextBut3.x, nextBut3.y, nextBut3.z)
								.setColor(barrierColor).setUv(sprite.getU1(), v2).setUv2(0, 240).setNormal(1, 0, 0);
							barrierBuffer.addVertex(poseStack.last(), nextNext.x, nextNext.y, nextNext.z)
								.setColor(barrierColor).setUv(sprite.getU1(), v1).setUv2(0, 240).setNormal(1, 0, 0);
*/
						}
					}
					poseStack.popPose();
				}
				
				break;
		}
	}
	public static void renderGlowLine(PoseStack poseStack, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, float height, float u0, float u1, float v0, float v1, int color)
	{
		buffer.addVertex(poseStack.last(), x2, y2 - height, z2)
			.setColor(color).setUv(u1, v0).setUv2(0, 240).setNormal(1, 0, 0);
		buffer.addVertex(poseStack.last(), x1, y1 - height, z1)
			.setColor(color).setUv(u0, v0).setUv2(0, 240).setNormal(1, 0, 0);
		buffer.addVertex(poseStack.last(), x1, y1 + height, z1)
			.setColor(color).setUv(u0, v1).setUv2(0, 240).setNormal(1, 0, 0);
		buffer.addVertex(poseStack.last(), x2, y2 + height, z2)
			.setColor(color).setUv(u1, v1).setUv2(0, 240).setNormal(1, 0, 0);
		
		buffer.addVertex(poseStack.last(), x1, y1 - height, z1)
			.setColor(color).setUv(u0, v0).setUv2(0, 240).setNormal(1, 0, 0);
		buffer.addVertex(poseStack.last(), x2, y2 - height, z2)
			.setColor(color).setUv(u1, v0).setUv2(0, 240).setNormal(1, 0, 0);
		buffer.addVertex(poseStack.last(), x2, y2 + height, z2)
			.setColor(color).setUv(u1, v1).setUv2(0, 240).setNormal(1, 0, 0);
		buffer.addVertex(poseStack.last(), x1, y1 + height, z1)
			.setColor(color).setUv(u0, v1).setUv2(0, 240).setNormal(1, 0, 0);
	}
}
