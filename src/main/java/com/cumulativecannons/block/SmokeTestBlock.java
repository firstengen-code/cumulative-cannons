package com.cumulativecannons.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import rbasamoyai.createbigcannons.effects.particles.explosions.ShellExplosionCloudParticleData;
import rbasamoyai.createbigcannons.effects.particles.smoke.CannonSmokeParticleData;
import rbasamoyai.createbigcannons.effects.particles.smoke.ShellExplosionSmokeParticleData;

public class SmokeTestBlock extends Block {
    public enum SmokeType {
        WHITE,
        BLACK,
        CLOUD
    }

    private final SmokeType type;

    public SmokeTestBlock(Properties properties, SmokeType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 10);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.is(this)) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.1D;
        double z = pos.getZ() + 0.5D;
        switch (this.type) {
            case WHITE -> level.sendParticles(new ShellExplosionSmokeParticleData(180, 3.0F),
                    x, y, z, 8, 0.3D, 0.25D, 0.3D, 0.01D);
            case BLACK -> level.sendParticles(new CannonSmokeParticleData(2.0F, 1.5F + random.nextFloat(),
                            120 + random.nextInt(80), 0.9F),
                    x, y, z, 8, 0.3D, 0.25D, 0.3D, 0.01D);
            case CLOUD -> level.sendParticles(new ShellExplosionCloudParticleData(2.0F, false),
                    x, y, z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
        }
    }
}
