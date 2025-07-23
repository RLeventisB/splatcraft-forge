package net.splatcraft.commands.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.StringRepresentableArgument;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public class HighlightTypeArgument extends StringRepresentableArgument<HighlightTypeArgument.HighlightType>
{
	protected HighlightTypeArgument()
	{
		super(HighlightTypeArgument.HighlightType.CODEC, HighlightTypeArgument.HighlightType::values);
	}
	public static HighlightTypeArgument.HighlightType getHighlightType(CommandContext<CommandSourceStack> context, String id)
	{
		return context.getArgument(id, HighlightTypeArgument.HighlightType.class);
	}
	public static HighlightTypeArgument highlightType()
	{
		return new HighlightTypeArgument();
	}
	public enum HighlightType implements StringRepresentable
	{
		NONE,
		PARTICLE,
		EDGE_PARTICLE;
		public static final Codec<HighlightType> CODEC = StringRepresentable.fromEnum(HighlightType::values);
		@Override
		public @NotNull String getSerializedName()
		{
			return name();
		}
	}
}
