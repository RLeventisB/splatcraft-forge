package net.splatcraft.items;

import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftSounds;
import org.jetbrains.annotations.NotNull;

public class PowerEggCanItem extends Item
{
	public PowerEggCanItem()
	{
		super(new Properties().stacksTo(16));
	}
	@Override
	public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand handIn)
	{
		ItemStack itemstack = player.getItemInHand(handIn);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.powerEggCanOpen, SoundSource.PLAYERS, 0.5F, 0.4F / (player.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!world.isClientSide())
		{
			double d0 = player.getEyeY() - (double) 0.3F;
			ItemEntity itementity = new ItemEntity(world, player.getX(), d0, player.getZ(), new ItemStack(SplatcraftItems.powerEgg.get(), (world.random.nextInt(4) + 1) * 10));
			itementity.setNoPickUpDelay();
			itementity.setThrower(player);
			
			float f = world.random.nextFloat() * 0.5F;
			float f1 = world.random.nextFloat() * ((float) Math.PI * 2F);
			itementity.setDeltaMovement(-Math.sin(f1) * f, 0.2F, Math.cos(f1) * f);
			
			world.addFreshEntity(itementity);
		}
		
		player.awardStat(Stats.ITEM_USED.get(this));
		if (!player.isCreative())
		{
			itemstack.shrink(1);
		}
		
		return InteractionResultHolder.sidedSuccess(itemstack, world.isClientSide());
	}
}