package com.melodiccougar7.immersivefixes.mixin;

import blusunrize.immersiveengineering.common.blocks.metal.TileEntityConnectorProbe;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityConnectorRedstone;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityConnectorProbe.class, remap = false)
public abstract class TileEntityConnectorProbeMixin extends TileEntityConnectorRedstone {

    @Shadow private int redstoneChannelSending;

    @Inject(method = "readCustomNBT(Lnet/minecraft/nbt/NBTTagCompound;Z)V", at = @At("TAIL"), require = 1)
    private void maskSendingChannel(NBTTagCompound nbt, boolean descPacket, CallbackInfo ci) {
        redstoneChannelSending &= 15;
    }
}
