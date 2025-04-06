package net.splatcraft.platform.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.splatcraft.platform.event.types.CompoundEvent;
import net.splatcraft.platform.event.types.ConsumerEvent;
import net.splatcraft.platform.event.types.SimpleEvent;

import java.util.UUID;

public interface InteractionEvents
{
	@FunctionalInterface
	interface ClientLeftClickAir extends ConsumerEvent.Hexa<Player, InteractionHand, Direction, ItemStack, Level, BlockPos>
	{
		void register(Player player, InteractionHand usedHand, Direction blockFace, ItemStack itemStack, Level level, BlockPos blockClicked);
		default void invoke(Player parameter1, InteractionHand parameter2, Direction parameter3, ItemStack parameter4, Level parameter5, BlockPos parameter6)
		{
			register(parameter1, parameter2, parameter3, parameter4, parameter5, parameter6);
		}
	}
	@FunctionalInterface
	interface ClientRightClickAir extends ConsumerEvent.Hexa<Player, InteractionHand, Direction, ItemStack, Level, BlockPos>
	{
		void register(Player player, InteractionHand usedHand, Direction blockFace, ItemStack itemStack, Level level, BlockPos blockClicked);
		default void invoke(Player parameter1, InteractionHand parameter2, Direction parameter3, ItemStack parameter4, Level parameter5, BlockPos parameter6)
		{
			register(parameter1, parameter2, parameter3, parameter4, parameter5, parameter6);
		}
	}
	@FunctionalInterface
	interface LeftClickBlock extends SimpleEvent.Hexa<Player, InteractionHand, Direction, ItemStack, Level, BlockPos>
	{
		EventResult register(Player player, InteractionHand usedHand, Direction blockFace, ItemStack itemStack, Level level, BlockPos blockClicked);
		default EventResult invoke(Player parameter1, InteractionHand parameter2, Direction parameter3, ItemStack parameter4, Level parameter5, BlockPos parameter6)
		{
			return register(parameter1, parameter2, parameter3, parameter4, parameter5, parameter6);
		}
	}
	@FunctionalInterface
	interface RightClickBlock extends SimpleEvent.Hexa<Player, InteractionHand, Direction, ItemStack, Level, BlockPos>
	{
		EventResult register(Player player, InteractionHand usedHand, Direction blockFace, ItemStack itemStack, Level level, BlockPos blockClicked);
		default EventResult invoke(Player parameter1, InteractionHand parameter2, Direction parameter3, ItemStack parameter4, Level parameter5, BlockPos parameter6)
		{
			return register(parameter1, parameter2, parameter3, parameter4, parameter5, parameter6);
		}
	}
	@FunctionalInterface
	interface RightClickItem extends SimpleEvent.Hexa<Player, InteractionHand, Direction, ItemStack, Level, BlockPos>
	{
		EventResult register(Player player, InteractionHand usedHand, Direction blockFace, ItemStack itemStack, Level level, BlockPos blockClicked);
		default EventResult invoke(Player parameter1, InteractionHand parameter2, Direction parameter3, ItemStack parameter4, Level parameter5, BlockPos parameter6)
		{
			return register(parameter1, parameter2, parameter3, parameter4, parameter5, parameter6);
		}
	}
	@FunctionalInterface
	interface InteractEntity extends SimpleEvent.Hexa<Player, InteractionHand, Direction, Entity, Level, BlockPos>
	{
		EventResult invoke(Player player, InteractionHand usedHand, Direction blockFace, Entity target, Level level, BlockPos blockClicked);
	}
	@FunctionalInterface
	interface ClientChatReceive extends CompoundEvent.Tri<ChatType.Bound, Component, UUID, Component>
	{
		CompoundEventResult<Component> invoke(ChatType.Bound chatType, Component message, UUID sender);
	}
	@FunctionalInterface
	interface BlockBreak extends SimpleEvent.Tetra<Player, LevelAccessor, BlockPos, BlockState>
	{
		EventResult invoke(Player player, LevelAccessor level, BlockPos blockPosition, BlockState state);
	}
}

