package com.melodiccougar7.immersivefixes.mixin;

import blusunrize.immersiveengineering.client.ClientEventHandler;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientEventHandler.class, remap = false)
public abstract class ClientEventHandlerSoundMixin {

    @Inject(method = "onPlaySound", at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelMuffledSoundWrapper(PlaySoundEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
