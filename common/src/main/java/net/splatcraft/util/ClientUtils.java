package net.splatcraft.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.playerinfo.EntityInfo;
import net.splatcraft.items.InkTankItem;
import net.splatcraft.items.weapons.DualieItem;
import net.splatcraft.network.SplatcraftPacketHandler;
import net.splatcraft.network.c2s.PlayerSetSquidC2SPacket;
import net.splatcraft.registries.SplatcraftGameRules;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.UUID;

public class ClientUtils
{
    @Environment(EnvType.CLIENT)
    public static final HashMap<String, Stage> clientStages = new HashMap<>();
    @Environment(EnvType.CLIENT)
    protected static final TreeMap<UUID, InkColor> clientColors = new TreeMap<>();

    @Environment(EnvType.CLIENT)
    public static void resetClientColors()
    {
        clientColors.clear();
    }

    @Environment(EnvType.CLIENT)
    public static InkColor getClientPlayerColor(UUID player)
    {
        return clientColors.getOrDefault(player, InkColor.INVALID);
    }

    @Environment(EnvType.CLIENT)
    public static void setClientPlayerColor(UUID player, InkColor color)
    {
        clientColors.put(player, color);
    }

    @Environment(EnvType.CLIENT)
    public static void putClientColors(TreeMap<UUID, InkColor> map)
    {
        clientColors.putAll(map);
    }

    public static ClientPlayerEntity getClientPlayer()
    {
        return MinecraftClient.getInstance().player;
    }

    public static boolean showDurabilityBar(ItemStack stack)
    {
        return (SplatcraftConfig.get("splatcraft.inkIndicator").equals(SplatcraftConfig.InkIndicator.BOTH) || SplatcraftConfig.get("splatcraft.inkIndicator").equals(SplatcraftConfig.InkIndicator.DURABILITY)) &&
            getClientPlayer().getStackInHand(Hand.MAIN_HAND).equals(stack) && getDurabilityForDisplay() > 0;
    }

    public static double getDurabilityForDisplay()
    {
        PlayerEntity player = getClientPlayer();

        if (!SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.getBlockPos(), SplatcraftGameRules.REQUIRE_INK_TANK))
        {
            return 0;
        }

        ItemStack chestpiece = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chestpiece.getItem() instanceof InkTankItem item)
        {
            return InkTankItem.getInkAmount(chestpiece) / item.capacity;
        }
        return 1;
    }

    public static boolean canPerformRoll(ClientPlayerEntity player)
    {
        Input input = getUnmodifiedInput(player);
        return (!PlayerCooldown.hasPlayerCooldown(player) || (PlayerCooldown.getPlayerCooldown(player) instanceof DualieItem.DodgeRollCooldown dodgeRoll && dodgeRoll.canCancelRoll())) && input.jumping && (input.movementSideways != 0 || input.movementForward != 0);
    }

    public static Vec2f getDodgeRollVector(ClientPlayerEntity player, float rollSpeed)
    {
        Input input = getUnmodifiedInput(player);
        Vec2f direction = new Vec2f(input.movementSideways, input.movementForward);
        float p_20018_ = player.getYaw(); // Entity::getInputVector
        Vec2f vec3 = direction.normalize().multiply(rollSpeed);
        float f = MathHelper.sin(p_20018_ * (0.017453292f));
        float f1 = MathHelper.cos(p_20018_ * (0.017453292f));
        return new Vec2f(vec3.x * f1 - vec3.y * f, vec3.y * f1 + vec3.x * f);
    }

    public static boolean shouldRenderSide(BlockEntity te, Direction direction)
    {
        if (te.getWorld() == null)
            return false;

        BlockPos tePos = te.getPos();

        Vector3f lookVec = MinecraftClient.getInstance().gameRenderer.getCamera().getHorizontalPlane();
        Vec3d blockVec = Vec3d.ofBottomCenter(tePos).add(lookVec.x(), lookVec.y(), lookVec.z());

        Vec3d directionVec3d = blockVec.subtract(MinecraftClient.getInstance().gameRenderer.getCamera().getPos()).normalize();
        Vector3f directionVec = new Vector3f((float) directionVec3d.x, (float) directionVec3d.y, (float) directionVec3d.z);
        if (lookVec.dot(directionVec) > 0)
        {
            if (direction == null) return true;
            BlockState offset = te.getWorld().getBlockState(tePos.offset(direction));
            return offset.equals(Blocks.BARRIER.getDefaultState()) || !offset.isSolid() || !offset.isSolidBlock(te.getWorld(), tePos.offset(direction));
        }

        return false;
    }

    public static void setSquid(EntityInfo cap, boolean newSquid)
    {
        setSquid(cap, newSquid, false);
    }

    public static void setSquid(EntityInfo cap, boolean newSquid, boolean sendSquidCancel)
    {
        if (cap.isSquid() == newSquid)
        {
            return;
        }
        cap.setIsSquid(newSquid);
        cap.flagSquidCancel();
        SplatcraftPacketHandler.sendToServer(new PlayerSetSquidC2SPacket(newSquid, sendSquidCancel));
    }

    public static Input getUnmodifiedInput(ClientPlayerEntity player)
    {
        return PlayerMovementHandler.unmodifiedInput.getOrDefault(player, new Input());
    }
}