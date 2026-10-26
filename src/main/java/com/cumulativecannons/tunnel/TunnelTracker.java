package com.cumulativecannons.tunnel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/** Remembers blocks whose LittleTiles were carved by a cumulative jet. */
public final class TunnelTracker extends SavedData {
    private static final String DATA_NAME = "cumulative_cannons_tunnels";

    private final Set<BlockPos> carved = new HashSet<>();

    public static TunnelTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TunnelTracker::new, TunnelTracker::load, null),
                DATA_NAME);
    }

    private static TunnelTracker load(CompoundTag tag, HolderLookup.Provider registries) {
        TunnelTracker data = new TunnelTracker();
        for (long packed : tag.getLongArray("carved")) {
            data.carved.add(BlockPos.of(packed));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        long[] packed = new long[carved.size()];
        int i = 0;
        for (BlockPos pos : carved) {
            packed[i++] = pos.asLong();
        }
        tag.putLongArray("carved", packed);
        return tag;
    }

    public boolean contains(BlockPos pos) {
        return carved.contains(pos);
    }

    public void add(BlockPos pos) {
        if (carved.add(pos.immutable())) {
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (carved.remove(pos)) {
            setDirty();
        }
    }
}
