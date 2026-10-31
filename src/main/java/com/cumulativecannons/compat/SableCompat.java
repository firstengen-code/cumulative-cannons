package com.cumulativecannons.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import team.creative.littletiles.common.block.entity.BETiles;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.block.mc.BlockTile;
import team.creative.littletiles.common.math.box.LittleBox;

/**
 * Sable sub-level helpers. Reflection-only so the addon still loads without Sable.
 */
public final class SableCompat {
    private static boolean unavailable;

    private SableCompat() {
    }

    public static Vec3 toWorld(Level level, Vec3 subLevelPos) {
        if (unavailable || subLevelPos == null) {
            return subLevelPos;
        }
        try {
            Object plot = findPlot(level, subLevelPos);
            if (plot == null) {
                return subLevelPos;
            }
            Object subLevel = plot.getClass().getMethod("getSubLevel").invoke(plot);
            Object pose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
            return (Vec3) pose.getClass().getMethod("transformPosition", Vec3.class).invoke(pose, subLevelPos);
        } catch (ClassNotFoundException e) {
            unavailable = true;
            return subLevelPos;
        } catch (Throwable ignored) {
            return subLevelPos;
        }
    }

    public static boolean isSubLevel(Level level, BlockPos pos) {
        if (unavailable || pos == null) {
            return false;
        }
        try {
            return findPlot(level, Vec3.atCenterOf(pos)) != null;
        } catch (ClassNotFoundException e) {
            unavailable = true;
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static BETiles loadTiles(Level level, BlockPos pos, BlockState state) {
        if (unavailable || pos == null) {
            return null;
        }
        try {
            Object plot = findPlot(level, Vec3.atCenterOf(pos));
            if (plot == null) {
                return null;
            }
            LevelChunk chunk = chunk(plot, pos);
            if (chunk == null) {
                return null;
            }
            BETiles be = new BETiles(pos, BlockTile.getStateByAttribute(level, pos, 0));
            be.setLevel(level);
            LittleGroup group = new LittleGroup();
            LittleBox full = new LittleBox(0, 0, 0, group.getGrid().count, group.getGrid().count, group.getGrid().count);
            group.add(group.getGrid(), new LittleElement(state, -1), full);
            be.updateTilesSecretly(interactor -> {
                for (LittleTile tile : group) {
                    interactor.noneStructureTiles().add(tile);
                }
            });
            BlockState tilesState = BlockTile.getState(be);
            chunk.setBlockState(pos, tilesState, true);
            chunk.setBlockEntity(be);
            plot.getClass().getMethod("onBlockChange", BlockPos.class, BlockState.class)
                    .invoke(plot, pos, tilesState);
            be.updateTiles();
            if (level instanceof ServerLevel server) {
                Packet<?> update = be.getUpdatePacket();
                ClientboundBlockUpdatePacket blockUpdate = new ClientboundBlockUpdatePacket(pos, tilesState);
                for (ServerPlayer player : server.players()) {
                    player.connection.send(blockUpdate);
                    if (update != null) {
                        player.connection.send(update);
                    }
                }
            }
            return be;
        } catch (ClassNotFoundException e) {
            unavailable = true;
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Vec3 toLocalDirection(Level level, Vec3 localPos, Vec3 worldDirection) {
        if (unavailable || localPos == null || worldDirection == null) {
            return worldDirection;
        }
        try {
            Object plot = findPlot(level, localPos);
            if (plot == null) {
                return worldDirection;
            }
            Object subLevel = plot.getClass().getMethod("getSubLevel").invoke(plot);
            Object pose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
            return (Vec3) pose.getClass().getMethod("transformNormalInverse", Vec3.class)
                    .invoke(pose, worldDirection);
        } catch (ClassNotFoundException e) {
            unavailable = true;
            return worldDirection;
        } catch (Throwable ignored) {
            return worldDirection;
        }
    }

    public static BlockState getBlockState(Level level, BlockPos pos) {
        if (unavailable || pos == null) {
            return level.getBlockState(pos);
        }
        try {
            Object plot = findPlot(level, Vec3.atCenterOf(pos));
            if (plot != null) {
                LevelChunk chunk = chunk(plot, pos);
                if (chunk != null) {
                    return chunk.getBlockState(pos);
                }
            }
        } catch (ClassNotFoundException e) {
            unavailable = true;
        } catch (Throwable ignored) {
            // Fall through to the normal level.
        }
        return level.getBlockState(pos);
    }

    public static boolean removeBlock(Level level, BlockPos pos) {
        if (unavailable || pos == null) {
            return false;
        }
        try {
            Object plot = findPlot(level, Vec3.atCenterOf(pos));
            if (plot == null) {
                return false;
            }
            LevelChunk chunk = chunk(plot, pos);
            if (chunk == null) {
                return false;
            }
            BlockState air = Blocks.AIR.defaultBlockState();
            chunk.setBlockState(pos, air, false);
            plot.getClass().getMethod("onBlockChange", BlockPos.class, BlockState.class).invoke(plot, pos, air);
            return true;
        } catch (ClassNotFoundException e) {
            unavailable = true;
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object findPlot(Level level, Vec3 pos) throws Exception {
        Class<?> containerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
        Object container = containerClass.getMethod("getContainer", Level.class).invoke(null, level);
        if (container == null) {
            return null;
        }
        ChunkPos chunk = new ChunkPos(BlockPos.containing(pos));
        return container.getClass().getMethod("getPlot", ChunkPos.class).invoke(container, chunk);
    }

    private static LevelChunk chunk(Object plot, BlockPos pos) throws Exception {
        ChunkPos global = new ChunkPos(pos);
        Object local = plot.getClass().getMethod("toLocal", ChunkPos.class).invoke(plot, global);
        Object chunk = plot.getClass().getMethod("getChunk", ChunkPos.class)
                .invoke(plot, local);
        return chunk instanceof LevelChunk levelChunk ? levelChunk : null;
    }
}
