package net.splatcraft.util.structs;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.splatcraft.data.InkColorRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

public record InkColor(int hexCode) implements Comparable<InkColor>
{
	public static final InkColor INVALID;
	static
	{
		try
		{
			INVALID = new InkColor(-1);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	private static final Int2ReferenceMap<InkColor> hexToColorMap = new Int2ReferenceAVLTreeMap<>();
	public static final StreamCodec<ByteBuf, InkColor> STREAM_CODEC =
		StreamCodec.composite(
			ByteBufCodecs.INT, InkColor::getColor,
			InkColor::constructOrReuse
		);
	public static final Codec<InkColor> RAW_INT_CODEC = new Codec<>()
	{
		@Override
		public <T> DataResult<T> encode(InkColor input, DynamicOps<T> ops, T prefix)
		{
			if (input == null)
			{
				return DataResult.error(() -> "Input InkColor is not valid");
			}
			
			return DataResult.success(ops.createInt(input.getColor()));
		}
		@Override
		public <T> DataResult<Pair<InkColor, T>> decode(DynamicOps<T> ops, T input)
		{
			DataResult<Number> hexValue = ops.getNumberValue(input);
			
			if (hexValue.isSuccess())
			{
				return DataResult.success(Pair.of(constructOrReuse(hexValue.map(Number::intValue).getOrThrow()), input));
			}
			
			return DataResult.error(() -> "InkColor wasn't formatted correctly, should've been an raw number.");
		}
	};
	public static final Codec<InkColor> HEX_CODEC = new Codec<>()
	{
		@Override
		public <T> DataResult<Pair<InkColor, T>> decode(DynamicOps<T> ops, T input)
		{
			InkColor inkColor = null;
			
			DataResult<String> stringValue = ops.getStringValue(input);
			if (stringValue.isSuccess())
			{
				String hexCode = stringValue.getOrThrow();
				try
				{
					inkColor = constructOrReuse(Integer.decode(hexCode));
				}
				catch (NumberFormatException ignored)
				{
				
				}
			}
			if (inkColor == null)
				return DataResult.error(() -> "Invalid InkColor color");
			return DataResult.success(Pair.of(inkColor, input));
		}
		@Override
		public <T> DataResult<T> encode(InkColor input, DynamicOps<T> ops, T prefix)
		{
			if (input == null)
			{
				return DataResult.error(() -> "Input InkColor is not valid");
			}
			return DataResult.success(ops.createString("#" + Integer.toHexString(input.hexCode)));
		}
	};
	public static final Codec<InkColor> NAME_CODEC = new Codec<>()
	{
		@Override
		public <T> DataResult<Pair<InkColor, T>> decode(DynamicOps<T> ops, T input)
		{
			InkColor inkColor = null;
			
			DataResult<ResourceLocation> idResult = ResourceLocation.CODEC.parse(ops, input);
			if (idResult.isSuccess())
			{
				ResourceLocation name = idResult.getOrThrow();
				inkColor = InkColorRegistry.getColorByAlias(name).orElse(null);
			}
			if (inkColor == null)
				return DataResult.error(() -> "Invalid InkColor color, didn't find a valid alias");
			
			return DataResult.success(Pair.of(inkColor, input));
		}
		@Override
		public <T> DataResult<T> encode(InkColor input, DynamicOps<T> ops, T prefix)
		{
			if (input == null)
			{
				return DataResult.error(() -> "Input InkColor is not valid");
			}
			ResourceLocation colorAliasId = InkColorRegistry.getColorAlias(input);
			if (colorAliasId == null)
			{
				return DataResult.error(() -> "Input InkColor has no alias");
			}
			return DataResult.success(ops.createString(colorAliasId.toString()));
		}
	};
	public static final Codec<InkColor> NUMBER_CODEC = Codec.withAlternative(RAW_INT_CODEC, HEX_CODEC);
	public static final Codec<InkColor> CODEC = Codec.withAlternative(NAME_CODEC, NUMBER_CODEC);
	public static InkColor constructOrReuse(int hexCode)
	{
		try
		{
			return hexToColorMap.computeIfAbsent(hexCode, InkColor::new);
		}
		catch (Exception e)
		{
			throw new RuntimeException("what did you do");
		}
	}
	public static Optional<InkColor> reuse(int hexCode)
	{
		return Optional.ofNullable(hexToColorMap.get(hexCode));
	}
	public static InkColor getFromNbt(Tag nbt)
	{
		return NUMBER_CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
	}
	public static InkColor getIfInversed(InkColor color, boolean inverted)
	{
		if (color.isInvalid())
		{
			return color;
		}
		return inverted ? constructOrReuse(0xFFFFFF - color.hexCode) : color;
	}
	public static boolean isHexCodeInRange(int hexCode)
	{
		return (hexCode & 0xFFFFFF) == hexCode;
	}
	public MutableComponent getLocalizedName()
	{
		return Component.translatable(getTranslationKey());
	}
	public String getTranslationKey()
	{
		ResourceLocation alias = InkColorRegistry.getFirstAliasForColor(hexCode);
		
		if (alias != null)
		{
			return "ink_color." + alias.toShortLanguageKey();
		}
		return "ink_color." + String.format("%06X", hexCode).toLowerCase();
	}
	public String getHexCode()
	{
		return String.format("%06X", hexCode);
	}
	public int getColor()
	{
		return hexCode;
	}
	public TextColor getTextColor()
	{
		return TextColor.fromRgb(hexCode);
	}
	public int getColorWithAlpha(int alpha)
	{
		return hexCode | (alpha << 24);
	}
	@Override
	public @NotNull String toString()
	{
		ResourceLocation alias = InkColorRegistry.getColorAlias(this);
		if (alias != null)
			return alias.getPath() + ": #" + getHexCode().toUpperCase();
		return "unregistered: #" + getHexCode().toUpperCase();
	}
	@Override
	public int compareTo(InkColor other)
	{
		return hexCode - other.hexCode;
	}
	public DyeColor getDyeColor()
	{
		return getDyeColor(DyeColor::getTextureDiffuseColor);
	}
	public DyeColor getDyeColor(Function<DyeColor, Integer> propertySelector)
	{
		int id = -1;
		int colorDifference = Integer.MAX_VALUE;
		
		int currentColorR = FastColor.ARGB32.red(hexCode);
		int currentColorG = FastColor.ARGB32.green(hexCode);
		int currentColorB = FastColor.ARGB32.blue(hexCode);
		
		for (DyeColor color : DyeColor.values())
		{
			int colorValue = propertySelector.apply(color);
			int r = FastColor.ARGB32.red(colorValue);
			int g = FastColor.ARGB32.green(colorValue);
			int b = FastColor.ARGB32.blue(colorValue);
			
			int difference = Mth.square(r - currentColorR) + Mth.square(g - currentColorG) + Mth.square(b - currentColorB);
			if (colorDifference > difference)
			{
				colorDifference = difference;
				id = color.getId();
			}
		}
		return DyeColor.byId(id);
	}
	public boolean isValid()
	{
		return isHexCodeInRange(hexCode);
	}
	public boolean isInvalid()
	{
		return !isValid();
	}
	public Tag getNbt()
	{
		return NUMBER_CODEC.encodeStart(NbtOps.INSTANCE, this).getOrThrow();
	}
	public InkColor getInverted()
	{
		return getIfInversed(this, true);
	}
	public float[] getRGB()
	{
		return new float[] {
			FastColor.ARGB32.red(hexCode) / 255f,
			FastColor.ARGB32.green(hexCode) / 255f,
			FastColor.ARGB32.blue(hexCode) / 255f
		};
	}
	public int[] getRGBInts()
	{
		return new int[] {
			FastColor.ARGB32.red(hexCode),
			FastColor.ARGB32.green(hexCode),
			FastColor.ARGB32.blue(hexCode)
		};
	}
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof InkColor color)) return false;
		return hexCode == color.hexCode;
	}
	@Override
	public int hashCode()
	{
		return hexCode;
	}
}