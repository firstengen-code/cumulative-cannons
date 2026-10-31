package com.cumulativecannons.munitions;

import com.cumulativecannons.Config;
import com.cumulativecannons.compat.ExplosionOverhaulCompat;
import com.cumulativecannons.jet.JetActionSource;
import com.cumulativecannons.jet.JetMaterialCosts;
import com.cumulativecannons.network.ImpactShakePayload;
import com.cumulativecannons.network.JetBeamPayload;
import com.cumulativecannons.registry.ModBlocks;
import com.cumulativecannons.registry.ModEffects;
import com.cumulativecannons.compat.SableCompat;
import com.cumulativecannons.tunnel.TunnelTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import rbasamoyai.createbigcannons.config.CBCCfgMunitions;
import rbasamoyai.createbigcannons.effects.particles.explosions.ShellExplosionCloudParticleData;
import rbasamoyai.createbigcannons.effects.particles.impacts.DebrisMatterParticleData;
import rbasamoyai.createbigcannons.effects.particles.impacts.SparkBurstParticleData;
import rbasamoyai.createbigcannons.effects.particles.smoke.ShellExplosionSmokeParticleData;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers;
import rbasamoyai.createbigcannons.index.CBCSoundEvents;
import rbasamoyai.createbigcannons.munitions.AbstractCannonProjectile.ImpactResult;
import rbasamoyai.createbigcannons.munitions.ProjectileContext;
import rbasamoyai.createbigcannons.munitions.big_cannon.AbstractBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.BigCannonProjectilePropertiesComponent;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.InertBigCannonProjectileProperties;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.EntityDamagePropertiesComponent;
import team.creative.littletiles.common.action.LittleAction;
import team.creative.littletiles.common.action.LittleActionDestroyBoxes;
import team.creative.littletiles.common.action.source.LittleActionSource;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CumulativeShellProjectile extends AbstractBigCannonProjectile {
    private static final double EFFECT_RADIUS = 200.0D;

    private boolean carving;
    private int blockOffset;
    private int maxBlocks;
    private List<TunnelBlock> tunnel = List.of();
    private LittleActionSource source;
    private Vec3 beamStart;
    private Vec3 beamDir;
    private Vec3 effectStart;
    private Vec3 effectDir = new Vec3(0.0D, -1.0D, 0.0D);
    private Vec3 entryPos;
    private Vec3 exitPos;
    private BlockState lastSolidState;
    private int particlesThisTick;
    private final Map<UUID, Float> shrapnelDamage = new HashMap<>();

    public CumulativeShellProjectile(EntityType<? extends CumulativeShellProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        if (carving) {
            if (!level().isClientSide) {
                carveStep();
                if (!carving) {
                    discard();
                }
            }
            return;
        }
        super.tick();
    }

    @Override
    protected ImpactResult calculateBlockPenetration(ProjectileContext context, BlockState state, BlockHitResult hit) {
        if (level().isClientSide) {
            return new ImpactResult(ImpactResult.KinematicOutcome.STOP, true);
        }
        if (!Config.JET_CARVE_ENABLED.get()) {
            return new ImpactResult(ImpactResult.KinematicOutcome.STOP, true);
        }
        if (Config.JET_RESPECT_GRIEF.get() && context.griefState() == CBCCfgMunitions.GriefState.NO_DAMAGE) {
            return new ImpactResult(ImpactResult.KinematicOutcome.STOP, true);
        }

        Vec3 direction = getDeltaMovement();
        if (direction.lengthSqr() < 1.0E-6D) {
            direction = Vec3.atLowerCornerOf(hit.getDirection().getOpposite().getNormal());
        }
        Vec3 worldDir = direction.normalize();
        Vec3 hitCenter = Vec3.atCenterOf(hit.getBlockPos());
        beamDir = SableCompat.toLocalDirection(level(), hitCenter, worldDir).normalize();
        if (beamDir.lengthSqr() < 1.0E-6D) {
            beamDir = worldDir;
        }
        Vec3 rawImpact = hitCenter.subtract(beamDir.scale(0.5D));
        if (!isSane(rawImpact)) {
            return new ImpactResult(ImpactResult.KinematicOutcome.STOP, true);
        }
        Vec3 impact = effectPoint(hitCenter);
        effectStart = impact;
        effectDir = worldDir;
        beamStart = rawImpact;
        exitPos = null;
        lastSolidState = null;
        particlesThisTick = 0;
        shrapnelDamage.clear();

        blockOffset = 0;
        maxBlocks = Config.JET_MAX_BLOCKS.get();
        source = new JetActionSource(level());
        TunnelPlan plan = planTunnel();
        tunnel = new ArrayList<>(plan.blocks());
        BlockPos hitBlock = hit.getBlockPos();
        Direction.Axis hitAxis = dominantAxis(beamDir);
        BlockPos entryBlock = BlockPos.containing(beamStart);
        boolean containsHit = tunnel.stream().anyMatch(b -> b.pos().equals(hitBlock));
        if (!containsHit) {
            tunnel.add(0, new TunnelBlock(hitBlock,
                    List.of(pixelBox(hitCenter, hitAxis))));
        }
        punchEntryFace(entryBlock);
        lastSolidState = plan.lastSolid() != null ? plan.lastSolid() : state;
        Vec3 back = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        entryPos = impact.add(back.scale(0.35D));
        if (level() instanceof ServerLevel serverLevel) {
            damageJetPath(serverLevel, effectStart, effectDir, tunnel.size());
            damageShockwave(serverLevel, impact, Config.SHOCKWAVE_RADIUS.get(),
                    Config.SHOCKWAVE_ENTRY_DAMAGE.get().floatValue());
            damageRicochetShrapnel(serverLevel, impact, back, 24, 0.9D);
            spawnBlackShrapnel(serverLevel, impact, back, 10, 2.5D);
        }
        spawnImpactEffect(impact, state, hit.getDirection());
        if (plan.exitPos() != null && plan.lastSolid() != null && level() instanceof ServerLevel serverLevel) {
            spawnExitEffect(serverLevel, effectPoint(Vec3.atCenterOf(plan.exitPos())), plan.lastSolid());
        }

        carving = true;
        setDeltaMovement(Vec3.ZERO);
        return new ImpactResult(ImpactResult.KinematicOutcome.STOP, false);
    }

    private TunnelPlan planTunnel() {
        List<TunnelBlock> blocks = new ArrayList<>();
        Map<BlockPos, List<BoxSpec>> boxesByPos = new LinkedHashMap<>();
        BlockState lastSolid = null;
        BlockPos exitPos = null;
        Direction.Axis axis = dominantAxis(beamDir);
        double remaining = Config.JET_BUDGET.get();
        double maxDistance = maxBlocks * 2.0D + 1.0D;
        double step = 1.0D / 16.0D;
        Vec3 start = beamStart.add(beamDir.scale(0.001D));

        for (double distance = 0.0D; distance <= maxDistance; distance += step) {
            Vec3 point = start.add(beamDir.scale(distance));
            BlockPos pos = BlockPos.containing(point);
            List<BoxSpec> boxes = boxesByPos.get(pos);
            if (boxes == null) {
                BlockState state = SableCompat.getBlockState(level(), pos);
                if (state.isAir() || !state.getFluidState().isEmpty()) {
                    exitPos = pos;
                    break;
                }
                if (Config.JET_RESPECT_PROTECTION.get() && state.getDestroySpeed(level(), pos) < 0) {
                    break;
                }
                if (blocks.size() >= maxBlocks) {
                    break;
                }
                double cost = JetMaterialCosts.getCost(state);
                if (remaining < cost) {
                    break;
                }
                remaining -= cost;
                boxes = new ArrayList<>();
                boxesByPos.put(pos, boxes);
                blocks.add(new TunnelBlock(pos, boxes));
                lastSolid = state;
            }
            BoxSpec box = pixelBox(point, axis);
            if (!boxes.contains(box)) {
                boxes.add(box);
            }
        }
        return new TunnelPlan(blocks, exitPos, lastSolid);
    }

    private static Direction.Axis dominantAxis(Vec3 direction) {
        if (Math.abs(direction.x) >= Math.abs(direction.y) && Math.abs(direction.x) >= Math.abs(direction.z)) {
            return Direction.Axis.X;
        }
        if (Math.abs(direction.y) >= Math.abs(direction.z)) {
            return Direction.Axis.Y;
        }
        return Direction.Axis.Z;
    }

    private static BoxSpec pixelBox(Vec3 point, Direction.Axis axis) {
        Vec3 local = point.subtract(Vec3.atLowerCornerOf(BlockPos.containing(point))).scale(16.0D);
        int x = Mth.clamp(Mth.floor(local.x), 0, 15);
        int y = Mth.clamp(Mth.floor(local.y), 0, 15);
        int z = Mth.clamp(Mth.floor(local.z), 0, 15);
        int centerX = Mth.clamp(Mth.floor(local.x + 0.5D), 1, 15) - 1;
        int centerY = Mth.clamp(Mth.floor(local.y + 0.5D), 1, 15) - 1;
        int centerZ = Mth.clamp(Mth.floor(local.z + 0.5D), 1, 15) - 1;
        return switch (axis) {
            case X -> new BoxSpec(x, centerY, centerZ, x + 1, centerY + 2, centerZ + 2);
            case Y -> new BoxSpec(centerX, y, centerZ, centerX + 2, y + 1, centerZ + 2);
            case Z -> new BoxSpec(centerX, centerY, z, centerX + 2, centerY + 2, z + 1);
        };
    }

    /**
     * Box for the skin layer of the face the beam enters through (pixel 0 or 15 of the beam axis),
     * keeping the 2x2 cross-section of the given point.
     */
    private BoxSpec entryPixelBox(Vec3 point, Direction.Axis axis) {
        BoxSpec base = pixelBox(point, axis);
        return switch (axis) {
            case X -> {
                int x = beamDir.x >= 0.0D ? 0 : 15;
                yield new BoxSpec(x, base.y0(), base.z0(), x + 1, base.y1(), base.z1());
            }
            case Y -> {
                int y = beamDir.y >= 0.0D ? 0 : 15;
                yield new BoxSpec(base.x0(), y, base.z0(), base.x1(), y + 1, base.z1());
            }
            case Z -> {
                int z = beamDir.z >= 0.0D ? 0 : 15;
                yield new BoxSpec(base.x0(), base.y0(), z, base.x1(), base.y1(), z + 1);
            }
        };
    }

    /**
     * A diagonal beam starts a fraction of a block inside the wall, so the outermost pixel layer of
     * the entry face is never sampled and stays intact: the shell pierces the block but leaves no
     * visible entrance hole. Punch that layer explicitly on the block the beam enters.
     */
    private void punchEntryFace(BlockPos entryBlock) {
        Direction.Axis axis = dominantAxis(beamDir);
        BoxSpec entry = entryPixelBox(beamStart, axis);
        for (int i = 0; i < tunnel.size(); i++) {
            TunnelBlock block = tunnel.get(i);
            if (!block.pos().equals(entryBlock)) {
                continue;
            }
            List<BoxSpec> boxes = new ArrayList<>(block.boxes());
            if (!boxes.contains(entry)) {
                boxes.add(0, entry);
                tunnel.set(i, new TunnelBlock(block.pos(), boxes));
            }
            return;
        }
    }

    private void spawnImpactEffect(Vec3 pos, BlockState state, Direction face) {
        if (!(level() instanceof ServerLevel serverLevel) || !Config.JET_EFFECTS_ENABLED.get()) {
            return;
        }
        BlockPos blockPos = BlockPos.containing(pos);
        boolean plume = level().getBlockState(blockPos.above()).isAir()
                && !level().getBlockState(blockPos.below()).isAir();

        // CBC HE shell visuals at half power (HE shell uses block power 8).
        int cloudSets = smokeCount(1, Config.SMOKE_EXPLOSION_CLOUD.get());
        for (int i = 0; i < cloudSets; i++) {
            sendParticle(serverLevel, new ShellExplosionCloudParticleData(4.0F, plume), pos, Vec3.ZERO);
            sendParticle(serverLevel, new ShellExplosionCloudParticleData(3.0F, false), offset(pos, 0.35D), Vec3.ZERO);
        }

        Vec3 back = Vec3.atLowerCornerOf(face.getNormal()).normalize();
        Vec3 side = perpendicular(effectDir);

        for (int i = 0; i < scaledCount(smokeCount(160, Config.SMOKE_EXTERNAL_WHITE.get())); i++) {
            Vec3 velocity = back.scale(0.01D + random.nextDouble() * 0.07D)
                    .add(side.scale((random.nextDouble() - 0.5D) * 0.09D))
                    .add(offset(Vec3.ZERO, 0.025D))
                    .add(0.0D, 0.02D + random.nextDouble() * 0.04D, 0.0D);
            int lifetime = 100 + random.nextInt(100);
            float scale = 4.0F + random.nextFloat() * 4.0F;
            sendParticle(serverLevel, new ShellExplosionSmokeParticleData(lifetime, scale), offset(pos, 0.35D), velocity);
        }

        for (int i = 0; i < scaledCount(smokeCount(40, Config.SMOKE_EXTERNAL_BLACK.get())); i++) {
            Vec3 velocity = back.scale(0.01D + random.nextDouble() * 0.05D)
                    .add(side.scale((random.nextDouble() - 0.5D) * 0.06D))
                    .add(0.0D, 0.015D + random.nextDouble() * 0.03D, 0.0D);
            sendParticle(serverLevel, new ShellExplosionSmokeParticleData(120 + random.nextInt(80), 3.0F),
                    offset(pos, 0.3D), velocity);
        }

        for (int i = 0; i < scaledCount(3); i++) {
            Vec3 velocity = back.scale(0.8D + random.nextDouble() * 0.8D)
                    .add(side.scale((random.nextDouble() - 0.5D) * 1.5D))
                    .add(0.0D, 0.2D + random.nextDouble() * 0.5D, 0.0D);
            sendParticle(serverLevel, new SparkBurstParticleData(1.0F, 0.72F, 0.28F, true, scaledCount(70)),
                    offset(pos, 0.12D), velocity);
        }

        if (state != null) {
            for (int i = 0; i < scaledCount(3); i++) {
                Vec3 velocity = back.scale(-0.15D - random.nextDouble() * 0.35D)
                        .add(side.scale((random.nextDouble() - 0.5D) * 0.4D))
                        .add(0.0D, 0.15D + random.nextDouble() * 0.35D, 0.0D);
                sendParticle(serverLevel, new DebrisMatterParticleData(false, true, state), offset(pos, 0.2D), velocity);
            }
        }

        ExplosionOverhaulCompat.triggerConcussion(serverLevel, pos);
        serverLevel.playSound(null, pos.x, pos.y, pos.z, CBCSoundEvents.SHELL_EXPLOSION.getMainEvent(),
                SoundSource.BLOCKS, 2.0F, 0.9F + random.nextFloat() * 0.1F);
        sendShake(serverLevel, pos);
        placeLight(serverLevel, blockPos.relative(face));
    }

    private void spawnExitEffect(ServerLevel serverLevel, Vec3 pos, BlockState state) {
        exitPos = pos;
        if (Config.DAMAGE_ENABLED.get()) {
            damageShockwave(serverLevel, pos, Config.SHOCKWAVE_RADIUS.get(),
                    Config.SHOCKWAVE_EXIT_DAMAGE.get().floatValue());
            damageRicochetShrapnel(serverLevel, pos, effectDir, 48, 1.5D);
            spawnBlackShrapnel(serverLevel, pos, effectDir, 20, 4.0D);
        }
        if (!Config.JET_EFFECTS_ENABLED.get()) {
            return;
        }
        for (int i = 0; i < smokeCount(1, Config.SMOKE_EXPLOSION_CLOUD.get()); i++) {
            sendParticle(serverLevel, new ShellExplosionCloudParticleData(2.0F, false), pos, Vec3.ZERO);
        }

        for (int i = 0; i < scaledCount(4); i++) {
            Vec3 velocity = coneVelocity(effectDir, 35.0D, 1.2D + random.nextDouble() * 0.9D);
            sendParticle(serverLevel, new SparkBurstParticleData(1.0F, 0.62F, 0.18F, true, scaledCount(60)),
                    offset(pos, 0.12D), velocity);
        }
        for (int i = 0; i < scaledCount(4); i++) {
            Vec3 velocity = coneVelocity(effectDir, 4.0D, 4.0D + random.nextDouble() * 3.0D);
            sendParticle(serverLevel, new SparkBurstParticleData(1.0F, 0.88F, 0.55F, false, scaledCount(40)),
                    offset(pos, 0.06D), velocity);
        }

        for (int i = 0; i < scaledCount(smokeCount(180, Config.SMOKE_INTERNAL_WHITE.get())); i++) {
            double distance = random.nextDouble() * 6.0D;
            Vec3 point = pos.add(effectDir.scale(distance)).add(offset(Vec3.ZERO, 0.12D));
            Vec3 velocity = effectDir.scale(0.005D + random.nextDouble() * 0.02D)
                    .add(offset(Vec3.ZERO, 0.012D))
                    .add(0.0D, 0.008D + random.nextDouble() * 0.02D, 0.0D);
            float scale = 3.0F + random.nextFloat() * 3.0F;
            sendParticle(serverLevel, new ShellExplosionSmokeParticleData(80 + random.nextInt(80), scale), point, velocity);
        }

        for (int i = 0; i < scaledCount(smokeCount(100, Config.SMOKE_INTERNAL_BLACK.get())); i++) {
            double distance = random.nextDouble() * 4.0D;
            Vec3 point = pos.add(effectDir.scale(distance)).add(offset(Vec3.ZERO, 0.25D));
            Vec3 velocity = offset(Vec3.ZERO, 0.02D).add(0.0D, 0.02D, 0.0D);
            sendParticle(serverLevel, new ShellExplosionSmokeParticleData(120 + random.nextInt(80), 3.0F),
                    point, velocity);
        }

        if (state != null) {
            for (int i = 0; i < scaledCount(3); i++) {
                Vec3 velocity = effectDir.scale(0.6D + random.nextDouble() * 0.8D)
                        .add(offset(Vec3.ZERO, 0.25D));
                sendParticle(serverLevel, new DebrisMatterParticleData(false, true, state), offset(pos, 0.2D), velocity);
            }
        }

        sendBeam(serverLevel, pos, effectDir, 15.0D, 0.09F, 3);
        spawnJetSmokeStrip(serverLevel, pos, effectDir, 15.0D);
        ExplosionOverhaulCompat.triggerConcussion(serverLevel, pos);
        serverLevel.playSound(null, pos.x, pos.y, pos.z, CBCSoundEvents.SHELL_EXPLOSION.getMainEvent(),
                SoundSource.BLOCKS, 1.2F, 1.0F + random.nextFloat() * 0.1F);
        placeLight(serverLevel, BlockPos.containing(pos));
    }

    private void spawnJetSmokeStrip(ServerLevel serverLevel, Vec3 start, Vec3 direction, double length) {
        int steps = Math.max(1, (int) Math.round(length / 0.35D));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(direction.scale(length * i / steps));
            int white = smokeCount(6, Config.SMOKE_INTERNAL_WHITE.get());
            for (int j = 0; j < white; j++) {
                Vec3 velocity = direction.scale(0.005D + random.nextDouble() * 0.015D)
                        .add(offset(Vec3.ZERO, 0.01D))
                        .add(0.0D, 0.005D + random.nextDouble() * 0.012D, 0.0D);
                float scale = 2.5F + random.nextFloat() * 2.5F;
                sendParticle(serverLevel, new ShellExplosionSmokeParticleData(100 + random.nextInt(80), scale),
                        point.add(offset(Vec3.ZERO, 0.12D)), velocity);
            }
            if (i % 4 == 0) {
                int black = smokeCount(2, Config.SMOKE_INTERNAL_BLACK.get());
                for (int j = 0; j < black; j++) {
                    Vec3 velocity = offset(Vec3.ZERO, 0.01D).add(0.0D, 0.005D, 0.0D);
                    sendParticle(serverLevel, new ShellExplosionSmokeParticleData(120 + random.nextInt(80), 3.0F),
                            point.add(offset(Vec3.ZERO, 0.1D)), velocity);
                }
            }
        }
    }

    private void spawnBlackShrapnel(ServerLevel serverLevel, Vec3 origin, Vec3 direction, int count, double speed) {
        if (!Config.JET_EFFECTS_ENABLED.get() || count <= 0) {
            return;
        }
        BlockState shard = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        for (int i = 0; i < scaledCount(count); i++) {
            Vec3 velocity = coneVelocity(direction, Config.SHRAPNEL_ANGLE.get(),
                    speed * (0.6D + random.nextDouble() * 0.8D));
            sendParticle(serverLevel, new DebrisMatterParticleData(false, true, shard),
                    offset(origin, 0.12D), velocity);
        }
    }

    private void damageJetPath(ServerLevel serverLevel, Vec3 start, Vec3 direction, int length) {
        if (!Config.DAMAGE_ENABLED.get() || length <= 0 || direction.lengthSqr() < 1.0E-6D
                || !isSane(start) || !isSane(direction)) {
            return;
        }
        Vec3 end = start.add(direction.scale(length));
        if (!isSane(end) || start.distanceToSqr(end) > 4096.0D) {
            return;
        }
        AABB area = new AABB(start, end).inflate(0.75D);
        for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!living.isAlive()) {
                continue;
            }
            Vec3 delta = living.getBoundingBox().getCenter().subtract(start);
            double distanceAlong = delta.dot(direction);
            if (distanceAlong < 0.0D || distanceAlong > length + 0.3D) {
                continue;
            }
            if (delta.subtract(direction.scale(distanceAlong)).length() > 0.55D) {
                continue;
            }
            float damage = (float) (Config.JET_DAMAGE.get()
                    * (1.0D - 0.6D * distanceAlong / Math.max(0.001D, length)));
            hurt(living, serverLevel.damageSources().magic(), damage);
            living.addEffect(new MobEffectInstance(ModEffects.BLEEDING, Config.BLEED_DURATION.get(), 0));
        }
    }

    private void damageShockwave(ServerLevel serverLevel, Vec3 center, double radius, float maxDamage) {
        if (!Config.DAMAGE_ENABLED.get() || radius <= 0.0D || maxDamage <= 0.0F || !isSane(center)) {
            return;
        }
        AABB area = new AABB(center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius);
        for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!living.isAlive()) {
                continue;
            }
            double distance = living.getBoundingBox().getCenter().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            float damage = (float) (maxDamage * (1.0D - distance / radius));
            if (damage <= 0.5F) {
                continue;
            }
            hurt(living, serverLevel.damageSources().explosion(this, null), damage);
            Vec3 push = living.position().subtract(center);
            if (push.lengthSqr() > 1.0E-6D) {
                push = push.normalize().scale(0.5D * (1.0D - distance / radius));
                living.push(push.x, 0.15D, push.z);
            }
        }
    }

    private void damageRicochetShrapnel(ServerLevel serverLevel, Vec3 origin, Vec3 direction,
                                        int count, double speed) {
        if (!Config.DAMAGE_ENABLED.get() || count <= 0 || direction.lengthSqr() < 1.0E-6D
                || !isSane(origin) || !isSane(direction)) {
            return;
        }

        Vec3 baseDirection = direction.normalize();
        int maxBounces = Config.SHRAPNEL_BOUNCES.get();
        double range = Config.SHRAPNEL_RANGE.get();

        for (int i = 0; i < count; i++) {
            Vec3 dir = coneVelocity(baseDirection, Config.SHRAPNEL_ANGLE.get(), 1.0D).normalize();
            Vec3 pos = origin.add(offset(Vec3.ZERO, 0.1D));

            for (int bounce = 0; bounce <= maxBounces; bounce++) {
                Vec3 end = pos.add(dir.scale(range));
                if (!isSane(pos) || !isSane(end)) {
                    break;
                }
                BlockHitResult blockHit = serverLevel.clip(new ClipContext(
                        pos, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (blockHit.getType() == HitResult.Type.BLOCK && !isSane(blockHit.getLocation())) {
                    break;
                }
                double blockDistance = blockHit.getType() == HitResult.Type.BLOCK
                        ? pos.distanceTo(blockHit.getLocation())
                        : range;

                LivingEntity hit = findEntityAlong(serverLevel, pos, dir, blockDistance);
                if (hit != null) {
                    damageWithShrapnel(serverLevel, hit, dir);
                    spawnShardParticle(serverLevel, hit.getBoundingBox().getCenter(), dir, speed);
                    break;
                }

                if (blockHit.getType() != HitResult.Type.BLOCK) {
                    break;
                }

                spawnShardParticle(serverLevel, blockHit.getLocation(), dir, speed);
                dir = reflect(dir, blockHit.getDirection());
                pos = blockHit.getLocation().add(dir.scale(0.02D));
            }
        }
    }

    private LivingEntity findEntityAlong(ServerLevel serverLevel, Vec3 start, Vec3 direction, double distance) {
        if (distance <= 0.0D || distance > 64.0D || !isSane(start) || !isSane(direction)) {
            return null;
        }
        Vec3 end = start.add(direction.scale(distance));
        if (!isSane(end) || start.distanceToSqr(end) > 4096.0D) {
            return null;
        }
        AABB area = new AABB(start, end).inflate(0.35D);
        LivingEntity closest = null;
        double closestDistance = distance + 0.01D;

        for (LivingEntity living : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!living.isAlive()) {
                continue;
            }
            Optional<Vec3> hit = living.getBoundingBox().inflate(0.08D).clip(start, end);
            if (hit.isEmpty()) {
                continue;
            }
            double hitDistance = start.distanceTo(hit.get());
            if (hitDistance < closestDistance) {
                closestDistance = hitDistance;
                closest = living;
            }
        }
        return closest;
    }

    private void damageWithShrapnel(ServerLevel serverLevel, LivingEntity living, Vec3 direction) {
        float maximum = Config.SHRAPNEL_MAX_DAMAGE.get().floatValue();
        float already = shrapnelDamage.getOrDefault(living.getUUID(), 0.0F);
        if (already >= maximum) {
            return;
        }
        float damage = Math.min(Config.SHRAPNEL_DAMAGE.get().floatValue(), maximum - already);
        if (damage <= 0.0F) {
            return;
        }
        if (hurt(living, serverLevel.damageSources().magic(), damage)) {
            shrapnelDamage.put(living.getUUID(), already + damage);
            living.addEffect(new MobEffectInstance(ModEffects.BLEEDING, Config.BLEED_DURATION.get(), 0));
            living.push(direction.x * 0.25D, 0.08D, direction.z * 0.25D);
        }
    }

    private void spawnShardParticle(ServerLevel serverLevel, Vec3 pos, Vec3 direction, double speed) {
        sendParticle(serverLevel, new DebrisMatterParticleData(false, true,
                Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState()), pos, direction.scale(speed * 0.8D));
    }

    private Vec3 effectPoint(Vec3 rawPoint) {
        Vec3 worldPoint = SableCompat.toWorld(level(), rawPoint);
        if (worldPoint == null || !isSane(worldPoint)
                || worldPoint.distanceToSqr(position()) > 4096.0D) {
            return position();
        }
        return worldPoint;
    }

    private static boolean isSane(Vec3 point) {
        return point != null
                && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z)
                && Math.abs(point.x) < 3.0E7D && Math.abs(point.y) < 3.0E7D && Math.abs(point.z) < 3.0E7D;
    }

    private static Vec3 reflect(Vec3 direction, Direction face) {
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        return direction.subtract(normal.scale(2.0D * direction.dot(normal))).normalize();
    }

    private static boolean hurt(LivingEntity living, DamageSource source, float amount) {
        if (amount <= 0.0F) {
            return false;
        }
        living.invulnerableTime = 0;
        return living.hurt(source, amount);
    }

    private void sendShake(ServerLevel serverLevel, Vec3 pos) {
        if (!Config.JET_CAMERA_SHAKE.get()) {
            return;
        }
        float strength = Config.JET_SHAKE_STRENGTH.get().floatValue();
        if (strength <= 0.0F) {
            return;
        }
        PacketDistributor.sendToPlayersNear(
                serverLevel,
                null,
                pos.x,
                pos.y,
                pos.z,
                EFFECT_RADIUS,
                new ImpactShakePayload(pos.x, pos.y, pos.z, strength, 8)
        );
    }

    private void sendBeam(ServerLevel serverLevel, Vec3 start, Vec3 direction, double length, float width, int life) {
        Vec3 end = start.add(direction.scale(length));
        PacketDistributor.sendToPlayersNear(
                serverLevel,
                null,
                start.x,
                start.y,
                start.z,
                EFFECT_RADIUS,
                new JetBeamPayload(start.x, start.y, start.z, end.x, end.y, end.z, width, life)
        );
    }

    private void emit(ServerLevel serverLevel, ParticleOptions particle, Vec3 pos, int count, double spread, double speed) {
        if (count <= 0 || !Config.JET_EFFECTS_ENABLED.get()) {
            return;
        }
        int limit = Config.JET_EFFECT_LIMIT.get();
        if (particlesThisTick >= limit) {
            return;
        }
        int scaled = Math.min(count, limit - particlesThisTick);
        if (scaled <= 0) {
            return;
        }
        sendParticlesNear(serverLevel, particle, pos, scaled, spread, spread, spread, (float) speed);
        particlesThisTick += scaled;
    }

    private void sendParticle(ServerLevel serverLevel, ParticleOptions particle, Vec3 pos, Vec3 velocity) {
        if (!Config.JET_EFFECTS_ENABLED.get()) {
            return;
        }
        int limit = Config.JET_EFFECT_LIMIT.get();
        if (particlesThisTick >= limit) {
            return;
        }
        sendParticlesNear(serverLevel, particle, pos, 0, velocity.x, velocity.y, velocity.z, 1.0F);
        particlesThisTick++;
    }

    private void sendParticlesNear(ServerLevel serverLevel, ParticleOptions particle, Vec3 pos,
                                   int count, double xDist, double yDist, double zDist, float speed) {
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
                particle, true, pos.x, pos.y, pos.z,
                (float) xDist, (float) yDist, (float) zDist, speed, count);
        double radiusSqr = EFFECT_RADIUS * EFFECT_RADIUS;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.position().distanceToSqr(pos.x, pos.y, pos.z) <= radiusSqr) {
                player.connection.send(packet);
            }
        }
    }

    private int scaledCount(int base) {
        return Math.max(0, (int) Math.round(base * Config.JET_EFFECT_MULTIPLIER.get()));
    }

    private int smokeCount(int base, int percent) {
        return Math.max(0, (int) Math.round(base * percent / 100.0D));
    }

    private static Vec3 offset(Vec3 pos, double amount) {
        double x = (Math.random() * 2.0D - 1.0D) * amount;
        double y = (Math.random() * 2.0D - 1.0D) * amount;
        double z = (Math.random() * 2.0D - 1.0D) * amount;
        return pos.add(x, y, z);
    }

    private static Vec3 perpendicular(Vec3 direction) {
        Vec3 helper = Math.abs(direction.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        return direction.cross(helper).normalize();
    }

    private static Vec3 coneVelocity(Vec3 direction, double angleDegrees, double speed) {
        Vec3 helper = Math.abs(direction.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 right = direction.cross(helper).normalize();
        Vec3 up = right.cross(direction).normalize();
        double cosMax = Math.cos(Math.toRadians(angleDegrees));
        double cosTheta = 1.0D - Math.random() * (1.0D - cosMax);
        double sinTheta = Math.sqrt(Math.max(0.0D, 1.0D - cosTheta * cosTheta));
        double phi = Math.random() * Math.PI * 2.0D;
        return direction.scale(cosTheta)
                .add(right.scale(sinTheta * Math.cos(phi)))
                .add(up.scale(sinTheta * Math.sin(phi)))
                .normalize()
                .scale(speed);
    }

    private void placeLight(ServerLevel serverLevel, BlockPos pos) {
        if (!Config.JET_IMPACT_LIGHT.get() || !serverLevel.getBlockState(pos).isAir()) {
            return;
        }
        serverLevel.setBlock(pos, Blocks.LIGHT.defaultBlockState(), 3);
        serverLevel.getServer().tell(new TickTask(serverLevel.getServer().getTickCount() + 2, () -> {
            if (serverLevel.getBlockState(pos).is(Blocks.LIGHT)) {
                serverLevel.removeBlock(pos, false);
            }
        }));
    }

    private void carveStep() {
        particlesThisTick = 0;
        int blocksPerTick = Math.max(1, Config.JET_LAYERS_PER_TICK.get() / 16);
        int processed = 0;
        while (processed < blocksPerTick && carving) {
            processed++;
            if (blockOffset >= tunnel.size()) {
                finishCarving();
                return;
            }

            TunnelBlock block = tunnel.get(blockOffset);
            carveBlock(block.pos(), block.boxes());
            blockOffset++;
        }
        if (blockOffset >= tunnel.size()) {
            finishCarving();
        }
    }

    private void finishCarving() {
        if (!carving) {
            return;
        }
        spawnCoolingEffects();
        carving = false;
    }

    private void spawnCoolingEffects() {
        if (!(level() instanceof ServerLevel serverLevel) || !Config.JET_EFFECTS_ENABLED.get()) {
            return;
        }

        Vec3 entry = entryPos != null ? entryPos : beamStart;
        for (int i = 0; i < scaledCount(smokeCount(60, Config.SMOKE_EXTERNAL_WHITE.get())); i++) {
            Vec3 velocity = offset(Vec3.ZERO, 0.025D).add(0.0D, 0.02D + random.nextDouble() * 0.03D, 0.0D);
            float scale = 3.0F + random.nextFloat() * 3.0F;
            sendParticle(serverLevel, new ShellExplosionSmokeParticleData(100 + random.nextInt(100), scale),
                    offset(entry, 0.2D), velocity);
        }
        for (int i = 0; i < scaledCount(smokeCount(40, Config.SMOKE_EXTERNAL_BLACK.get())); i++) {
            Vec3 velocity = offset(Vec3.ZERO, 0.015D).add(0.0D, 0.015D, 0.0D);
            sendParticle(serverLevel, new ShellExplosionSmokeParticleData(120 + random.nextInt(80), 3.0F),
                    offset(entry, 0.3D), velocity);
        }

        if (exitPos != null) {
            for (int i = 0; i < scaledCount(smokeCount(80, Config.SMOKE_EXTERNAL_WHITE.get())); i++) {
                double distance = random.nextDouble() * 2.5D;
                Vec3 point = exitPos.add(effectDir.scale(distance)).add(offset(Vec3.ZERO, 0.15D));
                Vec3 velocity = effectDir.scale(0.005D + random.nextDouble() * 0.015D)
                        .add(offset(Vec3.ZERO, 0.012D))
                        .add(0.0D, 0.012D, 0.0D);
                float scale = 3.0F + random.nextFloat() * 3.0F;
                sendParticle(serverLevel, new ShellExplosionSmokeParticleData(90 + random.nextInt(90), scale),
                        point, velocity);
            }
        }
    }

    private void carveBlock(BlockPos pos, List<BoxSpec> specs) {
        BlockState before = SableCompat.getBlockState(level(), pos);
        if (before.isAir()) {
            return;
        }
        BETiles be = null;
        if (SableCompat.isSubLevel(level(), pos)) {
            be = SableCompat.loadTiles(level(), pos, before);
        } else {
            try {
                be = LittleAction.loadBE(source, level(), pos, null, true, 0);
            } catch (Exception e) {
                removeWholeBlock(pos);
                return;
            }
        }
        if (be == null) {
            removeWholeBlock(pos);
            return;
        }

        try {
            if (be.getGrid().count < 16) {
                be.convertTo(LittleGrid.get(16));
            }

            LittleGrid grid = be.getGrid();
            double scale = grid.count / 16.0D;
            List<LittleBox> boxes = new ArrayList<>(specs.size());
            for (BoxSpec spec : specs) {
                int x0 = Mth.clamp((int) Math.round(spec.x0() * scale), 0, grid.count - 1);
                int y0 = Mth.clamp((int) Math.round(spec.y0() * scale), 0, grid.count - 1);
                int z0 = Mth.clamp((int) Math.round(spec.z0() * scale), 0, grid.count - 1);
                int x1 = Mth.clamp((int) Math.round(spec.x1() * scale), x0 + 1, grid.count);
                int y1 = Mth.clamp((int) Math.round(spec.y1() * scale), y0 + 1, grid.count);
                int z1 = Mth.clamp((int) Math.round(spec.z1() * scale), z0 + 1, grid.count);
                boxes.add(new LittleBox(x0, y0, z0, x1, y1, z1));
            }

            if (!boxes.isEmpty()) {
                LittleActionDestroyBoxes.removeBoxes(be, grid, boxes);
            }
            if (level() instanceof ServerLevel serverLevel) {
                if (be.isEmpty()) {
                    if (!SableCompat.removeBlock(serverLevel, pos)) {
                        be.convertBlockToVanilla();
                    }
                    TunnelTracker.get(serverLevel).remove(pos);
                } else {
                    TunnelTracker.get(serverLevel).add(pos);
                }
            } else if (be.isEmpty()) {
                be.convertBlockToVanilla();
            }
        } catch (Exception e) {
            removeWholeBlock(pos);
        }
    }

    private void removeWholeBlock(BlockPos pos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState state = serverLevel.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        if (Config.JET_RESPECT_PROTECTION.get() && state.getDestroySpeed(serverLevel, pos) < 0) {
            return;
        }
        if (!SableCompat.removeBlock(serverLevel, pos)) {
            serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    protected boolean onHitEntity(Entity entity, ProjectileContext context) {
        if (level().isClientSide || !(entity instanceof LivingEntity living)
                || !(level() instanceof ServerLevel serverLevel)) {
            return true;
        }
        if (Config.DAMAGE_ENABLED.get()) {
            Vec3 direction = getDeltaMovement();
            effectDir = direction.lengthSqr() > 1.0E-6D ? direction.normalize() : new Vec3(0.0D, -1.0D, 0.0D);
            effectStart = living.position();
            beamDir = effectDir;
            beamStart = living.position();
            hurt(living, serverLevel.damageSources().magic(),
                    Config.DIRECT_HIT_DAMAGE.get().floatValue());
            living.addEffect(new MobEffectInstance(ModEffects.BLEEDING, Config.BLEED_DURATION.get(), 0));
            damageShockwave(serverLevel, living.position(), Config.SHOCKWAVE_RADIUS.get(),
                    Config.SHOCKWAVE_ENTRY_DAMAGE.get().floatValue());
            spawnBlackShrapnel(serverLevel, living.position(), effectDir, 10, 2.5D);
            spawnImpactEffect(living.position(), null, Direction.UP);
        }
        return true;
    }

    @Override
    public BlockState getRenderedBlockState() {
        return ModBlocks.CUMULATIVE_SHELL.get().defaultBlockState()
                .setValue(DirectionalBlock.FACING, Direction.NORTH);
    }

    private InertBigCannonProjectileProperties properties() {
        return CBCMunitionPropertiesHandlers.INERT_BIG_CANNON_PROJECTILE.getPropertiesOf(this);
    }

    @Override
    public BigCannonProjectilePropertiesComponent getBigCannonProjectileProperties() {
        return properties().bigCannonProperties();
    }

    @Override
    public EntityDamagePropertiesComponent getDamageProperties() {
        return properties().damage();
    }

    @Override
    public BallisticPropertiesComponent getBallisticProperties() {
        return properties().ballistics();
    }

    private record BoxSpec(int x0, int y0, int z0, int x1, int y1, int z1) {}

    private record TunnelBlock(BlockPos pos, List<BoxSpec> boxes) {}

    private record TunnelPlan(List<TunnelBlock> blocks, BlockPos exitPos, BlockState lastSolid) {}
}
