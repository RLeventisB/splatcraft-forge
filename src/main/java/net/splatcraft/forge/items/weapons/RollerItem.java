package net.splatcraft.forge.items.weapons;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.splatcraft.forge.blocks.ColoredBarrierBlock;
import net.splatcraft.forge.client.audio.RollerRollTickableSound;
import net.splatcraft.forge.client.particles.InkSplashParticleData;
import net.splatcraft.forge.entities.InkProjectileEntity;
import net.splatcraft.forge.entities.SquidBumperEntity;
import net.splatcraft.forge.handlers.PlayerPosingHandler;
import net.splatcraft.forge.items.weapons.settings.RollerWeaponSettings;
import net.splatcraft.forge.mixin.accessors.EntityAccessor;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.registries.SplatcraftSounds;
import net.splatcraft.forge.util.*;

import java.util.ArrayList;

public class RollerItem extends WeaponBaseItem<RollerWeaponSettings>
{
    public static final ArrayList<RollerItem> rollers = Lists.newArrayList();
    public boolean isMoving;

    public static RegistryObject<RollerItem> create(DeferredRegister<Item> registry, String settings, String name)
    {
        return registry.register(name, () -> new RollerItem(settings));
    }

    public static RegistryObject<RollerItem> create(DeferredRegister<Item> registry, RegistryObject<RollerItem> parent, String name)
    {
        return registry.register(name, () -> new RollerItem(parent.get().settingsId.toString()));
    }

    protected RollerItem(String settings)
    {
        super(settings);
        rollers.add(this);
    }

    @Override
    public Class<RollerWeaponSettings> getSettingsClass()
    {
        return RollerWeaponSettings.class;
    }

    public static void applyRecoilKnockback(LivingEntity entity, double pow)
    {
        entity.setDeltaMovement(new Vec3(Math.cos(Math.toRadians(entity.getYRot() + 90)) * -pow, entity.getDeltaMovement().y(), Math.sin(Math.toRadians(entity.getYRot() + 90)) * -pow));
        entity.hurtMarked = true;
    }

    @OnlyIn(Dist.CLIENT)
    protected static void playRollSound(boolean isBrush)
    {
        Minecraft.getInstance().getSoundManager().queueTickingSound(new RollerRollTickableSound(Minecraft.getInstance().player, isBrush));
    }

    public ClampedItemPropertyFunction getUnfolded()
    {
        return (stack, level, entity, seed) ->
        {
            if (entity instanceof Player player && PlayerCooldown.hasOverloadedPlayerCooldown(player))
            {

                PlayerCooldown cooldown = PlayerCooldown.getPlayerCooldown(player);
                if (cooldown.getSlotIndex() > -1 && cooldown.getTime() > (cooldown.isGrounded() ? -10 : 0))
                {
                    ItemStack cooldownStack = cooldown.getHand() == (InteractionHand.MAIN_HAND) ? (player).getInventory().items.get(cooldown.getSlotIndex())
                        : entity.getOffhandItem();
                    return stack.equals(cooldownStack) && (getSettings(stack).isBrush || cooldown.isGrounded()) ? 1 : 0;
                }
            }
            return entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1 : 0;
        };
    }

    @Override
    public void weaponUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft)
    {
        if (!(entity instanceof Player player))
            return;
        ItemCooldowns cooldownTracker = player.getCooldowns();
        if (cooldownTracker.isOnCooldown(this))
            return;

        RollerWeaponSettings settings = getSettings(stack);
        RollerWeaponSettings.RollerAttackDataRecord attackData = entity.onGround() ? settings.swingData.attackData() : settings.flingData.attackData();
        int startupTicks = attackData.startupTime();
        int rollTime = getUseDuration(stack) - timeLeft;
        if (rollTime < startupTicks)
        {
            //if (getInkAmount(entity, stack) > inkConsumption){
            PlayerCooldown cooldown = new PlayerCooldown(stack, startupTicks, player.getInventory().selected, entity.getUsedItemHand(), true, false, true, player.onGround());
            PlayerCooldown.setPlayerCooldown(player, cooldown);
            cooldownTracker.addCooldown(this, startupTicks + 6);
            //} else
            if (settings.isBrush && reduceInk(entity, this, attackData.inkConsumption(), attackData.inkRecoveryCooldown(), timeLeft % 4 == 0))
            {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SplatcraftSounds.brushFling, SoundSource.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
                int total = settings.rollData.inkSize() * 2 + 1;
                for (int i = 0; i < total; i++)
                {
                    InkProjectileEntity proj = new InkProjectileEntity(level, entity, stack, InkBlockUtils.getInkType(entity), 1.6f, settings);
                    proj.setProjectileType(InkProjectileEntity.Types.ROLLER);
                    proj.dropImpactSize = proj.getProjectileSize() * 0.5f;
                    proj.shootFromRotation(entity, entity.getXRot(), entity.getYRot() + (i - total / 2f) * 20, 0, settings.swingData.attackData().maxSpeed(), 0.05f);
                    proj.moveTo(proj.getX(), proj.getY() - entity.getEyeHeight() / 2f, proj.getZ());
                    level.addFreshEntity(proj);
                }
            }
            return;
        }

        float toConsume = Mth.lerp(Math.min(1, rollTime / settings.rollData.dashTime()), settings.rollData.inkConsumption(), settings.rollData.dashConsumption());
        isMoving = Math.abs(entity.yHeadRotO - entity.yHeadRot) > 0 || (player.walkDist != player.walkDistO);

        double dxOff = 0;
        double dzOff = 0;
        for (int i = 1; i <= 2; i++)
        {
            dxOff = Math.cos(Math.toRadians(entity.getYRot() + 90)) * i;
            dzOff = Math.sin(Math.toRadians(entity.getYRot() + 90)) * i;

            BlockPos pos = CommonUtils.createBlockPos(entity.getX() + dxOff, entity.getY(), entity.getZ() + dzOff);
            if (!InkBlockUtils.canInkPassthrough(level, pos))
                break;
        }

        boolean doPush = false;
        if (isMoving)
        {
            BlockInkedResult result = BlockInkedResult.FAIL;
            for (int i = 0; i < settings.rollData.inkSize(); i++)
            {
                double off = (double) i - (settings.rollData.inkSize() - 1) / 2d;
                double xOff = Math.cos(Math.toRadians(entity.getYRot())) * off;
                double zOff = Math.sin(Math.toRadians(entity.getYRot())) * off;

                for (float yOff = 0; yOff >= -3; yOff--)
                {
                    if (!enoughInk(entity, this, toConsume, 0, timeLeft % 4 == 0))
                    {
                        break;
                    }

                    if (yOff == -3)
                    {
                        dxOff = Math.cos(Math.toRadians(entity.getYRot() + 90));
                        dzOff = Math.sin(Math.toRadians(entity.getYRot() + 90));
                    }

                    BlockPos pos = CommonUtils.createBlockPos(entity.getX() + xOff + dxOff, entity.getY() + yOff, entity.getZ() + zOff + dzOff);

                    if (level.getBlockState(pos).getBlock() instanceof ColoredBarrierBlock && ((ColoredBarrierBlock) level.getBlockState(pos).getBlock()).canAllowThrough(pos, entity))
                        continue;

                    if (!InkBlockUtils.canInkPassthrough(level, pos))
                    {
                        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);

                        result = InkBlockUtils.playerInkBlock(player, level, pos, ColorUtils.getInkColor(stack), Direction.UP, InkBlockUtils.getInkType(player), settings.rollData.damage());
                        double blockHeight = shape.isEmpty() ? 0 : shape.bounds().maxY;

                        if (yOff != -3 && !(shape.bounds().minX <= 0 && shape.bounds().minZ <= 0 && shape.bounds().maxX >= 1 && shape.bounds().maxZ >= 1))
                        {
                            BlockInkedResult secondResult = InkBlockUtils.playerInkBlock(player, level, pos.below(), ColorUtils.getInkColor(stack), Direction.UP, InkBlockUtils.getInkType(entity), settings.rollData.damage());
                            if (result == BlockInkedResult.FAIL)
                            {
                                result = secondResult;
                            }
                        }

                        if (result != BlockInkedResult.FAIL && i < settings.rollData.hitboxSize())
                        {
                            level.addParticle(new InkSplashParticleData(ColorUtils.getInkColor(stack), 1), entity.getX() + xOff + dxOff, pos.getY() + blockHeight + 0.1, entity.getZ() + zOff + dzOff, 0, 0, 0);
                            if (i > 0)
                            {
                                double xhOff = dxOff + Math.cos(Math.toRadians(entity.getYRot())) * (off - 0.5);
                                double zhOff = dzOff + Math.sin(Math.toRadians(entity.getYRot())) * (off - 0.5);
                                level.addParticle(new InkSplashParticleData(ColorUtils.getInkColor(stack), 1), entity.getX() + xhOff, pos.getY() + blockHeight + 0.1, entity.getZ() + zhOff, 0, 0, 0);
                            }
                        }
                        break;
                    }
                }

                if (level.isClientSide())
                {
                    // Damage and knockback are dealt server-side
                    continue;
                }

                if (i >= settings.rollData.hitboxSize())
                {
                    continue;
                }

                BlockPos attackPos = CommonUtils.createBlockPos(entity.getX() + xOff + dxOff, entity.getY() - 1, entity.getZ() + zOff + dzOff);
                for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(attackPos, attackPos.offset(1, 2, 1)), EntitySelector.NO_SPECTATORS.and(e ->
                {
                    if (e instanceof LivingEntity)
                    {
                        if (InkDamageUtils.isSplatted((LivingEntity) e)) return false;
                        return InkDamageUtils.canDamage(e, entity) || e instanceof SquidBumperEntity;
                    }
                    return false;
                })))
                {
                    if (!target.equals(entity) && (!enoughInk(entity, this, toConsume, 0, false) || !InkDamageUtils.doRollDamage(target, settings.rollData.damage(), entity, entity, stack) || !InkDamageUtils.isSplatted(target)))
                    {
                        doPush = true;
                    }
                }
            }
            if (result != BlockInkedResult.FAIL)
                reduceInk(entity, this, toConsume, settings.rollData.inkRecoveryCooldown(), false);
        }
        if (doPush)
            applyRecoilKnockback(entity, 0.8);
    }

    @Override
    public void onPlayerCooldownEnd(Level level, Player player, ItemStack stack, PlayerCooldown cooldown)
    {
        boolean grounded = cooldown.isGrounded();
        RollerWeaponSettings settings = getSettings(stack);

        if (level.isClientSide())
            playRollSound(settings.isBrush);

        RollerWeaponSettings.RollerAttackDataRecord attackData = grounded ? settings.swingData.attackData() : settings.flingData.attackData();
        if (!settings.isBrush && reduceInk(player, this, attackData.inkConsumption(), attackData.inkRecoveryCooldown(), true))
        {
            AttackId attackId = AttackId.registerAttack();
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SplatcraftSounds.rollerFling, SoundSource.PLAYERS, 0.8F, ((level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.1F + 1.0F) * 0.95F);
            if (grounded)
            {
                // TODO: use https://sighack.com/post/poisson-disk-sampling-bridsons-algorithm for better distribution

               /* for (float i = 0; i < settings.swingProjectileCount; i++)
                {
                    float progress = 1 - (float) Math.pow(1 - i / (float) (settings.swingProjectileCount - 1), 2);
                    InkProjectileEntity proj = new InkProjectileEntity(level, player, stack, InkBlockUtils.getInkType(player), 1.6f, settings);
                    proj.throwerAirborne = false;
                    float extraAngle = settings.swingAttackAngle * (float) (proj.getRandom().nextGaussian());
                    proj.shootFromRotation(player, player.getXRot(), player.getYRot() + extraAngle * side, 0, settings.swingProjectileSpeed * progress, 0.05f);
                    proj.moveTo(proj.getX(), proj.getY() + 0.5, proj.getZ());

                    if (extraAngle > settings.swingLetalAngle)
                    {
                        proj.damageMultiplier = settings.swingOffAnglePenalty;
                    }
                    proj.setRollerSwingStats(settings);
                    level.addFreshEntity(proj);

                    side = -side;
                }*/
            }
            else
            {
                int count = settings.flingData.calculateProjectileCount();
                attackId.countProjectile(count);
                for (int i = 0; i < count; i++)
                {
                    InkProjectileEntity proj = new InkProjectileEntity(level, player, stack, InkBlockUtils.getInkType(player), 1.6f, settings);
                    proj.throwerAirborne = true;

                    float progress = (float) i / (count - 1f);
                    proj.shootFromRotation(
                        player,
                        player.getXRot() - Mth.lerp(progress, settings.flingData.startPitchCompensation(), settings.flingData.endPitchCompensation()),
                        player.getYRot(), 0,
                        Mth.lerp(progress, attackData.minSpeed(), attackData.maxSpeed()),
                        0.05f);

                    proj.moveTo(proj.position().add(EntityAccessor.invokeGetInputVector(new Vec3(0, 1, 0), 1.4f, proj.getYRot())));
                    proj.setRollerSwingStats(settings);
                    proj.setAttackId(attackId);
                    level.addFreshEntity(proj);
                }
            }
        }
    }

    @Override
    public boolean hasSpeedModifier(LivingEntity entity, ItemStack stack)
    {
        if (entity instanceof Player && PlayerCooldown.hasPlayerCooldown(entity) || !entity.getUseItem().equals(stack))
            return false;
        return super.hasSpeedModifier(entity, stack);
    }

    @Override
    public AttributeModifier getSpeedModifier(LivingEntity entity, ItemStack stack)
    {
        RollerWeaponSettings settings = getSettings(stack);
        double appliedMobility;
        int useTime = entity.getUseItemRemainingTicks() - entity.getUseItemRemainingTicks();
        float dashProgress = Math.min(1, useTime / settings.rollData.dashTime());

        if (enoughInk(entity, this, Math.min(settings.rollData.dashConsumption(), settings.rollData.inkConsumption()), 0, false))
        {
            if (entity instanceof Player && PlayerCooldown.hasPlayerCooldown(entity))
                appliedMobility = settings.swingData.mobility();
            else
            {
                appliedMobility = dashProgress * (settings.rollData.dashMobility() - settings.rollData.mobility()) + settings.rollData.mobility();
            }
        }
        else
        {
            appliedMobility = 0.7;
        }

        return new AttributeModifier(SplatcraftItems.SPEED_MOD_UUID, "Roller Mobility", appliedMobility - 1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public PlayerPosingHandler.WeaponPose getPose(Player player, ItemStack stack)
    {
        return getSettings(stack).isBrush ? PlayerPosingHandler.WeaponPose.BRUSH : PlayerPosingHandler.WeaponPose.ROLL;
    }
}