package com.cumulativecannons.munitions;

import com.cumulativecannons.Config;
import com.cumulativecannons.jet.JetActionSource;
import com.cumulativecannons.jet.JetMaterialCosts;
import com.cumulativecannons.network.ImpactShakePayload;
import com.cumulativecannons.network.JetBeamPayload;
import com.cumulativecannons.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import rbasamoyai.createbigcannons.config.CBCCfgMunitions;
import rbasamoyai.createbigcannons.effects.particles.explosions.ShellExplosionCloudParticleData;
import rbasamoyai.createbigcannons.effects.particles.impacts.DebrisMatterParticleData;
import rbasamoyai.createbigcannons.effects.particles.impacts.SparkBurstParticleData;
import rbasamoyai.createbigcannons.effects.particles.smoke.CannonSmokeParticleData;
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

public class CumulativeShellProjectile extends AbstractBigCannonProjectile {
    private static final double EFFECT_RADIUS = 200.0D;

    private boolean carving;
    private BlockPos startPos;
    private Direction.Axis axis;
    private int sign;
    private double fractionA;
    private double fractionB;
    private int blockOffset;
    private int layer;
    private double budget;
    private double currentCost = -1;
    private int maxBlocks;
    private LittleActionSource source;
    private Vec3 beamStart;
    private Vec3 beamDir;
    private Vec3 entryPos;
    private Vec3 exitPos;
    private BlockState lastSolidState;
    private boolean exitSpawned;
    private int particlesThisTick;

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
        beamStart = hit.getLocation();
        beamDir = direction.normalize();
        exitPos = null;
        lastSolidState = null;
        exitSpawned = false;
        particlesThisTick = 0;

        if (Math.abs(direction.x) >= Math.abs(direction.y) && Math.abs(direction.x) >= Math.abs(direction.z)) {
            axis = Direction.Axis.X;
            sign = direction.x >= 0 ? 1 : -1;
            fractionA = hit.getLocation().y - Math.floor(hit.getLocation().y);
            fractionB = hit.getLocation().z - Math.floor(hit.getLocation().z);
        } else if (Math.abs(direction.y) >= Math.abs(direction.z)) {
            axis = Direction.Axis.Y;
            sign = direction.y >= 0 ? 1 : -1;
            fractionA = hit.getLocation().x - Math.floor(hit.getLocation().x);
            fractionB = hit.getLocation().z - Math.floor(hit.getLocation().z);
        } else {
            axis = Direction.Axis.Z;
            sign = direction.z >= 0 ? 1 : -1;
            fractionA = hit.getLocation().x - Math.floor(hit.getLocation().x);
            fractionB = hit.getLocation().y - Math.floor(hit.getLocation().y);
        }

        startPos = hit.getBlockPos();
        blockOffset = 0;
        layer = 0;
        budget = Config.JET_BUDGET.get();
        maxBlocks = Config.JET_MAX_BLOCKS.get();
        currentCost = -1;
        source = new JetActionSource(level());

        PredictedExit predicted = predictExit();
        if (predicted.exit()) {
            lastSolidState = predicted.lastSolid;
        } else {
            lastSolidState = state;
        }
        Vec3 back = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        entryPos = hit.getLocation().add(back.scale(0.35D));
        spawnImpactEffect(hit.getLocation(), state, hit.getDirection());
        if (predicted.exit() && level() instanceof ServerLevel serverLevel) {
            spawnExitEffect(serverLevel, Vec3.atCenterOf(predicted.exitPos), predicted.lastSolid);
            exitSpawned = true;
        }

        carving = true;
        setDeltaMovement(Vec3.ZERO);
        return new ImpactResult(ImpactResult.KinematicOutcome.STOP, false);
    }

    private PredictedExit predictExit() {
        BlockState lastSolid = null;
        double remaining = Config.JET_BUDGET.get();
        for (int i = 0; i < maxBlocks; i++) {
            BlockPos pos = startPos.relative(axisDirection(), i);
            BlockState state = level().getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                if (lastSolid == null) {
                    return PredictedExit.NONE;
                }
                return new PredictedExit(pos, lastSolid);
            }
            if (Config.JET_RESPECT_PROTECTION.get() && state.getDestroySpeed(level(), pos) < 0) {
                return PredictedExit.NONE;
            }
            double cost = JetMaterialCosts.getCost(state);
            if (remaining < cost) {
                return PredictedExit.NONE;
            }
            remaining -= cost;
            lastSolid = state;
        }
        return PredictedExit.NONE;
    }

    private void spawnImpactEffect(Vec3 pos, BlockState state, Direction face) {
        if (!(level() instanceof ServerLevel serverLevel) || !Config.JET_EFFECTS_ENABLED.get()) {
            return;
        }
        BlockPos blockPos = BlockPos.containing(pos);
        boolean plume = level().getBlockState(blockPos.above()).isAir()
                && !level().getBlockState(blockPos.below()).isAir();

        // CBC HE shell visuals at half power (HE shell uses block power 8).
        sendParticle(serverLevel, new ShellExplosionCloudParticleData(4.0F, plume), pos, Vec3.ZERO);
        sendParticle(serverLevel, new ShellExplosionCloudParticleData(3.0F, false), offset(pos, 0.35D), Vec3.ZERO);

        Vec3 back = Vec3.atLowerCornerOf(face.getNormal()).normalize();
        Vec3 side = perpendicular(beamDir);

        for (int i = 0; i < scaledCount(90); i++) {
            Vec3 velocity = back.scale(0.01D + random.nextDouble() * 0.07D)
                    .add(side.scale((random.nextDouble() - 0.5D) * 0.09D))
                    .add(offset(Vec3.ZERO, 0.025D))
                    .add(0.0D, 0.02D + random.nextDouble() * 0.04D, 0.0D);
            int lifetime = 100 + random.nextInt(100);
            float scale = 4.0F + random.nextFloat() * 4.0F;
            sendParticle(serverLevel, new ShellExplosionSmokeParticleData(lifetime, scale), offset(pos, 0.35D), velocity);
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

        serverLevel.playSound(null, pos.x, pos.y, pos.z, CBCSoundEvents.SHELL_EXPLOSION.getMainEvent(),
                SoundSource.BLOCKS, 2.0F, 0.9F + random.nextFloat() * 0.1F);
        sendShake(serverLevel, pos);
        placeLight(serverLevel, blockPos.relative(face));
    }

    private void spawnExitEffect(ServerLevel serverLevel, Vec3 pos, BlockState state) {
        exitPos = pos;
        if (!Config.JET_EFFECTS_ENABLED.get()) {
            return;
        }
        sendParticle(serverLevel, new ShellExplosionCloudParticleData(2.0F, false), pos, Vec3.ZERO);

        for (int i = 0; i < scaledCount(4); i++) {
            Vec3 velocity = coneVelocity(beamDir, 35.0D, 1.2D + random.nextDouble() * 0.9D);
            sendParticle(serverLevel, new SparkBurstParticleData(1.0F, 0.62F, 0.18F, true, scaledCount(60)),
                    offset(pos, 0.12D), velocity);
        }
        for (int i = 0; i < scaledCount(4); i++) {
            Vec3 velocity = coneVelocity(beamDir, 4.0D, 4.0D + random.nextDouble() * 3.0D);
            sendParticle(serverLevel, new SparkBurstParticleData(1.0F, 0.88F, 0.55F, false, scaledCount(40)),
                    offset(pos, 0.06D), velocity);
        }

        for (int i = 0; i < scaledCount(100); i++) {
            double distance = random.nextDouble() * 6.0D;
            Vec3 point = pos.add(beamDir.scale(distance)).add(offset(Vec3.ZERO, 0.12D));
            Vec3 velocity = beamDir.scale(0.005D + random.nextDouble() * 0.02D)
                    .add(offset(Vec3.ZERO, 0.012D))
                    .add(0.0D, 0.008D + random.nextDouble() * 0.02D, 0.0D);
            float scale = 3.0F + random.nextFloat() * 3.0F;
            sendParticle(serverLevel, new ShellExplosionSmokeParticleData(80 + random.nextInt(80), scale), point, velocity);
        }

        for (int i = 0; i < scaledCount(40); i++) {
            double distance = random.nextDouble() * 4.0D;
            Vec3 point = pos.add(beamDir.scale(distance)).add(offset(Vec3.ZERO, 0.25D));
            Vec3 velocity = offset(Vec3.ZERO, 0.02D).add(0.0D, 0.02D, 0.0D);
            sendParticle(serverLevel, new CannonSmokeParticleData(2.0F, 1.5F + random.nextFloat(),
                    80 + random.nextInt(60), 0.9F), point, velocity);
        }

        if (state != null) {
            for (int i = 0; i < scaledCount(3); i++) {
                Vec3 velocity = beamDir.scale(0.6D + random.nextDouble() * 0.8D)
                        .add(offset(Vec3.ZERO, 0.25D));
                sendParticle(serverLevel, new DebrisMatterParticleData(false, true, state), offset(pos, 0.2D), velocity);
            }
        }

        sendBeam(serverLevel, pos, beamDir, 15.0D, 0.09F, 3);
        spawnJetSmokeStrip(serverLevel, pos, beamDir, 15.0D);
        serverLevel.playSound(null, pos.x, pos.y, pos.z, CBCSoundEvents.SHELL_EXPLOSION.getMainEvent(),
                SoundSource.BLOCKS, 1.2F, 1.0F + random.nextFloat() * 0.1F);
        placeLight(serverLevel, BlockPos.containing(pos));
    }

    private void spawnJetSmokeStrip(ServerLevel serverLevel, Vec3 start, Vec3 direction, double length) {
        int steps = Math.max(1, (int) Math.round(length / 0.35D));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(direction.scale(length * i / steps));
            for (int j = 0; j < 3; j++) {
                Vec3 velocity = direction.scale(0.005D + random.nextDouble() * 0.015D)
                        .add(offset(Vec3.ZERO, 0.01D))
                        .add(0.0D, 0.005D + random.nextDouble() * 0.012D, 0.0D);
                float scale = 2.5F + random.nextFloat() * 2.5F;
                sendParticle(serverLevel, new ShellExplosionSmokeParticleData(100 + random.nextInt(80), scale),
                        point.add(offset(Vec3.ZERO, 0.12D)), velocity);
            }
            if (i % 4 == 0) {
                Vec3 velocity = offset(Vec3.ZERO, 0.01D).add(0.0D, 0.005D, 0.0D);
                sendParticle(serverLevel, new CannonSmokeParticleData(2.0F, 1.5F + random.nextFloat(),
                        120 + random.nextInt(80), 0.9F), point.add(offset(Vec3.ZERO, 0.1D)), velocity);
            }
        }
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
        int processed = 0;
        while (processed < Config.JET_LAYERS_PER_TICK.get() && carving) {
            processed++;
            if (blockOffset >= maxBlocks) {
                finishCarving();
                return;
            }

            BlockPos pos = startPos.relative(axisDirection(), blockOffset);
            BlockState state = level().getBlockState(pos);

            if (state.isAir() || !state.getFluidState().isEmpty()) {
                if (!exitSpawned && lastSolidState != null && level() instanceof ServerLevel serverLevel) {
                    spawnExitEffect(serverLevel, Vec3.atCenterOf(pos), lastSolidState);
                    exitSpawned = true;
                }
                nextBlock();
                continue;
            }

            lastSolidState = state;

            if (currentCost < 0) {
                if (Config.JET_RESPECT_PROTECTION.get() && state.getDestroySpeed(level(), pos) < 0) {
                    finishCarving();
                    return;
                }
                currentCost = JetMaterialCosts.getCost(state);
                if (budget < currentCost) {
                    finishCarving();
                    return;
                }
                budget -= currentCost;
            }

            carveLayer(pos, state);

            layer++;
            if (layer >= 16) {
                nextBlock();
            }
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
        for (int i = 0; i < scaledCount(30); i++) {
            Vec3 velocity = offset(Vec3.ZERO, 0.025D).add(0.0D, 0.02D + random.nextDouble() * 0.03D, 0.0D);
            float scale = 3.0F + random.nextFloat() * 3.0F;
            sendParticle(serverLevel, new ShellExplosionSmokeParticleData(100 + random.nextInt(100), scale),
                    offset(entry, 0.2D), velocity);
        }
        for (int i = 0; i < scaledCount(15); i++) {
            Vec3 velocity = offset(Vec3.ZERO, 0.015D).add(0.0D, 0.015D, 0.0D);
            sendParticle(serverLevel, new CannonSmokeParticleData(2.0F, 1.5F + random.nextFloat(),
                    100 + random.nextInt(80), 0.92F), offset(entry, 0.3D), velocity);
        }

        if (exitPos != null) {
            for (int i = 0; i < scaledCount(45); i++) {
                double distance = random.nextDouble() * 2.5D;
                Vec3 point = exitPos.add(beamDir.scale(distance)).add(offset(Vec3.ZERO, 0.15D));
                Vec3 velocity = beamDir.scale(0.005D + random.nextDouble() * 0.015D)
                        .add(offset(Vec3.ZERO, 0.012D))
                        .add(0.0D, 0.012D, 0.0D);
                float scale = 3.0F + random.nextFloat() * 3.0F;
                sendParticle(serverLevel, new ShellExplosionSmokeParticleData(90 + random.nextInt(90), scale),
                        point, velocity);
            }
        }
    }

    private void carveLayer(BlockPos pos, BlockState state) {
        BETiles be;
        try {
            be = LittleAction.loadBE(source, level(), pos, null, true, 0);
        } catch (Exception e) {
            return;
        }
        if (be == null) {
            return;
        }

        try {
            if (be.getGrid().count < 16) {
                be.convertTo(LittleGrid.get(16));
            }

            LittleGrid grid = be.getGrid();
            int pixel = Math.max(1, grid.count / 16);
            int layerCount = Math.max(1, grid.count / pixel);
            if (layer >= layerCount) {
                return;
            }

            int localLayer = sign > 0 ? layer : layerCount - 1 - layer;
            int crossA = Mth.clamp((int) Math.floor(fractionA * grid.count - pixel / 2.0D), 0, grid.count - 2 * pixel);
            int crossB = Mth.clamp((int) Math.floor(fractionB * grid.count - pixel / 2.0D), 0, grid.count - 2 * pixel);
            int minLayer = localLayer * pixel;
            int maxLayer = minLayer + pixel;

            LittleBox box = switch (axis) {
                case X -> new LittleBox(minLayer, crossA, crossB, maxLayer, crossA + 2 * pixel, crossB + 2 * pixel);
                case Y -> new LittleBox(crossA, minLayer, crossB, crossA + 2 * pixel, maxLayer, crossB + 2 * pixel);
                case Z -> new LittleBox(crossA, crossB, minLayer, crossA + 2 * pixel, crossB + 2 * pixel, maxLayer);
            };

            LittleActionDestroyBoxes.removeBox(be, grid, box, true, null);
            if (be.isEmpty()) {
                be.convertBlockToVanilla();
            }
        } catch (Exception e) {
            // Keep the tunnel going even if one block cannot be converted or carved.
        }
    }

    private Direction axisDirection() {
        Direction positive = switch (axis) {
            case X -> Direction.EAST;
            case Y -> Direction.UP;
            case Z -> Direction.SOUTH;
        };
        return sign > 0 ? positive : positive.getOpposite();
    }

    private void nextBlock() {
        blockOffset++;
        layer = 0;
        currentCost = -1;
    }

    @Override
    protected boolean onHitEntity(Entity entity, ProjectileContext context) {
        return false;
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

    private record PredictedExit(BlockPos exitPos, BlockState lastSolid) {
        private static final PredictedExit NONE = new PredictedExit(null, null);
        private boolean exit() {
            return exitPos != null;
        }
    }
}
