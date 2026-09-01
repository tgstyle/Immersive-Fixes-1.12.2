package com.melodiccougar7.immersivefixes.helper;

import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.common.Config.IEConfig;
import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.items.ItemEarmuffs;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;

public final class EarmuffVolume {

    public static final float MIN_VOLUME = 0.001f;

    private EarmuffVolume() {}

    public static float getVolumeMod(ISound sound) {
        if (sound == null) { return 1; }
        SoundCategory category = sound.getCategory();
        if (!ItemEarmuffs.affectedSoundCategories.contains(category.getName())) { return 1; }
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) { return 1; }
        ItemStack head = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (head.isEmpty()) { return 1; }
        ItemStack earmuffs = ItemNBTHelper.hasKey(head, Lib.NBT_Earmuffs) ? ItemNBTHelper.getItemStack(head, Lib.NBT_Earmuffs) : head;
        if (earmuffs.isEmpty() || !IEContent.itemEarmuffs.equals(earmuffs.getItem())) { return 1; }
        if (ItemNBTHelper.getBoolean(earmuffs, "IE:Earmuffs:Cat_" + category.getName())) { return 1; }
        String name = sound.getSoundLocation().toString();
        for (String blacklist : IEConfig.Tools.earDefenders_SoundBlacklist) {
            if (blacklist != null && blacklist.equalsIgnoreCase(name)) { return 1; }
        }
        return ItemEarmuffs.getVolumeMod(earmuffs);
    }
}
