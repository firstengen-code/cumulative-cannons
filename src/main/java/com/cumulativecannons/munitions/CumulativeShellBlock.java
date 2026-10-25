package com.cumulativecannons.munitions;

import com.cumulativecannons.registry.ModBlockEntities;
import com.cumulativecannons.registry.ModEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonProjectileBlockEntity;
import rbasamoyai.createbigcannons.munitions.big_cannon.InertProjectileBlock;

public class CumulativeShellBlock extends InertProjectileBlock {
    private static final MapCodec<CumulativeShellBlock> CODEC = simpleCodec(CumulativeShellBlock::new);

    public CumulativeShellBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public EntityType<? extends CumulativeShellProjectile> getAssociatedEntityType() {
        return ModEntities.CUMULATIVE_SHELL.get();
    }

    @Override
    public BlockEntityType<? extends BigCannonProjectileBlockEntity> getBlockEntityType() {
        return ModBlockEntities.CUMULATIVE_SHELL.get();
    }
}
