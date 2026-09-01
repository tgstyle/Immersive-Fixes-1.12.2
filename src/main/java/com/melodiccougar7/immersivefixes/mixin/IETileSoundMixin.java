package com.melodiccougar7.immersivefixes.mixin;

import blusunrize.immersiveengineering.common.util.sound.IETileSound;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = IETileSound.class, remap = false)
public abstract class IETileSoundMixin {

    @Redirect(method = "evaluateVolume", at = @At(value = "INVOKE", target = "Lblusunrize/immersiveengineering/common/items/ItemEarmuffs;getVolumeMod(Lnet/minecraft/item/ItemStack;)F"), remap = false)
    private float skipEarmuffAdjustment(ItemStack stack) {
        return 1;
    }
}
