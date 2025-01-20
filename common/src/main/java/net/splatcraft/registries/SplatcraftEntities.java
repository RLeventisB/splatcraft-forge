package net.splatcraft.registries;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.splatcraft.Splatcraft;
import net.splatcraft.client.layer.InkAccessoryLayer;
import net.splatcraft.client.layer.InkOverlayLayer;
import net.splatcraft.client.layer.PlayerInkColoredSkinLayer;
import net.splatcraft.client.models.InkSquidModel;
import net.splatcraft.client.models.SquidBumperModel;
import net.splatcraft.client.models.inktanks.ArmoredInkTankModel;
import net.splatcraft.client.models.inktanks.ClassicInkTankModel;
import net.splatcraft.client.models.inktanks.InkTankJrModel;
import net.splatcraft.client.models.inktanks.InkTankModel;
import net.splatcraft.client.models.projectiles.BlasterInkProjectileModel;
import net.splatcraft.client.models.projectiles.InkProjectileModel;
import net.splatcraft.client.models.projectiles.RollerInkProjectileModel;
import net.splatcraft.client.models.projectiles.ShooterInkProjectileModel;
import net.splatcraft.client.models.subs.BurstBombModel;
import net.splatcraft.client.models.subs.CurlingBombModel;
import net.splatcraft.client.models.subs.SplatBombModel;
import net.splatcraft.client.models.subs.SuctionBombModel;
import net.splatcraft.client.renderer.*;
import net.splatcraft.client.renderer.subs.BurstBombRenderer;
import net.splatcraft.client.renderer.subs.CurlingBombRenderer;
import net.splatcraft.client.renderer.subs.SplatBombRenderer;
import net.splatcraft.client.renderer.subs.SuctionBombRenderer;
import net.splatcraft.entities.*;
import net.splatcraft.entities.subs.BurstBombEntity;
import net.splatcraft.entities.subs.CurlingBombEntity;
import net.splatcraft.entities.subs.SplatBombEntity;
import net.splatcraft.entities.subs.SuctionBombEntity;
import net.splatcraft.util.CommonUtils;

import java.util.Map;
import java.util.Objects;

public class SplatcraftEntities
{
	protected static final DeferredRegister<EntityType<?>> REGISTRY = Splatcraft.deferredRegistryOf(Registries.ENTITY_TYPE);
	public static final RegistrySupplier<EntityType<InkSquidEntity>> INK_SQUID = create("ink_squid", InkSquidEntity::new, SpawnGroup.AMBIENT, 0.6f, 0.5f);
	public static final RegistrySupplier<EntityType<InkDropEntity>> INK_DROP = create("ink_drop", InkDropEntity::new, SpawnGroup.MISC, InkDropEntity.DROP_SIZE, InkDropEntity.DROP_SIZE);
	public static final RegistrySupplier<EntityType<InkProjectileEntity>> INK_PROJECTILE = create("ink_projectile", InkProjectileEntity::new, SpawnGroup.MISC);
	public static final RegistrySupplier<EntityType<SquidBumperEntity>> SQUID_BUMPER = create("squid_bumper", SquidBumperEntity::new, SpawnGroup.MISC, 0.6f, 1.8f);
	public static final RegistrySupplier<EntityType<SpawnShieldEntity>> SPAWN_SHIELD = create("spawn_shield", SpawnShieldEntity::new, SpawnGroup.MISC, 1, 1);
	//Sub Weapons
	public static final RegistrySupplier<EntityType<BurstBombEntity>> BURST_BOMB = create("burst_bomb", BurstBombEntity::new, SpawnGroup.MISC, 0.5f, 0.5f);
	public static final RegistrySupplier<EntityType<SuctionBombEntity>> SUCTION_BOMB = create("suction_bomb", SuctionBombEntity::new, SpawnGroup.MISC, 0.5f, 0.5f);
	public static final RegistrySupplier<EntityType<SplatBombEntity>> SPLAT_BOMB = create("splat_bomb", SplatBombEntity::new, SpawnGroup.MISC, 0.5f, 0.5f);
	public static final RegistrySupplier<EntityType<CurlingBombEntity>> CURLING_BOMB = create("curling_bomb", CurlingBombEntity::new, SpawnGroup.MISC, 0.5f, 0.5f);
	private static <T extends Entity> RegistrySupplier<EntityType<T>> create(String name, EntityType.EntityFactory<T> supplier, SpawnGroup classification, float width, float height)
	{
		return REGISTRY.register(name, () -> EntityType.Builder.create(supplier, classification).dimensions(width, height).build(Splatcraft.identifierOf(name).toString()));
	}
	private static <T extends Entity> RegistrySupplier<EntityType<T>> create(String name, EntityType.EntityFactory<T> supplier, SpawnGroup classification)
	{
		return create(name, supplier, classification, 1, 1);
	}
	@Environment(EnvType.CLIENT)
	public static void bindRenderers()
	{
		EntityRendererRegistry.register(INK_DROP, InkDropRenderer::new);
		EntityRendererRegistry.register(INK_PROJECTILE, InkProjectileRenderer::new);
		EntityRendererRegistry.register(INK_SQUID, InkSquidRenderer::new);
		EntityRendererRegistry.register(SQUID_BUMPER, SquidBumperRenderer::new);
		
		EntityRendererRegistry.register(SPLAT_BOMB, SplatBombRenderer::new);
		EntityRendererRegistry.register(BURST_BOMB, BurstBombRenderer::new);
		EntityRendererRegistry.register(SUCTION_BOMB, SuctionBombRenderer::new);
		EntityRendererRegistry.register(CURLING_BOMB, CurlingBombRenderer::new);
		
		EntityRendererRegistry.register(SPAWN_SHIELD, SpawnShieldRenderer::new);
		
		EntityAttributeRegistry.register(SQUID_BUMPER, SquidBumperEntity::setCustomAttributes);
		EntityAttributeRegistry.register(INK_SQUID, InkSquidEntity::setCustomAttributes);
	}
	@Environment(EnvType.CLIENT)
	public static void defineModelLayers()
	{
		EntityModelLayerRegistry.register(InkSquidModel.LAYER_LOCATION, InkSquidModel::createBodyLayer);
		EntityModelLayerRegistry.register(SquidBumperModel.LAYER_LOCATION, SquidBumperModel::createBodyLayer);
		
		EntityModelLayerRegistry.register(SplatBombModel.LAYER_LOCATION, SplatBombModel::createBodyLayer);
		EntityModelLayerRegistry.register(BurstBombModel.LAYER_LOCATION, BurstBombModel::createBodyLayer);
		EntityModelLayerRegistry.register(SuctionBombModel.LAYER_LOCATION, SuctionBombModel::createBodyLayer);
		EntityModelLayerRegistry.register(CurlingBombModel.LAYER_LOCATION, CurlingBombModel::createBodyLayer);
		
		EntityModelLayerRegistry.register(InkProjectileModel.LAYER_LOCATION, InkProjectileModel::createBodyLayer);
		EntityModelLayerRegistry.register(ShooterInkProjectileModel.LAYER_LOCATION, ShooterInkProjectileModel::createBodyLayer);
		EntityModelLayerRegistry.register(BlasterInkProjectileModel.LAYER_LOCATION, BlasterInkProjectileModel::createBodyLayer);
		EntityModelLayerRegistry.register(RollerInkProjectileModel.LAYER_LOCATION, RollerInkProjectileModel::createBodyLayer);
		
		EntityModelLayerRegistry.register(InkTankModel.LAYER_LOCATION, InkTankModel::createBodyLayer);
		EntityModelLayerRegistry.register(ClassicInkTankModel.LAYER_LOCATION, ClassicInkTankModel::createBodyLayer);
		EntityModelLayerRegistry.register(InkTankJrModel.LAYER_LOCATION, InkTankJrModel::createBodyLayer);
		EntityModelLayerRegistry.register(ArmoredInkTankModel.LAYER_LOCATION, ArmoredInkTankModel::createBodyLayer);
	}
	public static void registerDataTrackers()
	{
		registerDataTracker("ink_color_handler", CommonUtils.INKCOLORDATAHANDLER);
		registerDataTracker("vec3d_handler", CommonUtils.VEC3DDATAHANDLER);
		registerDataTracker("vector2_handler", CommonUtils.VEC2DATAHANDLER);
		registerDataTracker("extra_data_handler", ExtraSaveData.SERIALIZER);
	}
	@ExpectPlatform
	private static void registerDataTracker(String name, TrackedDataHandler<?> handler)
	{
		throw new AssertionError();
	}
	public static DefaultAttributeContainer.Builder injectPlayerAttributes(DefaultAttributeContainer.Builder builder)
	{
		builder.add(SplatcraftAttributes.inkSwimSpeed, SplatcraftAttributes.inkSwimSpeed.get().getDefaultValue());
		builder.add(SplatcraftAttributes.superJumpTravelTime, SplatcraftAttributes.superJumpTravelTime.get().getDefaultValue());
		builder.add(SplatcraftAttributes.superJumpWindupTime, SplatcraftAttributes.superJumpWindupTime.get().getDefaultValue());
		builder.add(SplatcraftAttributes.superJumpHeight, SplatcraftAttributes.superJumpHeight.get().getDefaultValue());
		return builder;
	}
	@Environment(EnvType.CLIENT)
	private static <T extends LivingEntity, M extends EntityModel<T>> void attachInkOverlay(LivingEntityRenderer<T, M> renderer)
	{
		renderer.addFeature(new InkOverlayLayer<>(renderer));
	}
	@Environment(EnvType.CLIENT)
	public static void addRenderLayers(Map<EntityType<?>, EntityRenderer<?>> renderers, Map<SkinTextures.Model, EntityRenderer<? extends PlayerEntity>> skinMap, EntityRendererFactory.Context context)
	{
		skinMap.keySet().forEach(renderer ->
		{
			LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> skin = (LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>) skinMap.get(renderer);
			skin.addFeature(new InkAccessoryLayer(skin, new BipedEntityModel<>(context.getModelLoader().getModelPart(EntityModelLayers.PLAYER_OUTER_ARMOR))));
			skin.addFeature(new PlayerInkColoredSkinLayer(skin, new PlayerEntityModel<>(context.getModelLoader().getModelPart(renderer.equals(SkinTextures.Model.SLIM) ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers
				.PLAYER), renderer.equals(SkinTextures.Model.SLIM))));
			attachInkOverlay(Objects.requireNonNull(skin));
		});
		renderers
			.values().stream()
			.filter(LivingEntityRenderer.class::isInstance)
			.map(LivingEntityRenderer.class::cast)
			.forEach(SplatcraftEntities::attachInkOverlay);
	}
}
