package com.melodiccougar7.immersivefixes.mixin;

import blusunrize.immersiveengineering.api.energy.wires.TileEntityImmersiveConnectable;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityConnectorRedstone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityConnectorRedstone.class, remap = false)
public abstract class TileEntityConnectorRedstoneMixin extends TileEntityImmersiveConnectable {

    @Shadow public int redstoneChannel;

    @Shadow public EnumFacing facing;

    @Inject(method = "readCustomNBT(Lnet/minecraft/nbt/NBTTagCompound;Z)V", at = @At("TAIL"), require = 1)
    private void maskChannel(NBTTagCompound nbt, boolean descPacket, CallbackInfo ci) {
        redstoneChannel &= 15;
    }

    @ModifyArg(method = "onChange()V", at = @At(value = "INVOKE", target = "Lblusunrize/immersiveengineering/common/blocks/metal/TileEntityConnectorRedstone;markBlockForUpdate(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)V"), index = 1, require = 1)
    private IBlockState attachedBlockState(IBlockState stateHere) {
        return world.getBlockState(pos.offset(facing));
    }
}
