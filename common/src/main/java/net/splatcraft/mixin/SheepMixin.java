package net.splatcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.DyeColor;
import net.splatcraft.data.capabilities.inkoverlay.InkOverlayCapability;
import net.splatcraft.util.InkColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SheepEntity.class)
public class SheepMixin
{
    @WrapOperation(method = "sheared", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SheepEntity;getColor()Lnet/minecraft/util/DyeColor;"))
    public DyeColor getWool(SheepEntity sheep, Operation<DyeColor> original)
    {
        if (InkOverlayCapability.hasCapability(sheep))
        {
            InkColor color = InkOverlayCapability.get(sheep).getWoolColor();
            if (color.isValid())
            {
                return color.getDyeColor();
            }
        }
        return original.call(sheep);
    }
//    @WrapOperation(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Sheep;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/entity/item/ItemEntity;"))
//    public ItemEntity spawnAtLocation(Sheep instance, ItemLike iItemProvider, int i, Operation<ItemEntity> original)
//    {
//        if (InkOverlayCapability.hasCapability(instance))
//        {
//            InkOverlayInfo info = InkOverlayCapability.get(instance);
//            if (info.getWoolColor() > -1)
//            {
//                return original.call(instance, ColorUtils.setColorLocked(ColorUtils.setInkColor(new ItemStack(SplatcraftItems.inkedWool.get()), info.getWoolColor()), true).getItem(), i);
//            }
//        }
//
//        return original.call(instance, iItemProvider, i);
//    }
}
