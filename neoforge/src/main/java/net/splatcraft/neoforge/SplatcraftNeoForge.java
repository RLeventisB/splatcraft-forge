package net.splatcraft.neoforge;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.handlers.SquidFormHandler;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftRegistries;
import org.jetbrains.annotations.NotNull;

@Mod(Splatcraft.MODID)
public final class SplatcraftNeoForge
{
	public SplatcraftNeoForge(IEventBus modBus)
	{
		// Run our common setup.
		Splatcraft.init();
		modBus.addListener(this::onRegistryUnlocked);
		modBus.addListener(this::registerGuiOverlays);
		modBus.addListener(SplatcraftNeoForge::registerClientExtensions);
		
		NeoForge.EVENT_BUS.addListener(SplatcraftNeoForge::onGamemodeChange);
		NeoForge.EVENT_BUS.addListener(SplatcraftNeoForge::onInputUpdate);
	}
	private static void registerClientExtensions(RegisterClientExtensionsEvent event)
	{
		event.registerItem(new IClientItemExtensions()
		                   {
			                   @Override
			                   public @NotNull BipedEntityModel<?> getHumanoidArmorModel(@NotNull LivingEntity livingEntity, @NotNull ItemStack itemStack, @NotNull EquipmentSlot armorSlot, @NotNull BipedEntityModel<?> original)
			                   {
				                   if (!InkTankItem.initModels) //i have NO idea where else to put this
				                   {
					                   InkTankItem.initModels = true;
					                   SplatcraftItems.registerArmorModels();
				                   }
				                   
				                   if (!(itemStack.getItem() instanceof InkTankItem item))
				                   {
					                   return DEFAULT.getHumanoidArmorModel(livingEntity, itemStack, armorSlot, original);
				                   }
				                   
				                   if (item.model == null)
				                   {
					                   return DEFAULT.getHumanoidArmorModel(livingEntity, itemStack, armorSlot, original);
				                   }
				                   
				                   if (!itemStack.isEmpty())
				                   {
					                   if (itemStack.getItem() instanceof InkTankItem)
					                   {
						                   item.model.rightLeg.visible = armorSlot == EquipmentSlot.LEGS || armorSlot == EquipmentSlot.FEET;
						                   item.model.leftLeg.visible = armorSlot == EquipmentSlot.LEGS || armorSlot == EquipmentSlot.FEET;
						                   
						                   item.model.body.visible = armorSlot == EquipmentSlot.CHEST;
						                   item.model.leftArm.visible = armorSlot == EquipmentSlot.CHEST;
						                   item.model.rightArm.visible = armorSlot == EquipmentSlot.CHEST;
						                   
						                   item.model.head.visible = armorSlot == EquipmentSlot.HEAD;
						                   item.model.hat.visible = armorSlot == EquipmentSlot.HEAD;
						                   
						                   item.model.sneaking = original.sneaking;
						                   item.model.riding = original.riding;
						                   item.model.child = original.child;
						                   
						                   item.model.rightArmPose = original.rightArmPose;
						                   item.model.leftArmPose = original.leftArmPose;
						                   
						                   item.model.setInkLevels(InkTankItem.getInkAmount(itemStack) / item.capacity);
						                   
						                   return item.model;
					                   }
				                   }
				                   
				                   return DEFAULT.getHumanoidArmorModel(livingEntity, itemStack, armorSlot, original);
			                   }
		                   }, SplatcraftItems.inkTank.get(),
			SplatcraftItems.armoredInkTank.get(),
			SplatcraftItems.inkTankJr.get(),
			SplatcraftItems.classicInkTank.get()
		);
	}
	private static void onInputUpdate(MovementInputUpdateEvent event)
	{
		PlayerMovementHandler.onInputUpdate((ClientPlayerEntity) event.getEntity(), event.getInput());
	}
	private static void onGamemodeChange(PlayerEvent.PlayerChangeGameModeEvent event)
	{
		SquidFormHandler.onGameModeSwitch(event.getEntity(), event.getNewGameMode());
	}
	public void onRegistryUnlocked(NewRegistryEvent event)
	{
		SplatcraftRegistries.register();
	}
	public void registerGuiOverlays(RegisterGuiLayersEvent event)
	{
		event.registerAbove(VanillaGuiLayers.CROSSHAIR, Splatcraft.identifierOf("overlay"), RendererHandler::renderGui);
		event.registerAbove(VanillaGuiLayers.HOTBAR, Splatcraft.identifierOf("jump_lure"), JumpLureHudHandler::renderGui);
	}
}
