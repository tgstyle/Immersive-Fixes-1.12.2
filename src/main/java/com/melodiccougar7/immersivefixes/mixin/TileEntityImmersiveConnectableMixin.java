package com.melodiccougar7.immersivefixes.mixin;

import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.energy.wires.TileEntityImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.WireType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityImmersiveConnectable.class, remap = false)
public abstract class TileEntityImmersiveConnectableMixin {

    @Inject(method = "getBaseDamage(Lblusunrize/immersiveengineering/api/energy/wires/ImmersiveNetHandler$Connection;)F", at = @At("RETURN"), cancellable = true, require = 1)
    private void shockOtherWireTypes(Connection c, CallbackInfoReturnable<Float> cir) {
        WireType type = c.cableType;
        if(cir.getReturnValueF()!=0F||type==null||!type.canCauseDamage()) { return; }
        int rate = type.getTransferRate();
        if(rate<=0) { return; }
        float tier = rate<=WireType.COPPER.getTransferRate()?2F: rate<=WireType.ELECTRUM.getTransferRate()?5F: 15F;
        cir.setReturnValue(8*tier/rate);
    }
}
