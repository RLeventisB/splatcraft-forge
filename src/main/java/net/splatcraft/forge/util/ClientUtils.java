package net.splatcraft.forge.util;

import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.splatcraft.forge.SplatcraftConfig;
import net.splatcraft.forge.client.handlers.PlayerMovementHandler;
import net.splatcraft.forge.data.Stage;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfo;
import net.splatcraft.forge.items.InkTankItem;
import net.splatcraft.forge.items.weapons.DualieItem;
import net.splatcraft.forge.network.SplatcraftPacketHandler;
import net.splatcraft.forge.network.c2s.PlayerSetSquidC2SPacket;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class ClientUtils
{
    @OnlyIn(Dist.CLIENT)
    protected static final TreeMap<UUID, Integer> clientColors = new TreeMap<>();
    @OnlyIn(Dist.CLIENT)
    public static final HashMap<String, Stage> clientStages = new HashMap<>();

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

        if (!SplatcraftGameRules.getLocalizedRule(player.level(), player.blockPosition(), SplatcraftGameRules.REQUIRE_INK_TANK))
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
        float f = Mth.sin(p_20018_ * (0.017453292f));
        float f1 = Mth.cos(p_20018_ * (0.017453292f));
        return new Vec2(vec3.x * f1 - vec3.y * f, vec3.y * f1 + vec3.x * f);
    }

    public static boolean shouldRenderSide(BlockEntity te, Direction direction)
    {
        if (te.getLevel() == null)
            return false;

        BlockPos tePos = te.getBlockPos();

        Vector3f lookVec = Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector();
        Vec3 blockVec = Vec3.atBottomCenterOf(tePos).add(lookVec.x(), lookVec.y(), lookVec.z());

        Vec3 directionVec3d = blockVec.subtract(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()).normalize();
        Vector3f directionVec = new Vector3f((float) directionVec3d.x, (float) directionVec3d.y, (float) directionVec3d.z);
        if (lookVec.dot(directionVec) > 0)
        {
            if (direction == null) return true;
            BlockState relative = te.getLevel().getBlockState(tePos.relative(direction));
            return relative.getMaterial().equals(Material.BARRIER) || !relative.getMaterial().isSolidBlocking() || !relative.isCollisionShapeFullBlock(te.getLevel(), tePos.relative(direction));
        }

        return false;
    }

    public static void setSquid(PlayerInfo cap, boolean newSquid)
    {
        if (cap.isSquid() == newSquid)
        {
            return;
        }
        cap.setIsSquid(newSquid);
        SplatcraftPacketHandler.sendToServer(new PlayerSetSquidC2SPacket(newSquid));
    }

    public static Input getUnmodifiedInput(LocalPlayer player)
    {
        return PlayerMovementHandler.unmodifiedInput.getOrDefault(player, new Input());
    }

    public static Vec3 getInputVector(Vec3 pRelative, float pFacing)
    {
        double d0 = pRelative.lengthSqr();
        if (d0 < 1.0E-7)
        {
            return Vec3.ZERO;
        } else
        {
            Vec3 vec3 = (d0 > 1.0 ? pRelative.normalize() : pRelative).scale(0.1);
            float f = Mth.sin(pFacing * 0.017453292F);
            float f1 = Mth.cos(pFacing * 0.017453292F);
            return new Vec3(vec3.x * (double) f1 - vec3.z * (double) f, vec3.y, vec3.z * (double) f1 + vec3.x * (double) f);
        }
    }
}