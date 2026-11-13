package com.melodiccougar7.immersivefixes.mixin;

import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Connection.class, remap = false)
public abstract class ConnectionMixin {

    @Shadow public BlockPos start;
    @Shadow public BlockPos end;

    /**
     * @author tgstyle
     * @reason Hashes only start and end, which is all equals compares once the transfer rate and length match, and skips the varargs array Objects.hash allocates on every lookup.
     */
    @Overwrite
    public int hashCode() {
        int result = 31+(start==null?0: start.hashCode());
        return 31*result+(end==null?0: end.hashCode());
    }

    @Inject(method = "readFromNBT(Lnet/minecraft/nbt/NBTTagCompound;)Lblusunrize/immersiveengineering/api/energy/wires/ImmersiveNetHandler$Connection;", at = @At("HEAD"), cancellable = true, require = 1)
    private static void rejectShortPositions(NBTTagCompound tag, CallbackInfoReturnable<Connection> cir) {
        if(tag!=null&&(tag.getIntArray("start").length < 3||tag.getIntArray("end").length < 3)) { cir.setReturnValue(null); }
    }
}
