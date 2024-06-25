package net.splatcraft.forge.network.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.splatcraft.forge.items.weapons.DualieItem;

import java.util.UUID;

public class DodgeRollPacket extends PlayC2SPacket
{
	UUID target;
	ItemStack activeDualie;
	int maxRolls;
	Vec2 rollDirection;
	public DodgeRollPacket(UUID target, ItemStack activeDualie, int maxRolls, Vec2 rollDirection)
	{
		this.target = target;
		this.activeDualie = activeDualie;
		this.maxRolls = maxRolls;
		this.rollDirection = rollDirection;
	}
	public static DodgeRollPacket decode(FriendlyByteBuf buffer)
	{
		return new DodgeRollPacket(buffer.readUUID(), buffer.readItem(), buffer.readInt(), new Vec2(buffer.readFloat(), buffer.readFloat()));
	}
	@Override
	public void execute(Player player)
	{
		Player target = player.level.getPlayerByUUID(this.target);
		((DualieItem) activeDualie.getItem()).performRoll(target, activeDualie, maxRolls, rollDirection, false);
	}
	@Override
	public void encode(FriendlyByteBuf buffer)
	{
		buffer.writeUUID(target);
		buffer.writeItem(activeDualie);
		buffer.writeInt(maxRolls);
		buffer.writeFloat(rollDirection.x); // important note dont use writeDouble so your rollDirection.x isnt't 3.16345E19 (god damn it minecraft why did you make it so Vec2 uses floats but Vec3 uses doubles)
		buffer.writeFloat(rollDirection.y);
	}
}
