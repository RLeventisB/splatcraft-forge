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
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public interface EntitySlot
{
	StackComparator DEFAULT_COMPARATOR = StackComparator.INCLUDE_REFERENCE_AND_COMPONENTS;
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
	StreamCodec<ByteBuf, EntitySlot> SERIALIZER_STREAM_CODEC = new StreamCodec<>()
	{
		@Override
		public @NotNull EntitySlot decode(@NotNull ByteBuf buf)
		{
			SlotId id = SlotId.STREAM_CODEC.decode(buf);
			return id.packetCodec.decode(buf);
		}
		@Override
		public void encode(@NotNull ByteBuf buf, EntitySlot value)
		{
			SlotId id = value.getId();
			SlotId.STREAM_CODEC.encode(buf, id);
			id.packetCodec.encode(buf, value);
		}
	};
	EntitySlot EMPTY = new EntitySlot()
	{
		@Override
		public boolean isItemForSlot(LivingEntity entity, ItemStack stack)
		{
			return false;
		}
		@Override
		public boolean isItemForSlot(LivingEntity entity, InteractionHand hand)
		{
			return false;
		}
		@Override
		public Optional<ItemStack> tryGetItemFrom(LivingEntity entity)
		{
			return Optional.empty();
		}
		@Override
		public SlotId getId()
		{
			return SlotId.EMPTY;
		}
		@Override
		public boolean isValidForEntity(LivingEntity entity)
		{
			return false;
		}
	};
	// todo: more descriptive names for these methods pls
	static EntitySlot createForUsed(LivingEntity entity)
	{
		if (entity instanceof Player player)
			return createWithSlot(player.getInventory().selected, player.getUsedItemHand());
		return createWithHand(entity.getUsedItemHand(), DEFAULT_COMPARATOR);
	}
	static EntitySlot createForUsed(LivingEntity entity, InteractionHand hand)
	{
		if (entity instanceof Player player)
			return createWithSlot(player.getInventory().selected, hand);
		return createWithHand(hand, DEFAULT_COMPARATOR);
	}
	static EntitySlot searchAndCreateWithStack(LivingEntity entity, ItemStack stack)
	{
		return searchAndCreateWithStack(entity, stack, DEFAULT_COMPARATOR);
	}
	static EntitySlot searchAndCreateWithStack(LivingEntity entity, ItemStack stack, StackComparator comparator)
	{
		// prioritize player because if this gets sent to a packet and the player changes slots it could create a desync :(
		if (entity instanceof Player player)
		{
			Inventory inventory = player.getInventory();
			for (int i = 0; i < inventory.getContainerSize(); i++)
			{
				if (comparator.areEquals(inventory.getItem(i), stack))
				{
					// fix since sometimes we need a selected index that is within the hotbar
					if (i == Inventory.SLOT_OFFHAND)
						return new PlayerInventorySlot(inventory.selected, InteractionHand.OFF_HAND, comparator);
					return createWithSlot(i, InteractionHand.MAIN_HAND, comparator);
				}
			}
		}
		if (comparator.areEquals(entity.getMainHandItem(), stack))
		{
			return createWithHand(InteractionHand.MAIN_HAND, comparator);
		}
		if (comparator.areEquals(entity.getOffhandItem(), stack))
		{
			return createWithHand(InteractionHand.OFF_HAND, comparator);
		}
		
		throw new AssertionError("The given stack isn't contained by the given entity");
	}
	static EntitySlot createWithSlot(int slot)
	{
		return createWithSlot(slot, InteractionHand.MAIN_HAND, DEFAULT_COMPARATOR);
	}
	static EntitySlot createWithSlot(int slot, InteractionHand hand)
	{
		return createWithSlot(slot, hand, DEFAULT_COMPARATOR);
	}
	static EntitySlot createWithSlot(int slot, InteractionHand hand, StackComparator comparator)
	{
		return new PlayerInventorySlot(slot, hand, comparator);
	}
	static EntitySlot createWithHand(InteractionHand hand)
	{
		return createWithHand(hand, DEFAULT_COMPARATOR);
	}
	static EntitySlot createWithHand(InteractionHand hand, StackComparator comparator)
	{
		return new EntityHandSlot(hand, comparator);
	}
	static EntitySlot createWithHandToSlot(LivingEntity user, InteractionHand hand)
	{
		return createWithHandToSlot(user, hand, DEFAULT_COMPARATOR);
	}
	static EntitySlot createWithHandToSlot(LivingEntity user, InteractionHand hand, StackComparator comparator)
	{
		if (user instanceof Player player)
		{
			return new PlayerInventorySlot(player.getInventory().selected, hand, comparator);
		}
		if (hand != null)
			return new EntityHandSlot(hand, comparator);
		return EMPTY;
	}
	boolean isItemForSlot(LivingEntity entity, ItemStack stack);
	boolean isItemForSlot(LivingEntity entity, InteractionHand hand);
	Optional<ItemStack> tryGetItemFrom(LivingEntity entity);
	SlotId getId();
	boolean isValidForEntity(LivingEntity entity);
	enum StackComparator implements StringRepresentable
	{
		ONLY_ITEM(ItemStack::isSameItem),
		INCLUDE_COMPONENTS(ItemStack::isSameItemSameComponents),
		INCLUDE_REFERENCE_AND_COMPONENTS(ItemStack::matches),
		ONLY_REFERENCE((x, y) -> x == y);
		public static final StreamCodec<ByteBuf, StackComparator> STREAM_CODEC = CodecUtils.createEnumPacketCodec(StackComparator::values);
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
		public @NotNull String getSerializedName()
		{
			return name();
		}
	}
	enum SlotId implements StringRepresentable
	{
		ENTITY_HAND(EntityHandSlot.CODEC, EntityHandSlot.STREAM_CODEC),
		PLAYER_SLOT(PlayerInventorySlot.CODEC, PlayerInventorySlot.STREAM_CODEC),
		EMPTY(MapCodec.unit(EntitySlot.EMPTY), StreamCodec.unit(EntitySlot.EMPTY));
		public static final StreamCodec<ByteBuf, SlotId> STREAM_CODEC = CodecUtils.createEnumPacketCodec(SlotId::values);
		public static final Codec<SlotId> CODEC = StringRepresentable.fromEnum(SlotId::values);
		public final MapCodec<EntitySlot> codec;
		public final StreamCodec<ByteBuf, EntitySlot> packetCodec;
		SlotId(MapCodec<? extends EntitySlot> codec, StreamCodec<ByteBuf, ? extends EntitySlot> packetCodec)
		{
			this.codec = (MapCodec<EntitySlot>) codec;
			this.packetCodec = (StreamCodec<ByteBuf, EntitySlot>) packetCodec;
		}
		@Override
		public @NotNull String getSerializedName()
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
		public static final StreamCodec<ByteBuf, EntityHandSlot> STREAM_CODEC = StreamCodec.composite(
			CodecUtils.Codecs.PACKET_HAND, v -> v.hand,
			StackComparator.STREAM_CODEC, v -> v.comparator,
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
		public boolean isItemForSlot(LivingEntity entity, ItemStack stack)
		{
			return comparator.areEquals(entity.getItemInHand(hand), stack);
		}
		@Override
		public boolean isItemForSlot(LivingEntity entity, InteractionHand hand)
		{
			return hand == this.hand;
		}
		@Override
		public Optional<ItemStack> tryGetItemFrom(LivingEntity entity)
		{
			return Optional.of(entity.getItemInHand(hand));
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
			Codec.INT.fieldOf("slot").forGetter(v -> v.selectedSlot),
			CodecUtils.Codecs.HAND_CODEC.fieldOf("hand").forGetter(v -> v.hand),
			StackComparator.CODEC.fieldOf("comparator").forGetter(v -> v.comparator)
		).apply(inst, PlayerInventorySlot::new));
		public static final StreamCodec<ByteBuf, PlayerInventorySlot> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, v -> v.selectedSlot,
			CodecUtils.Codecs.PACKET_HAND, v -> v.hand,
			StackComparator.STREAM_CODEC, v -> v.comparator,
			PlayerInventorySlot::new
		);
		private final int selectedSlot;
		private final InteractionHand hand;
		private final StackComparator comparator;
		public PlayerInventorySlot(int slot, InteractionHand hand, StackComparator comparator)
		{
			this.selectedSlot = slot;
			this.hand = hand;
			this.comparator = comparator;
		}
		public int getSlotIndex()
		{
			return selectedSlot;
		}
		@Override
		public boolean isItemForSlot(LivingEntity entity, ItemStack stack)
		{
			if (hand == InteractionHand.OFF_HAND)
				return comparator.areEquals(entity.getOffhandItem(), stack);
			
			if (!(entity instanceof Player player))
				return false;
			
			return comparator.areEquals(player.getInventory().getItem(selectedSlot), stack);
		}
		@Override
		public boolean isItemForSlot(LivingEntity entity, InteractionHand hand)
		{
			if (!(entity instanceof Player player))
				return false;
			
			return player.getInventory().selected == selectedSlot && this.hand == hand;
		}
		@Override
		public Optional<ItemStack> tryGetItemFrom(LivingEntity entity)
		{
			if (entity instanceof Player player)
				return Optional.of(player.getInventory().getItem(selectedSlot));
			
			return Optional.empty();
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
