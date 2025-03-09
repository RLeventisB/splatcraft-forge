package net.splatcraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.splatcraft.data.InkColorRegistry;

import java.util.TreeMap;
import java.util.function.Function;

public class InkColor implements Comparable<InkColor>
{
	public static final InkColor INVALID;
	private static final TreeMap<Integer, InkColor> hexToColorMap = new TreeMap<>();
	public static final StreamCodec<RegistryFriendlyByteBuf, InkColor> PACKET_CODEC =
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
				inkColor = InkColorRegistry.getInkColorByAlias(name);
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
	private final int hexCode;
	public InkColor(int color)
	{
		hexCode = color;
	}
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
	public String toString()
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
		
		int currentColorR = (hexCode & 0xFF0000) >> 16;
		int currentColorG = (hexCode & 0x00FF00) >> 8;
		int currentColorB = (hexCode & 0x0000FF);
		
		for (DyeColor color : DyeColor.values())
		{
			int colorValue = propertySelector.apply(color);
			int r = (colorValue & 0xFF0000) >> 16;
			int g = (colorValue & 0x00FF00) >> 8;
			int b = (colorValue & 0x0000FF);
			
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
		return IntTag.valueOf(hexCode);
	}
	public InkColor getInverted()
	{
		return getIfInversed(this, true);
	}
	public float[] getRGB()
	{
		int currentColorR = (hexCode & 0xFF0000) >> 16;
		int currentColorG = (hexCode & 0x00FF00) >> 8;
		int currentColorB = (hexCode & 0x0000FF);
		
		return new float[] {currentColorR / 255f, currentColorG / 255f, currentColorB / 255f};
	}
	public byte[] getRGBBytes()
	{
		byte currentColorR = (byte) ((hexCode & 0xFF0000) >> 16);
		byte currentColorG = (byte) ((hexCode & 0x00FF00) >> 8);
		byte currentColorB = (byte) ((hexCode & 0x0000FF));
		
		return new byte[] {currentColorR, currentColorG, currentColorB};
	}
}