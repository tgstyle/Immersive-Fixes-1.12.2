package com.melodiccougar7.immersivefixes.mixin;

import com.melodiccougar7.immersivefixes.helper.EarmuffVolume;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Inject(method = "getClampedVolume", at = @At("RETURN"), cancellable = true)
    private void applyEarmuffVolume(ISound soundIn, CallbackInfoReturnable<Float> cir) {
        float clamped = cir.getReturnValue();
        if (clamped <= 0) { return; }
        float mod = EarmuffVolume.getVolumeMod(soundIn);
        if (mod >= 1) { return; }
        cir.setReturnValue(Math.max(clamped * mod, EarmuffVolume.MIN_VOLUME));
    }
}
