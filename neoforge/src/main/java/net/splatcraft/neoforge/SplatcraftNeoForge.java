package net.splatcraft.neoforge;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.registries.IdMappingEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.handlers.ClientSetupHandler;
import net.splatcraft.client.handlers.JumpLureHudHandler;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.client.handlers.RendererHandler;
import net.splatcraft.client.particles.InkExplosionParticle;
import net.splatcraft.client.particles.InkSplashParticle;
import net.splatcraft.client.particles.InkTerrainParticle;
import net.splatcraft.client.particles.SquidSoulParticle;
import net.splatcraft.handlers.ChunkInkHandler;
import net.splatcraft.handlers.SplatcraftCommonHandler;
import net.splatcraft.handlers.SquidFormHandler;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.registries.SplatcraftItems;
import net.splatcraft.registries.SplatcraftParticleTypes;
import net.splatcraft.registries.SplatcraftRegistries;
import net.splatcraft.registries.neoforge.SplatcraftEntitiesImpl;
import org.jetbrains.annotations.NotNull;

@Mod(Splatcraft.MODID)
public final class SplatcraftNeoForge
{
	private final IEventBus modBus;
	public SplatcraftNeoForge(IEventBus modBus)
	{
		this.modBus = modBus;
		// Run our common setup.
		Splatcraft.init();
		SplatcraftEntitiesImpl.REGISTRY.register(modBus);
		modBus.addListener(SplatcraftNeoForge::onRegistryUnlocked);
		modBus.addListener(SplatcraftNeoForge::registerGuiOverlays);
		modBus.addListener(SplatcraftNeoForge::registerClientExtensions);
		modBus.addListener(SplatcraftNeoForge::registerParticleProviders);
		modBus.addListener(SplatcraftNeoForge::registerColorHandlersItem);
		modBus.addListener(SplatcraftNeoForge::registerColorHandlersBlock);
		
		NeoForge.EVENT_BUS.addListener(SplatcraftNeoForge::afterRegistryEvent);
		NeoForge.EVENT_BUS.addListener(SplatcraftNeoForge::onMobDrops);
		NeoForge.EVENT_BUS.addListener(SplatcraftNeoForge::onGamemodeChange);
		NeoForge.EVENT_BUS.addListener(SplatcraftNeoForge::onInputUpdate);
		NeoForge.EVENT_BUS.addListener(SplatcraftNeoForge::onChunkWatch);
		
		if (Platform.getEnvironment() == Env.CLIENT)
			Splatcraft.initClient();
	}
	private static void registerColorHandlersItem(RegisterColorHandlersEvent.Item event)
	{
		ClientSetupHandler.initItemColors(event.getItemColors());
	}
	private static void registerColorHandlersBlock(RegisterColorHandlersEvent.Block event)
	{
		ClientSetupHandler.initBlockColors(event.getBlockColors());
	}
	private static void registerParticleProviders(RegisterParticleProvidersEvent event)
	{
		event.registerSpriteSet(SplatcraftParticleTypes.INK_SPLASH, InkSplashParticle.Factory::new);
		event.registerSpriteSet(SplatcraftParticleTypes.INK_EXPLOSION, InkExplosionParticle.Factory::new);
		event.registerSpriteSet(SplatcraftParticleTypes.SQUID_SOUL, SquidSoulParticle.Factory::new);
		event.registerSpriteSet(SplatcraftParticleTypes.INK_TERRAIN, InkTerrainParticle.Factory::new);
	}
	private static void onChunkWatch(ChunkWatchEvent.Sent event)
	{
		ChunkInkHandler.sendChunkData(event.getPlayer().networkHandler, event.getLevel(), event.getLevel().getChunk(event.getPos().x, event.getPos().z));
	}
	private static void onMobDrops(LivingDropsEvent event)
	{
		SplatcraftCommonHandler.onLivingDeathDrops(event.getEntity(), event.getDrops());
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
	public static void afterRegistryEvent(IdMappingEvent event)
	{
		SplatcraftRegistries.afterRegister();
	}
	public static void onRegistryUnlocked(NewRegistryEvent event)
	{
		SplatcraftRegistries.register();
	}
	public static void registerGuiOverlays(RegisterGuiLayersEvent event)
	{
		event.registerAbove(VanillaGuiLayers.CROSSHAIR, Splatcraft.identifierOf("overlay"), RendererHandler::renderGui);
		event.registerAbove(VanillaGuiLayers.HOTBAR, Splatcraft.identifierOf("jump_lure"), JumpLureHudHandler::renderGui);
	}
}
