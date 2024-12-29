package net.splatcraft.client.models.inktanks;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractInkTankModel extends EntityModel<LivingEntity>
{
	protected List<ModelPart> tankPieces = new ArrayList<>();
	protected List<ModelPart> inkLevelPieces = new ArrayList<>();
	public AbstractInkTankModel(ModelPart root, int inkPieceCount, String... inkTankModelNames)
	{
		ModelPart inkTank = null;
		for (String name : inkTankModelNames)
		{
			String[] levels = name.split(":");
			ModelPart part = root;
			for (String level : levels)
			{
				part = part.getChild(level);
				if (level.equals("ink_tank"))
					inkTank = part;
			}
			tankPieces.add(part);
		}
		for (int i = 0; i < inkPieceCount; i++)
			inkLevelPieces.add(inkTank.getChild("ink_piece_" + i));
	}
	public void setInkLevels(float inkPctg)
	{
		for (int i = 1; i <= inkLevelPieces.size(); i++)
		{
			ModelPart box = inkLevelPieces.get(i - 1);
			if (inkPctg == 0)
			{
				box.visible = false;
				continue;
			}
			box.visible = true;
			box.pivotY = 23.25F - Math.min(i * inkPctg, i);
		}
	}
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color)
	{
		Iterable<ModelPart> parts = (color == -1 ? getTankParts() : getInkLevelParts());
		parts.forEach((modelPart) ->
			modelPart.render(matrices, vertices, light, overlay, color));
	}
	public Iterable<ModelPart> getTankParts()
	{
		return tankPieces;
	}
	public Iterable<ModelPart> getInkLevelParts()
	{
		return inkLevelPieces;
	}
	public <M extends EntityModel<? extends LivingEntity>> void notifyState(M contextModel)
	{
	}
}
