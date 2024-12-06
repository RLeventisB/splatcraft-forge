package net.splatcraft.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.MathHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.splatcraft.SplatcraftConfig;
import net.splatcraft.client.handlers.PlayerMovementHandler;
import net.splatcraft.data.Stage;
import net.splatcraft.data.capabilities.playerinfo.PlayerInfo;
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
    @OnlyIn(Dist.CLIENT)
    public static final HashMap<String, Stage> clientStages = new HashMap<>();
    @OnlyIn(Dist.CLIENT)
    protected static final TreeMap<UUID, Integer> clientColors = new TreeMap<>();

    @OnlyIn(Dist.CLIENT)
    public static void resetClientColors()
    {
        clientColors.clear();
    }

    @OnlyIn(Dist.CLIENT)
    public static int getClientPlayerColor(UUID player)
    {
        return clientColors.getOrDefault(player, -1);
    }

    @OnlyIn(Dist.CLIENT)
    public static void setClientPlayerColor(UUID player, int color)
    {
        clientColors.put(player, color);
    }

    @OnlyIn(Dist.CLIENT)
    public static void putClientColors(TreeMap<UUID, Integer> map)
    {
        clientColors.putAll(map);
    }

    public static Player getClientPlayer()
    {
        return Minecraft.getInstance().player;
    }

    public static boolean showDurabilityBar(ItemStack stack)
    {
        return (SplatcraftConfig.Client.inkIndicator.get().equals(SplatcraftConfig.InkIndicator.BOTH) || SplatcraftConfig.Client.inkIndicator.get().equals(SplatcraftConfig.InkIndicator.DURABILITY)) &&
            getClientPlayer().getItemInHand(InteractionHand.MAIN_HAND).equals(stack) && getDurabilityForDisplay() > 0;
    }

    public static double getDurabilityForDisplay()
    {
        Player player = getClientPlayer();

        if (!SplatcraftGameRules.getLocalizedRule(player.getWorld(), player.blockPosition(), SplatcraftGameRules.REQUIRE_INK_TANK))
        {
            return 0;
        }

        ItemStack chestpiece = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestpiece.getItem() instanceof InkTankItem item)
        {
            return InkTankItem.getInkAmount(chestpiece) / item.capacity;
        }
        return 1;
    }

    public static boolean canPerformRoll(LocalPlayer player)
    {
        Input input = getUnmodifiedInput(player);
        return (!PlayerCooldown.hasPlayerCooldown(player) || (PlayerCooldown.getPlayerCooldown(player) instanceof DualieItem.DodgeRollCooldown dodgeRoll && dodgeRoll.canCancelRoll())) && input.jumping && (input.leftImpulse != 0 || input.forwardImpulse != 0);
    }

    public static Vec2 getDodgeRollVector(LocalPlayer player, float rollSpeed)
    {
        Input input = getUnmodifiedInput(player);
        Vec2 direction = new Vec2(input.leftImpulse, input.forwardImpulse);
        float p_20018_ = player.getYRot(); // Entity::getInputVector
        Vec2 vec3 = direction.normalized().scale(rollSpeed);
        float f = MathHelper.sin(p_20018_ * (0.017453292f));
        float f1 = MathHelper.cos(p_20018_ * (0.017453292f));
        return new Vec2(vec3.x * f1 - vec3.y * f, vec3.y * f1 + vec3.x * f);
    }

    public static boolean shouldRenderSide(BlockEntity te, Direction direction)
    {
        if (te.getLevel() == null)
            return false;

        BlockPos tePos = te.getBlockPos();

        Vector3f lookVec = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
        Vec3d blockVec = Vec3d.atBottomCenterOf(tePos).add(lookVec.x(), lookVec.y(), lookVec.z());

        Vec3d directionVec3d = blockVec.subtract(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()).normalize();
        Vector3f directionVec = new Vector3f((float) directionVec3d.x, (float) directionVec3d.y, (float) directionVec3d.z);
        if (lookVec.dot(directionVec) > 0)
        {
            if (direction == null) return true;
            BlockState relative = te.getLevel().getBlockState(tePos.relative(direction));
            return relative.equals(Blocks.BARRIER.defaultBlockState()) || !relative.isSolid() || !relative.isCollisionShapeFullBlock(te.getLevel(), tePos.relative(direction));
        }

        return false;
    }

    public static void setSquid(PlayerInfo cap, boolean newSquid)
    {
        setSquid(cap, newSquid, false);
    }

    public static void setSquid(PlayerInfo cap, boolean newSquid, boolean sendSquidCancel)
    {
        if (cap.isSquid() == newSquid)
        {
            return;
        }
        cap.setIsSquid(newSquid);
        cap.flagSquidCancel();
        SplatcraftPacketHandler.sendToServer(new PlayerSetSquidC2SPacket(newSquid, sendSquidCancel));
    }

    public static Input getUnmodifiedInput(LocalPlayer player)
    {
        return PlayerMovementHandler.unmodifiedInput.getOrDefault(player, new Input());
    }
}