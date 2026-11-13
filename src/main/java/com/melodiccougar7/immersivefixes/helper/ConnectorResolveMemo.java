package com.melodiccougar7.immersivefixes.helper;

import blusunrize.immersiveengineering.api.ApiUtils;
import blusunrize.immersiveengineering.api.energy.wires.IImmersiveConnectable;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

@Mod.EventBusSubscriber public final class ConnectorResolveMemo {

    private static final Map<Integer, Memo> MEMOS = new HashMap<>();

    private ConnectorResolveMemo() {}

    public static IImmersiveConnectable resolve(BlockPos pos, World world) {
        Memo memo = MEMOS.computeIfAbsent(world.provider.getDimension(), dimension -> new Memo());
        long tick = world.getTotalWorldTime();
        if(memo.tick!=tick) {
            memo.tiles.clear();
            memo.tick = tick;
        }
        TileEntity hit = memo.tiles.get(pos);
        if(hit!=null&&!hit.isInvalid()) { return (IImmersiveConnectable)hit; }
        IImmersiveConnectable iic = ApiUtils.toIIC(pos, world);
        if(iic instanceof TileEntity) { memo.tiles.put(pos, (TileEntity)iic); }
        else { memo.tiles.remove(pos); }
        return iic;
    }

    @SubscribeEvent public static void onServerTick(ServerTickEvent event) {
        if(event.phase!=Phase.END) { return; }
        for(Memo memo : MEMOS.values()) { memo.tiles.clear(); }
    }

    @SubscribeEvent public static void onWorldUnload(WorldEvent.Unload event) {
        if(event.getWorld().isRemote) { return; }
        MEMOS.remove(event.getWorld().provider.getDimension());
    }

    private static final class Memo {
        private final Map<BlockPos, TileEntity> tiles = new HashMap<>();
        private long tick = -1;
    }
}
