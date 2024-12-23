package net.splatcraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.MathHelper;
import net.splatcraft.data.InkColorRegistry;

import java.util.TreeMap;
import java.util.function.Function;

public class InkColor implements Comparable<InkColor>
{
	public static final InkColor INVALID;
	private static final TreeMap<Integer, InkColor> hexToColorMap = new TreeMap<>();
	public static final PacketCodec<? super RegistryByteBuf, InkColor> PACKET_CODEC =
		PacketCodec.tuple(
			PacketCodecs.INTEGER, InkColor::getColor,
			InkColor::constructOrReuse
		);
	public static final Codec<InkColor> CODEC = new Codec<>()
	{
		@Override
		public <T> DataResult<Pair<InkColor, T>> decode(DynamicOps<T> ops, T input)
		{
			DataResult<Number> hexValue = ops.getNumberValue(input);
			InkColor inkColor = null;
			
			if (hexValue.isSuccess())
			{
				inkColor = constructOrReuse(hexValue.map(Number::intValue).getOrThrow());
			}
			else
			{
				DataResult<String> stringValue = ops.getStringValue(input);
				if (stringValue.isSuccess())
				{
					String hexCode = stringValue.getOrThrow();
					if (!hexCode.startsWith("0x") && !hexCode.startsWith("0X") && !hexCode.startsWith("#") && !hexCode.startsWith("0"))
						inkColor = constructOrReuse(Integer.valueOf(hexCode, 16));
					else
						inkColor = constructOrReuse(Integer.decode(hexCode));
				}
			}
			if (inkColor == null)
				return DataResult.error(() -> "Invalid InkColor color", Pair.of(INVALID, ops.empty()));
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
		if (hexToColorMap.containsKey(hexCode))
			return hexToColorMap.get(hexCode);
		try
		{
			InkColor color = new InkColor(hexCode);
			hexToColorMap.put(hexCode, color);
			return color;
		}
		catch (Exception e)
		{
			throw new RuntimeException("what did you do");
		}
	}
	public static InkColor getFromNbt(NbtElement nbt)
	{
		return CODEC.decode(NbtOps.INSTANCE, nbt).getOrThrow().getFirst();
	}
	public static InkColor getIfInversed(InkColor color, boolean inverted)
	{
		if (color.getColor() < 0)
		{
			return color;
		}
		return inverted ? constructOrReuse(0xFFFFFF - color.hexCode) : color;
	}
	public static boolean isHexCodeInRange(int hexCode)
	{
		return (hexCode & 0xFFFFFF) == hexCode;
	}
	public MutableText getLocalizedName()
	{
		return Text.translatable(getTranslationKey());
	}
	public String getTranslationKey()
	{
		return "ink_color." + InkColorRegistry.getFirstAliasForColor(hexCode).toShortTranslationKey();
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
		return hexCode | alpha << 24;
	}
	public String getName()
	{
		return InkColorRegistry.getFirstAliasForColor(hexCode).getPath();
	}
	@Override
	public String toString()
	{
		return getName() + ": #" + getHexCode().toUpperCase();
	}
	@Override
	public int compareTo(InkColor other)
	{
		return hexCode - other.hexCode;
	}
	public DyeColor getDyeColor()
	{
		return getDyeColor(DyeColor::getEntityColor);
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
			
			int difference = MathHelper.square(r - currentColorR) + MathHelper.square(g - currentColorG) + MathHelper.square(b - currentColorB);
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
	public NbtElement getNbt()
	{
		return NbtInt.of(hexCode);
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