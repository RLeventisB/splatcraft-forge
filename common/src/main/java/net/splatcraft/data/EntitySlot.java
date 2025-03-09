package net.splatcraft.data;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
	StreamCodec<ByteBuf, EntitySlot> SERIALIZER_PACKET_CODEC = new StreamCodec<>()
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
		if (comparator.areEquals(entity.getMainHandItem(), stack))
		{
			return createFor(InteractionHand.MAIN_HAND, comparator);
		}
		if (comparator.areEquals(entity.getOffhandItem(), stack))
		{
			return createFor(InteractionHand.OFF_HAND, comparator);
		}
		if (entity instanceof Player player)
		{
			Inventory inventory = player.getInventory();
			for (int i = 0; i < inventory.getContainerSize(); i++)
			{
				if (comparator.areEquals(inventory.getItem(i), stack))
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
	static EntitySlot createFor(InteractionHand hand)
	{
		return createFor(hand, DEFAULT_COMPARATOR);
	}
	static EntitySlot createFor(InteractionHand hand, StackComparator comparator)
	{
		return new EntityHandSlot(hand, comparator);
	}
	boolean isSlotFor(LivingEntity entity, ItemStack stack);
	SlotId getId();
	boolean isValidForEntity(LivingEntity entity);
	enum StackComparator implements StringRepresentable
	{
		ONLY_ITEM(ItemStack::isSameItem),
		INCLUDE_COMPONENTS(ItemStack::isSameItemSameComponents),
		INCLUDE_REFERENCE(ItemStack::matches);
		public static final StreamCodec<ByteBuf, StackComparator> PACKET_CODEC = CodecUtils.createEnumPacketCodec(StackComparator::values);
		public static final Codec<StackComparator> CODEC = StringRepresentable.fromEnum(StackComparator::values);
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
		public String getSerializedName()
		{
			return name();
		}
	}
	enum SlotId implements StringRepresentable
	{
		ENTITY_HAND(EntityHandSlot.CODEC, EntityHandSlot.PACKET_CODEC),
		PLAYER_SLOT(PlayerInventorySlot.CODEC, PlayerInventorySlot.PACKET_CODEC);
		public static final StreamCodec<ByteBuf, SlotId> PACKET_CODEC = CodecUtils.createEnumPacketCodec(SlotId::values);
		public static final Codec<SlotId> CODEC = StringRepresentable.fromEnum(SlotId::values);
		public final MapCodec<EntitySlot> codec;
		public final StreamCodec<ByteBuf, EntitySlot> packetCodec;
		SlotId(MapCodec<?> codec, StreamCodec<ByteBuf, ?> packetCodec)
		{
			this.codec = (MapCodec<EntitySlot>) codec;
			this.packetCodec = (StreamCodec<ByteBuf, EntitySlot>) packetCodec;
		}
		@Override
		public String getSerializedName()
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
			CodecUtils.Codecs.HAND_CODEC.fieldOf("hand").forGetter(v -> v.hand),
			StackComparator.CODEC.fieldOf("comparator").forGetter(v -> v.comparator)
		).apply(inst, EntityHandSlot::new));
		public static final StreamCodec<ByteBuf, EntityHandSlot> PACKET_CODEC = StreamCodec.composite(
			CodecUtils.Codecs.PACKET_HAND, v -> v.hand,
			StackComparator.PACKET_CODEC, v -> v.comparator,
			EntityHandSlot::new
		);
		private final InteractionHand hand;
		private final StackComparator comparator;
		public EntityHandSlot(InteractionHand hand, StackComparator comparator)
		{
			this.hand = hand;
			this.comparator = comparator;
		}
		@Override
		public boolean isSlotFor(LivingEntity entity, ItemStack stack)
		{
			return comparator.areEquals(entity.getItemInHand(hand), stack);
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
		public static final StreamCodec<ByteBuf, PlayerInventorySlot> PACKET_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, v -> v.slot,
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
			return comparator.areEquals(((Player) entity).getInventory().getItem(slot), stack);
		}
		@Override
		public SlotId getId()
		{
			return SlotId.PLAYER_SLOT;
		}
		@Override
		public boolean isValidForEntity(LivingEntity entity)
		{
			return entity instanceof Player;
		}
	}
}
