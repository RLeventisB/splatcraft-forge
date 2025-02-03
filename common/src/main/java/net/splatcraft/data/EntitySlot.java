package net.splatcraft.data;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.splatcraft.util.CodecUtils;

import java.util.function.BiPredicate;
import java.util.stream.Stream;

public interface EntitySlot
{
	StackComparator DEFAULT_COMPARATOR = StackComparator.INCLUDE_REFERENCE;
	Codec<EntitySlot> SERIALIZER_CODEC = new MapCodec<EntitySlot>()
	{
		@Override
		public <T> RecordBuilder<T> encode(EntitySlot input, DynamicOps<T> ops, RecordBuilder<T> builder)
		{
			SlotId id = input.getId();
			builder.add("slot_id", SlotId.CODEC.encodeStart(ops, id));
			return id.codec.encode(input, ops, builder);
		}
		@Override
		public <T> DataResult<EntitySlot> decode(DynamicOps<T> ops, MapLike<T> input)
		{
			T slotIdData = input.get("slot_id");
			if (slotIdData == null)
			{
				throw new AssertionError("No key slot_id is present in " + input);
			}
			DataResult<SlotId> result = SlotId.CODEC.parse(ops, slotIdData);
			if (result.isError())
			{
				throw new AssertionError("Error upon reading slot index of " + slotIdData);
			}
			return result.getOrThrow().codec.decode(ops, input);
		}
		@Override
		public <T> Stream<T> keys(DynamicOps<T> ops)
		{
			return Stream.of(ops.createString("id"), ops.createString("data"));
		}
	}.codec();
	PacketCodec<ByteBuf, EntitySlot> SERIALIZER_PACKET_CODEC = new PacketCodec<>()
	{
		@Override
		public EntitySlot decode(ByteBuf buf)
		{
			SlotId id = SlotId.PACKET_CODEC.decode(buf);
			return id.packetCodec.decode(buf);
		}
		@Override
		public void encode(ByteBuf buf, EntitySlot value)
		{
			SlotId id = value.getId();
			SlotId.PACKET_CODEC.encode(buf, id);
			id.packetCodec.encode(buf, value);
		}
	};
	static EntitySlot createFor(LivingEntity entity, ItemStack stack)
	{
		return createFor(entity, stack, DEFAULT_COMPARATOR);
	}
	static EntitySlot createFor(LivingEntity entity, ItemStack stack, StackComparator comparator)
	{
		if (comparator.areEquals(entity.getMainHandStack(), stack))
		{
			return createFor(Hand.MAIN_HAND, comparator);
		}
		if (comparator.areEquals(entity.getOffHandStack(), stack))
		{
			return createFor(Hand.OFF_HAND, comparator);
		}
		if (entity instanceof PlayerEntity player)
		{
			PlayerInventory inventory = player.getInventory();
			for (int i = 0; i < inventory.size(); i++)
			{
				if (comparator.areEquals(inventory.getStack(i), stack))
				{
					return createFor(i, comparator);
				}
			}
		}
		
		throw new AssertionError("The given stack isn't contained by the given entity");
	}
	static EntitySlot createFor(int slot)
	{
		return createFor(slot, DEFAULT_COMPARATOR);
	}
	static EntitySlot createFor(int slot, StackComparator comparator)
	{
		return new PlayerInventorySlot(slot, comparator);
	}
	static EntitySlot createFor(Hand hand)
	{
		return createFor(hand, DEFAULT_COMPARATOR);
	}
	static EntitySlot createFor(Hand hand, StackComparator comparator)
	{
		return new EntityHandSlot(hand, comparator);
	}
	boolean isSlotFor(LivingEntity entity, ItemStack stack);
	SlotId getId();
	boolean isValidForEntity(LivingEntity entity);
	enum StackComparator implements StringIdentifiable
	{
		ONLY_ITEM(ItemStack::areItemsEqual),
		INCLUDE_COMPONENTS(ItemStack::areItemsAndComponentsEqual),
		INCLUDE_REFERENCE(ItemStack::areEqual);
		public static final PacketCodec<ByteBuf, StackComparator> PACKET_CODEC = CodecUtils.createEnumPacketCodec(StackComparator::values);
		public static final Codec<StackComparator> CODEC = StringIdentifiable.createCodec(StackComparator::values);
		private final BiPredicate<ItemStack, ItemStack> comparer;
		StackComparator(BiPredicate<ItemStack, ItemStack> comparer)
		{
			this.comparer = comparer;
		}
		public boolean areEquals(ItemStack stack, ItemStack otherStack)
		{
			return comparer.test(stack, otherStack);
		}
		@Override
		public String asString()
		{
			return name();
		}
	}
	enum SlotId implements StringIdentifiable
	{
		ENTITY_HAND(EntityHandSlot.CODEC, EntityHandSlot.PACKET_CODEC),
		PLAYER_SLOT(PlayerInventorySlot.CODEC, PlayerInventorySlot.PACKET_CODEC);
		public static final PacketCodec<ByteBuf, SlotId> PACKET_CODEC = CodecUtils.createEnumPacketCodec(SlotId::values);
		public static final Codec<SlotId> CODEC = StringIdentifiable.createCodec(SlotId::values);
		public final MapCodec<EntitySlot> codec;
		public final PacketCodec<ByteBuf, EntitySlot> packetCodec;
		SlotId(MapCodec<?> codec, PacketCodec<ByteBuf, ?> packetCodec)
		{
			this.codec = (MapCodec<EntitySlot>) codec;
			this.packetCodec = (PacketCodec<ByteBuf, EntitySlot>) packetCodec;
		}
		@Override
		public String asString()
		{
			return name();
		}
		public MapCodec<? extends EntitySlot> getCodec()
		{
			return codec;
		}
	}
	class EntityHandSlot implements EntitySlot
	{
		public static final MapCodec<EntityHandSlot> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
			CodecUtils.HAND_CODEC.fieldOf("hand").forGetter(v -> v.hand),
			StackComparator.CODEC.fieldOf("comparator").forGetter(v -> v.comparator)
		).apply(inst, EntityHandSlot::new));
		public static final PacketCodec<ByteBuf, EntityHandSlot> PACKET_CODEC = PacketCodec.tuple(
			CodecUtils.PACKET_HAND, v -> v.hand,
			StackComparator.PACKET_CODEC, v -> v.comparator,
			EntityHandSlot::new
		);
		private final Hand hand;
		private final StackComparator comparator;
		public EntityHandSlot(Hand hand, StackComparator comparator)
		{
			this.hand = hand;
			this.comparator = comparator;
		}
		@Override
		public boolean isSlotFor(LivingEntity entity, ItemStack stack)
		{
			return comparator.areEquals(entity.getStackInHand(hand), stack);
		}
		@Override
		public SlotId getId()
		{
			return SlotId.ENTITY_HAND;
		}
		@Override
		public boolean isValidForEntity(LivingEntity entity)
		{
			return true;
		}
	}
	class PlayerInventorySlot implements EntitySlot
	{
		public static final MapCodec<PlayerInventorySlot> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
			Codec.INT.fieldOf("slot").forGetter(v -> v.slot),
			StackComparator.CODEC.fieldOf("comparator").forGetter(v -> v.comparator)
		).apply(inst, PlayerInventorySlot::new));
		public static final PacketCodec<ByteBuf, PlayerInventorySlot> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.INTEGER, v -> v.slot,
			StackComparator.PACKET_CODEC, v -> v.comparator,
			PlayerInventorySlot::new
		);
		private final int slot;
		private final StackComparator comparator;
		public PlayerInventorySlot(int slot, StackComparator comparator)
		{
			this.slot = slot;
			this.comparator = comparator;
		}
		@Override
		public boolean isSlotFor(LivingEntity entity, ItemStack stack)
		{
			return comparator.areEquals(((PlayerEntity) entity).getInventory().getStack(slot), stack);
		}
		@Override
		public SlotId getId()
		{
			return SlotId.PLAYER_SLOT;
		}
		@Override
		public boolean isValidForEntity(LivingEntity entity)
		{
			return entity instanceof PlayerEntity;
		}
	}
}
