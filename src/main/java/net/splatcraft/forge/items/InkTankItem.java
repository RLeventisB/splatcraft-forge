package net.splatcraft.forge.items;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.splatcraft.forge.SplatcraftConfig;
import net.splatcraft.forge.client.models.inktanks.AbstractInkTankModel;
import net.splatcraft.forge.data.SplatcraftTags;
import net.splatcraft.forge.data.capabilities.playerinfo.PlayerInfoCapability;
import net.splatcraft.forge.items.weapons.RollerItem;
import net.splatcraft.forge.items.weapons.WeaponBaseItem;
import net.splatcraft.forge.registries.SplatcraftGameRules;
import net.splatcraft.forge.registries.SplatcraftItemGroups;
import net.splatcraft.forge.registries.SplatcraftItems;
import net.splatcraft.forge.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class InkTankItem extends ColoredArmorItem
{
    public static final ArrayList<InkTankItem> inkTanks = new ArrayList<>();
    public final float capacity;
    public final Properties properties;
    @OnlyIn(Dist.CLIENT)
    private AbstractInkTankModel model;

    public InkTankItem(String tagId, float capacity, ArmorMaterial material, Properties properties)
    {
        super(material, Type.CHESTPLATE, properties);
        this.capacity = capacity;
        this.properties = properties;

        SplatcraftItems.weapons.add(this);
        SplatcraftItemGroups.addGeneralItem(this);
        inkTanks.add(this);
        SplatcraftTags.Items.putInkTankTags(this, tagId);
    }

    public InkTankItem(String tagId, float capacity, ArmorMaterial material)
    {
        this(tagId, capacity, material, new Properties().stacksTo(1));
    }

    public InkTankItem(String name, InkTankItem parent)
    {
        this(name, parent.capacity, new SplatcraftArmorMaterial(name, (SplatcraftArmorMaterial) parent.material), parent.properties);
    }

    public InkTankItem(String name, float capacity)
    {
        this(name, capacity, new SplatcraftArmorMaterial(name, SoundEvents.ARMOR_EQUIP_CHAIN, 0, 0, 0));
    }

    public static float getInkAmount(ItemStack stack)
    {
        if (!(stack.getItem() instanceof InkTankItem inkTankItem))
            return 0;
        float capacity = inkTankItem.capacity;
        if (stack.getOrCreateTag().getBoolean("InfiniteInk")) return capacity;
        return Math.max(0, Math.min(capacity, stack.getOrCreateTag().getFloat("Ink")));
    }

    public static void setInkAmount(ItemStack stack, float value)
    {
        stack.getOrCreateTag().putFloat("Ink", Math.max(0, Math.min(((InkTankItem) stack.getItem()).capacity, value)));
    }

    public static boolean canRecharge(ItemStack stack, boolean fromTick)
    {
        CompoundTag tag = stack.getOrCreateTag();
        boolean cannotRecharge = tag.contains("CannotRecharge");
        if (!tag.contains("RecoveryCooldown"))
            tag.putInt("RecoveryCooldown", 0);
        int cooldown = tag.getInt("RecoveryCooldown");
        if (cooldown == 0 || !fromTick) return !cannotRecharge;
        tag.putInt("RecoveryCooldown", --cooldown);
        return false;
    }

    public static void setRecoveryCooldown(ItemStack stack, int recoveryCooldown)
    {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("RecoveryCooldown", Math.max(tag.getInt("RecoveryCooldown"), recoveryCooldown));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, level, entity, itemSlot, isSelected);

        if (entity instanceof Player player && !level.isClientSide && SplatcraftGameRules.getLocalizedRule(level, entity.blockPosition(), SplatcraftGameRules.RECHARGEABLE_INK_TANK))
        {
            float ink = getInkAmount(stack);
            Item using = player.getUseItem().getItem();

            if (canRecharge(stack, true) && player.getItemBySlot(EquipmentSlot.CHEST).equals(stack) && ColorUtils.colorEquals(player, stack) && ink < capacity
                    && (!PlayerCooldown.hasPlayerCooldown(player) || PlayerCooldown.getPlayerCooldown(player).interruptsRecharge())
                    && !PlayerCharge.hasCharge(player)
                    && (!(using instanceof WeaponBaseItem)
                    || (using instanceof RollerItem r && !r.isMoving))
            )
            {
                setInkAmount(stack, ink + (100f / 20f / ((InkBlockUtils.canSquidHide(player) && PlayerInfoCapability.isSquid(player)) ? 3f : 10f)));
            }
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag)
    {
        if (ColorUtils.isColorLocked(stack))
            tooltip.add(ColorUtils.getFormatedColorName(ColorUtils.getInkColor(stack), true));

        super.appendHoverText(stack, level, tooltip, flag);

        if (!stack.getOrCreateTag().getBoolean("HideTooltip"))
        {
            if (!canRecharge(stack, false))
            {
                tooltip.add(Component.translatable("item.splatcraft.ink_tank.cant_recharge"));
            }

            if (flag.isAdvanced())
            {
                tooltip.add(Component.translatable("item.splatcraft.ink_tank.ink", String.format("%.1f", getInkAmount(stack)), capacity));
            }
        }
    }

    private static boolean initModels = false;

    @OnlyIn(Dist.CLIENT)
    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer)
    {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions()
        {
            @Override
            public @NotNull HumanoidModel<?> getHumanoidArmorModel(LivingEntity entityLiving, ItemStack itemStack, EquipmentSlot armorSlot, HumanoidModel<?> _default)
            {
                if (!initModels) //i have NO idea where else to put this
                {
                    initModels = true;
                    SplatcraftItems.registerArmorModels();
                }

                if (!(itemStack.getItem() instanceof InkTankItem))
                {
                    return IClientItemExtensions.DEFAULT.getHumanoidArmorModel(entityLiving, itemStack, armorSlot, _default);
                }

                if (model == null)
                {
                    return IClientItemExtensions.DEFAULT.getHumanoidArmorModel(entityLiving, itemStack, armorSlot, _default);
                }

                if (!itemStack.isEmpty())
                {
                    if (itemStack.getItem() instanceof InkTankItem item)
                    {
                        model.rightLeg.visible = armorSlot == EquipmentSlot.LEGS || armorSlot == EquipmentSlot.FEET;
                        model.leftLeg.visible = armorSlot == EquipmentSlot.LEGS || armorSlot == EquipmentSlot.FEET;

                        model.body.visible = armorSlot == EquipmentSlot.CHEST;
                        model.leftArm.visible = armorSlot == EquipmentSlot.CHEST;
                        model.rightArm.visible = armorSlot == EquipmentSlot.CHEST;

                        model.head.visible = armorSlot == EquipmentSlot.HEAD;
                        model.hat.visible = armorSlot == EquipmentSlot.HEAD;

                        model.crouching = _default.crouching;
                        model.riding = _default.riding;
                        model.young = _default.young;

                        model.rightArmPose = _default.rightArmPose;
                        model.leftArmPose = _default.leftArmPose;

                        model.setInkLevels(InkTankItem.getInkAmount(itemStack) / item.capacity);

                        return model;
                    }
                }

                return IClientItemExtensions.DEFAULT.getHumanoidArmorModel(entityLiving, itemStack, armorSlot, _default);
            }
        });
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack)
    {
        return (int) (getInkAmount(stack) / capacity * 13);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack)
    {
        return !SplatcraftConfig.Client.vanillaInkDurability.get() ? ColorUtils.getInkColor(stack) : super.getBarColor(stack);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isBarVisible(@NotNull ItemStack stack)
    {
        return (SplatcraftConfig.Client.inkIndicator.get().equals(SplatcraftConfig.InkIndicator.BOTH) || SplatcraftConfig.Client.inkIndicator.get().equals(SplatcraftConfig.InkIndicator.DURABILITY)) &&
                stack.getOrCreateTag().contains("Ink") && getInkAmount(stack) < capacity;
    }

    @Override
    public boolean isRepairable(@Nullable ItemStack stack)
    {
        return false;
    }

    public boolean canUse(Item item)
    {
        boolean inWhitelist = item.builtInRegistryHolder().is(SplatcraftTags.Items.INK_TANK_WHITELIST.get(this));
        boolean inBlacklist = item.builtInRegistryHolder().is(SplatcraftTags.Items.INK_TANK_BLACKLIST.get(this));

        return !inBlacklist && inWhitelist;
    }

    public void refill(ItemStack stack)
    {
        setInkAmount(stack, capacity);
    }

    public void setArmorModel(AbstractInkTankModel inkTankModel)
    {
        this.model = inkTankModel;
    }
}
