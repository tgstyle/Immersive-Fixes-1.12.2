package com.melodiccougar7.immersivefixes.mixin;

import com.melodiccougar7.immersivefixes.helper.ChunkDirty;
import com.melodiccougar7.immersivefixes.helper.ConnectorOutputMemo;
import com.melodiccougar7.immersivefixes.helper.ConnectorResolveMemo;
import com.melodiccougar7.immersivefixes.helper.ConnectorRoutes;

import blusunrize.immersiveengineering.api.energy.immersiveflux.FluxStorage;
import blusunrize.immersiveengineering.api.energy.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.AbstractConnection;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.energy.wires.TileEntityImmersiveConnectable;
import blusunrize.immersiveengineering.common.Config.IEConfig;
import blusunrize.immersiveengineering.common.EventHandler;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityConnectorLV;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityConnectorLV.class, remap = false)
public abstract class TileEntityConnectorLVMixin extends TileEntityImmersiveConnectable implements ConnectorOutputMemo {

    @Shadow private FluxStorage energyStorage;

    @Shadow public abstract int getMaxInput();

    @Shadow public abstract int getMaxOutput();

    @Unique private boolean immersivefixes$lastOutput;

    @Inject(method = "isEnergyOutput()Z", at = @At("RETURN"), require = 1)
    private void rememberOutput(CallbackInfoReturnable<Boolean> cir) {
        immersivefixes$lastOutput = cir.getReturnValue();
    }

    @Override
    public boolean immersivefixes$outputChanged() {
        boolean before = immersivefixes$lastOutput;
        return isEnergyOutput()!=before;
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true, remap = true, require = 1)
    private void stopRelayTicking(CallbackInfo ci) {
        if(world.isRemote||!isRelay()) { return; }
        EventHandler.REMOVE_FROM_TICKING.add(this);
        ci.cancel();
    }

    @Inject(method = "update", at = @At("HEAD"), remap = true, require = 1)
    private void notifyShockSourcesOnce(CallbackInfo ci) {
        if(world.isRemote||!IEConfig.enableWireDamage) { return; }
        int stored = energyStorage.getEnergyStored();
        if(stored<=0) { return; }
        for(AbstractConnection con : ImmersiveNetHandler.INSTANCE.getIndirectEnergyConnections(pos, world, true)) {
            if(con.cableType==null||immersivefixes$noShockingWire(con.end)) { continue; }
            IImmersiveConnectable end = ConnectorResolveMemo.resolve(con.end, world);
            if(end==null||!end.allowEnergyToPass(null)) { continue; }
            float loss = con.getAverageLossRate();
            end.addAvailableEnergy((1-loss)*stored, energy -> energyStorage.modifyEnergyStored((int)(-energy/(1-loss))));
        }
    }

    @Inject(method = "notifyAvailableEnergy(ILjava/util/Set;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void notifyFromUpdateOnly(CallbackInfo ci) {
        ci.cancel();
    }

    @Unique
    private boolean immersivefixes$noShockingWire(BlockPos node) {
        Set<Connection> wires = ImmersiveNetHandler.INSTANCE.getConnections(world, node);
        if(wires==null) { return true; }
        for(Connection wire : wires) {
            if(wire.cableType!=null&&wire.cableType.canCauseDamage()) { return false; }
        }
        return true;
    }

    /**
     * @author tgstyle
     * @reason Outputs come presorted from the route cache, and the per-hop bookkeeping runs only in the real pass.
     */
    @Overwrite
    public int transferEnergy(int energy, boolean simulate, int energyType) {
        if(world.isRemote) { return 0; }
        List<AbstractConnection> outputs = ((ConnectorRoutes)ImmersiveNetHandler.INSTANCE).immersivefixes$outputsByPriority(pos, world);
        if(outputs.isEmpty()) { return 0; }
        int powerLeft = Math.min(Math.min(getMaxOutput(), getMaxInput()), energy);
        int powerForSort = powerLeft;
        int count = outputs.size();
        int[] offered = new int[count];
        int sum = 0;
        for(int i = 0; i < count; i++) {
            AbstractConnection con = outputs.get(i);
            IImmersiveConnectable end = ConnectorResolveMemo.resolve(con.end, world);
            if(end==null) { continue; }
            int tempR = end.outputEnergy(Math.min(powerForSort, con.cableType.getTransferRate()), true, energyType);
            if(tempR > 0) {
                offered[i] = tempR;
                sum += tempR;
            }
        }
        if(sum<=0) { return 0; }
        int received = 0;
        for(int i = 0; i < count; i++) {
            if(offered[i]==0) { continue; }
            AbstractConnection con = outputs.get(i);
            IImmersiveConnectable end = ConnectorResolveMemo.resolve(con.end, world);
            if(end==null) { continue; }
            float prio = offered[i]/(float)sum;
            int output = Math.min(MathHelper.ceil(powerForSort*prio), powerLeft);
            int r = end.outputEnergy(Math.min(output, con.cableType.getTransferRate()), true, energyType);
            if(!simulate) { immersivefixes$deliver(con, end, r, energyType); }
            received += r;
            powerLeft -= r;
            if(powerLeft<=0) { break; }
        }
        return received;
    }

    @Unique
    private void immersivefixes$deliver(AbstractConnection con, IImmersiveConnectable end, int r, int energyType) {
        int maxInput = getMaxInput();
        int tempR = r-(int)Math.max(0, Math.floor(r*con.getPreciseLossRate(r, maxInput)));
        end.outputEnergy(tempR, false, energyType);
        Map<Connection, Integer> rates = ImmersiveNetHandler.INSTANCE.getTransferedRates(world.provider.getDimension());
        Set<IImmersiveConnectable> passed = new HashSet<>();
        float intermediaryLoss = 0;
        for(Connection sub : con.subConnections) {
            float length = sub.length/(float)sub.cableType.getMaxLength();
            float baseLoss = (float)sub.cableType.getLossRatio();
            float mod = (((maxInput-tempR)/(float)maxInput)/.25f)*.1f;
            intermediaryLoss = MathHelper.clamp(intermediaryLoss+length*(baseLoss+baseLoss*mod), 0, 1);
            rates.merge(sub, r, Integer::sum);
            IImmersiveConnectable subStart = ConnectorResolveMemo.resolve(sub.start, world);
            IImmersiveConnectable subEnd = ConnectorResolveMemo.resolve(sub.end, world);
            if(subStart!=null&&passed.add(subStart)) { subStart.onEnergyPassthrough(r-r*intermediaryLoss); }
            if(subEnd!=null&&passed.add(subEnd)) { subEnd.onEnergyPassthrough(r-r*intermediaryLoss); }
        }
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lblusunrize/immersiveengineering/common/blocks/metal/TileEntityConnectorLV;markDirty()V"), remap = true, require = 1)
    private void dirtyChunkAfterTransfer(TileEntityConnectorLV self) {
        ChunkDirty.mark(self);
    }

    @Redirect(method = "receiveEnergy(Lnet/minecraft/util/EnumFacing;IZ)I", at = @At(value = "INVOKE", target = "Lblusunrize/immersiveengineering/common/blocks/metal/TileEntityConnectorLV;markDirty()V", remap = true), require = 1)
    private void dirtyChunkOnReceive(TileEntityConnectorLV self) {
        ChunkDirty.mark(self);
    }
}
