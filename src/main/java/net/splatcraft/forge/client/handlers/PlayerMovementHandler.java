package net.splatcraft.forge.client.handlers;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.items.weapons.DualieItem;
import net.splatcraft.forge.items.weapons.RollerItem;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.c2s.SquidInputPacket;
import net.splatcraft.forge.registries.SplatcraftAttributes;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.util.InkBlockUtils;
import net.splatcraft.forge.util.PlayerCooldown;

import java.util.HashMap;
import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlayerMovementHandler
{
    public static final HashMap<LocalPlayer, Input> unmodifiedInput = new HashMap<>();
    private static final AttributeModifier INK_SWIM_SPEED = new AttributeModifier("Ink swimming speed boost", 0D, AttributeModifier.Operation.ADDITION);
    private static final AttributeModifier SQUID_SWIM_SPEED = new AttributeModifier("Squid swim speed boost", 0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier ENEMY_INK_SPEED = new AttributeModifier("Enemy ink speed penalty", -0.5D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier SLOW_FALLING = new AttributeModifier(UUID.fromString("A5B6CF2A-2F7C-31EF-9022-7C3E7D5E6ABA"), "Slow falling acceleration reduction", -0.07, AttributeModifier.Operation.ADDITION); // Add -0.07 to 0.08 so we get the vanilla default of 0.01

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void playerMovement(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player)
        {
        }
        else if (event.phase == TickEvent.Phase.START && event.player instanceof LocalPlayer player)
        {
            PlayerInfo playerInfo = PlayerInfoCapability.get(player);
            if (playerInfo == null)
                playerInfo = new PlayerInfo();

            boolean hasCooldown = PlayerCooldown.hasPlayerCooldown(player);
            PlayerCooldown cooldown = hasCooldown ? PlayerCooldown.getPlayerCooldown(player) : null;

            AttributeInstance speedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            AttributeInstance swimAttribute = player.getAttribute(ForgeMod.SWIM_SPEED.get());

            if (speedAttribute.hasModifier(INK_SWIM_SPEED))
                speedAttribute.removeModifier(INK_SWIM_SPEED);
            if (speedAttribute.hasModifier(ENEMY_INK_SPEED))
                speedAttribute.removeModifier(ENEMY_INK_SPEED);
            if (swimAttribute.hasModifier(SQUID_SWIM_SPEED))
                swimAttribute.removeModifier(SQUID_SWIM_SPEED);

            if (speedAttribute.getModifier(SplatcraftItems.SPEED_MOD_UUID) != null)
                speedAttribute.removeModifier(SplatcraftItems.SPEED_MOD_UUID);

            if (InkBlockUtils.onEnemyInk(player))
            {
                //player.setDeltaMovement(player.getDeltaMovement().x, Math.min(player.getDeltaMovement().y, 0.05f), player.getDeltaMovement().z);
                if (!speedAttribute.hasModifier(ENEMY_INK_SPEED))
                    speedAttribute.addTransientModifier(ENEMY_INK_SPEED);
            }

            ItemStack useStack = player.getUseItem();
            if (hasCooldown)
                useStack = cooldown.storedStack;
            else if (useStack.isEmpty())
                useStack = player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem()) ? player.getMainHandItem() :
                        player.getCooldowns().isOnCooldown(player.getOffhandItem().getItem()) ? player.getOffhandItem() : ItemStack.EMPTY;

            if (useStack.getItem() instanceof WeaponBaseItem<?> weapon && weapon.hasSpeedModifier(player, useStack))
            {
                AttributeModifier mod = weapon.getSpeedModifier(player, useStack);
                if (!speedAttribute.hasModifier(mod))
                    speedAttribute.addTransientModifier(mod);
            }

            if (playerInfo.isSquid())
            {
                if (InkBlockUtils.canSquidSwim(player) && !speedAttribute.hasModifier(INK_SWIM_SPEED) && player.onGround())
                    speedAttribute.addTransientModifier(INK_SWIM_SPEED);
                if (!swimAttribute.hasModifier(SQUID_SWIM_SPEED))
                    swimAttribute.addTransientModifier(SQUID_SWIM_SPEED);
            }

            if (hasCooldown && cooldown.getSlotIndex() >= 0)
                player.getInventory().selected = cooldown.getSlotIndex();

            if (!player.getAbilities().flying)
                if (speedAttribute.hasModifier(INK_SWIM_SPEED))
                    player.moveRelative((float) player.getAttributeValue(SplatcraftAttributes.inkSwimSpeed.get()) * (player.onGround() ? 1 : 0.75f), new Vec3(player.xxa, 0.0f, player.zza).normalize());
        }
    }

    @SubscribeEvent
    public static void onInputUpdate(net.minecraftforge.client.event.MovementInputUpdateEvent event)
    {
        Input input = event.getInput();
        LocalPlayer player = (LocalPlayer) event.getEntity();
        PlayerInfo playerInfo = PlayerInfoCapability.get(player);
        if (playerInfo == null)
            playerInfo = new PlayerInfo();

        Input clonedInput = new Input();
        clonedInput.leftImpulse = input.leftImpulse;
        clonedInput.forwardImpulse = input.forwardImpulse;
        clonedInput.up = input.up;
        clonedInput.down = input.down;
        clonedInput.left = input.left;
        clonedInput.right = input.right;
        clonedInput.jumping = input.jumping;
        clonedInput.shiftKeyDown = input.shiftKeyDown;
        unmodifiedInput.put(player, clonedInput);

        float speedMod = !input.shiftKeyDown ? playerInfo.isSquid() && InkBlockUtils.canSquidHide(player) ? 30f : 2f : 1f;

        input.forwardImpulse *= speedMod;
        //input = player.movementInput;
        input.leftImpulse *= speedMod;
        //input = player.movementInput;

        if (playerInfo.isSquid())
        {
            handleSquidMovement(playerInfo, player, input.leftImpulse, input.forwardImpulse, input.jumping, input.shiftKeyDown, input);
        }

        if (player.isUsingItem())
        {
            ItemStack stack = player.getUseItem();
            if (!stack.isEmpty())
            {
                if (stack.getItem() instanceof WeaponBaseItem)
                {
                    input.leftImpulse *= 5.0F;
                    input.forwardImpulse *= 5.0F;

                    if (stack.getItem() instanceof DualieItem && (input.leftImpulse != 0 || input.forwardImpulse != 0))
                    {
                        input.jumping = false;
                    }
                }
            }
        }

        if (PlayerCooldown.hasPlayerCooldown(player))
        {
            PlayerCooldown cooldown = PlayerCooldown.getPlayerCooldown(player);

            if (!cooldown.canMove())
            {
                input.forwardImpulse = 0;
                input.leftImpulse = 0;
                input.jumping = false;
            }
            else if (cooldown.storedStack.getItem() instanceof RollerItem rollerItem)
            {
                input.forwardImpulse = Math.min(1, Math.abs(input.forwardImpulse)) * Math.signum(input.forwardImpulse) * rollerItem.getSettings(cooldown.storedStack).swingData.mobility();
                input.leftImpulse = Math.min(1, Math.abs(input.leftImpulse)) * Math.signum(input.leftImpulse) * rollerItem.getSettings(cooldown.storedStack).swingData.mobility();
            }
            if (cooldown.forceCrouch() && cooldown.getTime() >= 1)
            {
                input.shiftKeyDown = !player.getAbilities().flying;
            }
        }
    }

    private static void handleSquidMovement(PlayerInfo playerInfo, Player player, float leftImpulse, float forwardImpulse, boolean jumping, boolean shiftKeyDown, Input input)
    {
        if (playerInfo.getClimbedDirection().isPresent())
        {
            Direction oldClimbedDirection = playerInfo.getClimbedDirection().get();
            Direction climbedDirection = InkBlockUtils.getSquidClimbingDirection(player, leftImpulse, forwardImpulse, oldClimbedDirection);

            if (climbedDirection != null && !player.onGround()) // if player is still swimming on a wall
            {
                playerInfo.setClimbedDirection(climbedDirection);
                Vec3 deltaMovement = player.getDeltaMovement();
                if (deltaMovement.y() < 0.4f && (forwardImpulse != 0 || leftImpulse != 0)) // handle input on wall
                {
                    float yRot = player.getYHeadRot();
                    Vec3 vec3 =
                            Entity.getInputVector(new Vec3(0f, forwardImpulse, 0f), 0.12f, yRot).add(
                                    Entity.getInputVector(new Vec3(leftImpulse, 0f, 0f), 0.02f, yRot)
                            );

                    deltaMovement = deltaMovement.add(vec3);
                }
                if (shiftKeyDown) // set minimum y velocity to 0 if shifting
                    deltaMovement = new Vec3(deltaMovement.x, Math.max(0, deltaMovement.y()), deltaMovement.z);

                if (climbedDirection.getAxis() != oldClimbedDirection.getAxis()) // if player swam to another wall, rotate velocity
                {
                    deltaMovement = deltaMovement.yRot(Mth.DEG_TO_RAD * (climbedDirection.toYRot() - oldClimbedDirection.toYRot()));
                }

                if (climbedDirection.getAxis() == Direction.Axis.X) // set velocity perpendicular to the wall to 0 because YOU CANNOT ESCAPE THE WALL (unless you press back).
                {
                    double parallelMovement = deltaMovement.x;
                    if (Math.abs(parallelMovement) < 0.6)
                        deltaMovement = new Vec3(0, deltaMovement.y, deltaMovement.z);
                }
                else
                {
                    double parallelMovement = deltaMovement.z;
                    if (Math.abs(parallelMovement) < 0.6)
                        deltaMovement = new Vec3(deltaMovement.x, deltaMovement.y, 0);
                }

                if (deltaMovement.y <= -0.3D) // limit gravity
                {
                    deltaMovement = new Vec3(deltaMovement.x, -0.3D, deltaMovement.z);
                }

                if (jumping) // squid surge
                {
                    deltaMovement = deltaMovement.scale(1f / (1f + playerInfo.getSquidSurgeCharge() / 2f));

                    if (playerInfo.getSquidSurgeCharge() < 30)
                        playerInfo.setSquidSurgeCharge(playerInfo.getSquidSurgeCharge() + 1);
                }
                else // stop squid surge
                {
                    if (playerInfo.getSquidSurgeCharge() >= 30) // do squid surge logic
                    {
                        deltaMovement = new Vec3(0, 10, 0);
                    }
                    playerInfo.setSquidSurgeCharge(0f);
                }

                if (input != null) // set input as 0 because movement was handled!! i think i should've used the event thingy though
                {
                    input.forwardImpulse = 0;
                    input.leftImpulse = 0;
                }

                player.fallDistance = 0.0F;
                player.setDeltaMovement(deltaMovement);
            }
            else
            {
                playerInfo.setClimbedDirection(null);
            }
        }

        if (playerInfo.getClimbedDirection().isEmpty())
        {
            playerInfo.setSquidSurgeCharge(0f);
            Direction newDirection = InkBlockUtils.canSquidClimb(player, leftImpulse, forwardImpulse, player.getYRot());
            if (newDirection != null)
            {
                player.teleportRelative(0, 0.01, 0);
                player.setOnGround(false);
                playerInfo.setClimbedDirection(newDirection);
            }
        }
        SplatcraftPacketHandler.sendToServer(new SquidInputPacket(
                playerInfo.getClimbedDirection(),
                playerInfo.getSquidSurgeCharge()));
    }
}
