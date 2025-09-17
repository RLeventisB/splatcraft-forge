package net.splatcraft.registries;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
import net.splatcraft.client.models.subs.*;
import net.splatcraft.client.renderer.*;
import net.splatcraft.client.renderer.subs.*;
import net.splatcraft.entities.*;
import net.splatcraft.entities.subs.*;
import net.splatcraft.platform.DeferredRegister;
import net.splatcraft.platform.RegistrySupplier;
import net.splatcraft.platform.Services;
import net.splatcraft.util.CommonUtils;

import java.util.Map;
import java.util.Objects;

public class SplatcraftEntities
{
	public static final DeferredRegister<EntityType<?>> REGISTRY = Splatcraft.deferredRegistryOf(BuiltInRegistries.ENTITY_TYPE);
	public static final RegistrySupplier<EntityType<InkSquidEntity>> INK_SQUID = create("ink_squid", InkSquidEntity::new, MobCategory.AMBIENT, 0.6f, 0.5f);
	public static final RegistrySupplier<EntityType<InkDropEntity>> INK_DROP = create("ink_drop", InkDropEntity::new, MobCategory.MISC, InkDropEntity.DROP_SIZE, InkDropEntity.DROP_SIZE);
	public static final RegistrySupplier<EntityType<InkProjectileEntity>> INK_PROJECTILE = create("ink_projectile", InkProjectileEntity::new, MobCategory.MISC);
	public static final RegistrySupplier<EntityType<SquidBumperEntity>> SQUID_BUMPER = create("squid_bumper", SquidBumperEntity::new, MobCategory.MISC, 0.6f, 1.8f);
	public static final RegistrySupplier<EntityType<SpawnShieldEntity>> SPAWN_SHIELD = create("spawn_shield", SpawnShieldEntity::new, MobCategory.MISC, 1, 1);
	//Sub Weapons
	public static final RegistrySupplier<EntityType<BurstBombEntity>> BURST_BOMB = create("burst_bomb", BurstBombEntity::new, MobCategory.MISC, 0.5f, 0.5f);
	public static final RegistrySupplier<EntityType<SuctionBombEntity>> SUCTION_BOMB = create("suction_bomb", SuctionBombEntity::new, MobCategory.MISC, 0.3f, 0.3f);
	public static final RegistrySupplier<EntityType<SplatBombEntity>> SPLAT_BOMB = create("splat_bomb", SplatBombEntity::new, MobCategory.MISC, 0.5f, 0.5f);
	public static final RegistrySupplier<EntityType<CurlingBombEntity>> CURLING_BOMB = create("curling_bomb", CurlingBombEntity::new, MobCategory.MISC, 0.5f, 0.5f);
	public static final RegistrySupplier<EntityType<TorpedoEntity>> TORPEDO = create("torpedo", TorpedoEntity::new, MobCategory.MISC, 0.7f, 0.7f);
	// Special Weapons
	public static final RegistrySupplier<EntityType<StingRayBeamEntity>> STING_RAY_PROJECTILE = create("sting_ray_beam", StingRayBeamEntity::new, MobCategory.MISC, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
	private static <T extends Entity> RegistrySupplier<EntityType<T>> create(String name, EntityType.EntityFactory<T> supplier, MobCategory classification, float width, float height)
	{
		return REGISTRY.register(name, () -> EntityType.Builder.of(supplier, classification).sized(width, height).build(Splatcraft.identifierOf(name).toString()));
	}
	private static <T extends Entity> RegistrySupplier<EntityType<T>> create(String name, EntityType.EntityFactory<T> supplier, MobCategory classification)
	{
		return create(name, supplier, classification, 1, 1);
	}
	@OnlyIn(Dist.CLIENT)
	public static void bindRenderers()
	{
		Services.PLATFORM.registerEntityRenderer(INK_DROP, InkDropRenderer::new);
		Services.PLATFORM.registerEntityRenderer(INK_PROJECTILE, InkProjectileRenderer::new);
		Services.PLATFORM.registerEntityRenderer(INK_SQUID, InkSquidRenderer::new);
		Services.PLATFORM.registerEntityRenderer(SQUID_BUMPER, SquidBumperRenderer::new);

		Services.PLATFORM.registerEntityRenderer(SPLAT_BOMB, SplatBombRenderer::new);
		Services.PLATFORM.registerEntityRenderer(BURST_BOMB, BurstBombRenderer::new);
		Services.PLATFORM.registerEntityRenderer(SUCTION_BOMB, SuctionBombRenderer::new);
		Services.PLATFORM.registerEntityRenderer(CURLING_BOMB, CurlingBombRenderer::new);
		Services.PLATFORM.registerEntityRenderer(TORPEDO, TorpedoRenderer::new);

		Services.PLATFORM.registerEntityRenderer(SPAWN_SHIELD, SpawnShieldRenderer::new);

		Services.PLATFORM.registerEntityRenderer(STING_RAY_PROJECTILE, StingRayBeamRenderer::new);
	}
	@OnlyIn(Dist.CLIENT)
	public static void defineModelLayers()
	{
		Services.PLATFORM.registerEntityLayerRenderer(InkSquidModel.LAYER_LOCATION, InkSquidModel::createBodyLayer);
		Services.PLATFORM.registerEntityLayerRenderer(SquidBumperModel.LAYER_LOCATION, SquidBumperModel::createBodyLayer);

		Services.PLATFORM.registerEntityLayerRenderer(SplatBombModel.LAYER_LOCATION, SplatBombModel::createBodyLayer);
		Services.PLATFORM.registerEntityLayerRenderer(BurstBombModel.LAYER_LOCATION, BurstBombModel::createBodyLayer);
		Services.PLATFORM.registerEntityLayerRenderer(SuctionBombModel.LAYER_LOCATION, SuctionBombModel::createBodyLayer);
		Services.PLATFORM.registerEntityLayerRenderer(CurlingBombModel.LAYER_LOCATION, CurlingBombModel::createBodyLayer);
		Services.PLATFORM.registerEntityLayerRenderer(TorpedoModel.LAYER_LOCATION, TorpedoModel::createBodyLayer);

		Services.PLATFORM.registerEntityLayerRenderer(InkProjectileModel.LAYER_LOCATION, InkProjectileModel::createBodyLayer);
		Services.PLATFORM.registerEntityLayerRenderer(ShooterInkProjectileModel.LAYER_LOCATION, ShooterInkProjectileModel::createBodyLayer);
		Services.PLATFORM.registerEntityLayerRenderer(BlasterInkProjectileModel.LAYER_LOCATION, BlasterInkProjectileModel::createBodyLayer);
		Services.PLATFORM.registerEntityLayerRenderer(RollerInkProjectileModel.LAYER_LOCATION, RollerInkProjectileModel::createBodyLayer);

		Services.PLATFORM.registerEntityLayerRenderer(InkTankModel.LAYER_LOCATION, InkTankModel::createBodyLayer);
		Services.PLATFORM.registerEntityLayerRenderer(ClassicInkTankModel.LAYER_LOCATION, ClassicInkTankModel::createBodyLayer);
		Services.PLATFORM.registerEntityLayerRenderer(InkTankJrModel.LAYER_LOCATION, InkTankJrModel::createBodyLayer);
		Services.PLATFORM.registerEntityLayerRenderer(ArmoredInkTankModel.LAYER_LOCATION, ArmoredInkTankModel::createBodyLayer);
	}
	public static void registerDataTrackers()
	{
		registerDataTracker("vec2_handler", CommonUtils.VEC2_DATA_HANDLER);
		registerDataTracker("vec3_handler", CommonUtils.VEC3_DATA_HANDLER);
		registerDataTracker("optional_vec3", CommonUtils.OPTIONAL_VEC3_DATA_HANDLER);
		registerDataTracker("optional_direction", CommonUtils.OPTIONAL_DIRECTION_DATA_HANDLER);
		registerDataTracker("ink_color_handler", CommonUtils.INKCOLOR_DATA_HANDLER);
		registerDataTracker("extra_data_handler", ExtraSaveData.SERIALIZER);
	}
	private static void registerDataTracker(String name, EntityDataSerializer<?> handler)
	{
		Services.PLATFORM.registerDataTracker(name, handler);
	}
	public static AttributeSupplier.Builder injectPlayerAttributes(AttributeSupplier.Builder builder)
	{
		builder.add(SplatcraftAttributes.inkSwimSpeed, SplatcraftAttributes.inkSwimSpeed.get().getDefaultValue());
		builder.add(SplatcraftAttributes.superJumpTravelTime, SplatcraftAttributes.superJumpTravelTime.get().getDefaultValue());
		builder.add(SplatcraftAttributes.superJumpWindupTime, SplatcraftAttributes.superJumpWindupTime.get().getDefaultValue());
		builder.add(SplatcraftAttributes.superJumpHeight, SplatcraftAttributes.superJumpHeight.get().getDefaultValue());
		builder.add(SplatcraftAttributes.enemyInkResistanceTime, SplatcraftAttributes.enemyInkResistanceTime.get().getDefaultValue());
		builder.add(SplatcraftAttributes.maxEnemyInkDamage, SplatcraftAttributes.maxEnemyInkDamage.get().getDefaultValue());
		builder.add(SplatcraftAttributes.enemyInkJumpMultiplier, SplatcraftAttributes.enemyInkJumpMultiplier.get().getDefaultValue());
		builder.add(SplatcraftAttributes.specialLoss, SplatcraftAttributes.specialLoss.get().getDefaultValue());
		return builder;
	}
	@OnlyIn(Dist.CLIENT)
	private static <T extends LivingEntity, M extends EntityModel<T>> void attachInkOverlay(LivingEntityRenderer<T, M> renderer)
	{
		renderer.addLayer(new InkOverlayLayer<>(renderer));
	}
	@OnlyIn(Dist.CLIENT)
	public static void addRenderLayers(Map<EntityType<?>, EntityRenderer<?>> renderers, Map<PlayerSkin.Model, EntityRenderer<? extends Player>> skinMap, EntityRendererProvider.Context context)
	{
		skinMap.keySet().forEach(renderer ->
		{
			LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> skin = (LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) skinMap.get(renderer);
			skin.addLayer(new InkAccessoryLayer(skin, new HumanoidModel<>(context.getModelSet().bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR))));
			skin.addLayer(new PlayerInkColoredSkinLayer(skin, new PlayerModel<>(context.getModelSet().bakeLayer(renderer.equals(PlayerSkin.Model.SLIM) ? ModelLayers.PLAYER_SLIM : ModelLayers
				.PLAYER), renderer.equals(PlayerSkin.Model.SLIM))));
			attachInkOverlay(Objects.requireNonNull(skin));
		});
		renderers
			.values().stream()
			.filter(LivingEntityRenderer.class::isInstance)
			.map(LivingEntityRenderer.class::cast)
			.forEach(SplatcraftEntities::attachInkOverlay);
	}
	public static void registerAttributes()
	{
		Services.PLATFORM.registerAttribute(SQUID_BUMPER, SquidBumperEntity::setCustomAttributes);
		Services.PLATFORM.registerAttribute(INK_SQUID, InkSquidEntity::setCustomAttributes);
	}
}
