package com.melodiccougar7.immersivefixes.mixin;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.energy.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.AbstractConnection;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.energy.wires.WireType;
import gnu.trove.map.TIntObjectMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ImmersiveNetHandler.class, remap = false)
public abstract class ImmersiveNetHandlerMixin {

    @Shadow public TIntObjectMap<Map<BlockPos, Set<AbstractConnection>>> indirectConnections;
    @Shadow public TIntObjectMap<Map<BlockPos, Set<AbstractConnection>>> indirectConnectionsIgnoreOut;
    @Shadow public abstract Set<Connection> getConnections(World world, BlockPos node);

    /**
     * @author tgstyle
     * @reason The same search without the original's quadratic list and queue scans.
     */
    @Overwrite
    public Set<AbstractConnection> getIndirectEnergyConnections(BlockPos node, World world, boolean ignoreIsEnergyOutput) {
        int dimension = world.provider.getDimension();
        if(!ignoreIsEnergyOutput&&indirectConnections.containsKey(dimension)&&indirectConnections.get(dimension).containsKey(node)) { return indirectConnections.get(dimension).get(node); }
        if(ignoreIsEnergyOutput&&indirectConnectionsIgnoreOut.containsKey(dimension)&&indirectConnectionsIgnoreOut.get(dimension).containsKey(node)) { return indirectConnectionsIgnoreOut.get(dimension).get(node); }

        PriorityQueue<Pair<IImmersiveConnectable, Float>> queue = new PriorityQueue<>(Comparator.comparingDouble(Pair::getRight));
        Map<IImmersiveConnectable, Pair<IImmersiveConnectable, Float>> queued = new IdentityHashMap<>();
        Set<AbstractConnection> closedList = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Set<BlockPos> checked = new HashSet<>();
        Map<BlockPos, BlockPos> backtracker = new HashMap<>();

        checked.add(node);
        Set<Connection> conL = getConnections(world, node);
        if(conL!=null) {
            for(Connection con : conL) {
                IImmersiveConnectable end = ApiUtils.toIIC(con.end, world);
                if(end!=null) {
                    immersivefixes$offer(queue, queued, end, con.getBaseLoss());
                    backtracker.put(con.end, node);
                }
            }
        }

        final int closedListMax = 1200;
        while(closedList.size() < closedListMax&&!queue.isEmpty()) {
            Pair<IImmersiveConnectable, Float> pair = queue.poll();
            IImmersiveConnectable next = pair.getLeft();
            if(queued.get(next)==pair) { queued.remove(next); }
            float loss = pair.getRight();
            BlockPos nextPos = ApiUtils.toBlockPos(next);
            if(checked.contains(nextPos)) { continue; }
            boolean isOutput = next.isEnergyOutput();
            if(ignoreIsEnergyOutput||isOutput) {
                BlockPos last = nextPos;
                WireType minimumType = null;
                int distance = 0;
                List<Connection> connectionParts = new ArrayList<>();
                while(last!=null) {
                    BlockPos prev = last;
                    last = backtracker.get(last);
                    if(last!=null) {
                        Set<Connection> conLB = getConnections(world, last);
                        if(conLB!=null) {
                            for(Connection conB : conLB) {
                                if(conB.end.equals(prev)) {
                                    connectionParts.add(0, conB);
                                    distance += conB.length;
                                    if(minimumType==null||conB.cableType.getTransferRate() < minimumType.getTransferRate()) { minimumType = conB.cableType; }
                                    break;
                                }
                            }
                        }
                    }
                }
                closedList.add(new AbstractConnection(ApiUtils.toBlockPos(node), nextPos, minimumType, distance, isOutput, connectionParts.toArray(new Connection[0])));
            }
            Set<Connection> conLN = getConnections(world, nextPos);
            if(conLN!=null) {
                for(Connection con : conLN) {
                    if(next.allowEnergyToPass(con)) {
                        IImmersiveConnectable end = ApiUtils.toIIC(con.end, world);
                        if(end!=null&&!checked.contains(con.end)&&immersivefixes$offer(queue, queued, end, con.getBaseLoss()+loss)) { backtracker.put(con.end, nextPos); }
                    }
                }
            }
            checked.add(nextPos);
        }

        if(FMLCommonHandler.instance().getEffectiveSide()==Side.SERVER) {
            if(ignoreIsEnergyOutput) {
                if(!indirectConnectionsIgnoreOut.containsKey(dimension)) { indirectConnectionsIgnoreOut.put(dimension, new ConcurrentHashMap<>()); }
                Map<BlockPos, Set<AbstractConnection>> conns = indirectConnectionsIgnoreOut.get(dimension);
                if(!conns.containsKey(node)) { conns.put(node, Collections.newSetFromMap(new ConcurrentHashMap<>())); }
                conns.get(node).addAll(closedList);
            }
            else {
                if(!indirectConnections.containsKey(dimension)) { indirectConnections.put(dimension, new ConcurrentHashMap<>()); }
                Map<BlockPos, Set<AbstractConnection>> conns = indirectConnections.get(dimension);
                if(!conns.containsKey(node)) { conns.put(node, Collections.newSetFromMap(new ConcurrentHashMap<>())); }
                conns.get(node).addAll(closedList);
            }
        }
        return closedList;
    }

    @Unique private static boolean immersivefixes$offer(PriorityQueue<Pair<IImmersiveConnectable, Float>> queue, Map<IImmersiveConnectable, Pair<IImmersiveConnectable, Float>> queued, IImmersiveConnectable end, float loss) {
        Pair<IImmersiveConnectable, Float> existing = queued.get(end);
        if(existing!=null&&existing.getRight() <= loss) { return false; }
        if(existing!=null) { queue.remove(existing); }
        Pair<IImmersiveConnectable, Float> added = new ImmutablePair<>(end, loss);
        queue.add(added);
        queued.put(end, added);
        return true;
    }
}
