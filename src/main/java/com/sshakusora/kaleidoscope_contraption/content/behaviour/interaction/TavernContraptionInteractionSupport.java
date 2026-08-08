package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.CellarCabinetBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.CircularRackBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.HolderBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.TiltedRackBlock;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

final class TavernContraptionInteractionSupport {
    private static final double EPSILON = 1.0E-4;

    private TavernContraptionInteractionSupport() {
    }

    static Optional<Hit> findHit(Player player, BlockPos localPos,
                                 AbstractContraptionEntity contraptionEntity) {
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 endPosition = eyePosition.add(player.getViewVector(1.0F).scale(player.getBlockReach()));
        Vec3 localEyePosition = contraptionEntity.toLocalVector(eyePosition, 1.0F);
        Vec3 localEndPosition = contraptionEntity.toLocalVector(endPosition, 1.0F);

        Optional<Vec3> intersection = new AABB(localPos).clip(localEyePosition, localEndPosition);
        return intersection.map(point -> new Hit(point, getHitFace(point, localPos)));
    }

    static int getStorageSlot(BlockState state, BlockPos localPos, Hit hit) {
        Block block = state.getBlock();
        if (block instanceof HolderBlock) {
            return 0;
        }

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (block instanceof TiltedRackBlock) {
            double localX = getLocalX(facing, localPos, hit.point());
            if (localX < 1.0 / 3.0) {
                return 0;
            }
            if (localX < 2.0 / 3.0) {
                return 1;
            }
            return 2;
        }

        if (block instanceof CircularRackBlock) {
            double localX = getLocalX(facing, localPos, hit.point());
            double localZ = getLocalZ(facing, localPos, hit.point());
            double angle = Math.toDegrees(Math.atan2(localZ - 0.5, localX - 0.5));
            angle = (angle + 360.0) % 360.0;
            if (angle > 300.0) {
                return 5;
            } else if (angle > 240.0) {
                return 0;
            } else if (angle > 180.0) {
                return 1;
            } else if (angle > 120.0) {
                return 2;
            } else if (angle > 60.0) {
                return 3;
            }
            return 4;
        }

        if (block instanceof CellarCabinetBlock) {
            if (hit.face() != facing) {
                return -1;
            }
            double localX = getLocalX(facing, localPos, hit.point());
            double relativeY = hit.point().y - localPos.getY();
            int column = Math.min(2, Math.max(0, (int) (localX * 3.0)));
            int rowFromTop = Math.min(2, Math.max(0, (int) (relativeY * 3.0)));
            return column + (2 - rowFromTop) * 3;
        }

        return -1;
    }

    static int getGlasswareSlot(BlockPos localPos, Hit hit) {
        double localX = hit.point().x - localPos.getX();
        double localZ = hit.point().z - localPos.getZ();
        if (localX > 0.5) {
            return localZ > 0.5 ? 3 : 1;
        }
        return localZ > 0.5 ? 2 : 0;
    }

    static boolean isLeftSide(Direction facing, BlockPos localPos, Vec3 hitPoint) {
        double relativeX = hitPoint.x - localPos.getX();
        double relativeZ = hitPoint.z - localPos.getZ();
        return switch (facing) {
            case NORTH -> relativeX > 0.5;
            case SOUTH -> relativeX < 0.5;
            case EAST -> relativeZ < 0.5;
            case WEST -> relativeZ > 0.5;
            default -> false;
        };
    }

    static void removeBlock(AbstractContraptionEntity contraptionEntity, BlockPos localPos, BlockState state) {
        removeBlock(contraptionEntity, localPos, state.getSoundType().getBreakSound(), 0.8F);
    }

    static void removeBlock(AbstractContraptionEntity contraptionEntity, BlockPos localPos, SoundEvent sound) {
        removeBlock(contraptionEntity, localPos, sound, 1.0F);
    }

    private static void removeBlock(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                                    SoundEvent sound, float pitch) {
        ContraptionInteractionUtil.removeBlockFromContraption(contraptionEntity, localPos);
        var updatedBounds = ContraptionInteractionUtil.recalculateBounds(contraptionEntity);
        contraptionEntity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(contraptionEntity, localPos, updatedBounds);
        ContraptionInteractionUtil.playSound(contraptionEntity, localPos, sound,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, pitch);
    }

    static void playSound(AbstractContraptionEntity contraptionEntity, BlockPos localPos,
                          net.minecraft.sounds.SoundEvent sound) {
        ContraptionInteractionUtil.playSound(contraptionEntity, localPos, sound,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static double getLocalX(Direction direction, BlockPos pos, Vec3 hitPoint) {
        double relativeX = hitPoint.x - pos.getX();
        double relativeZ = hitPoint.z - pos.getZ();
        return switch (direction) {
            case NORTH -> 1.0 - relativeX;
            case SOUTH -> relativeX;
            case EAST -> 1.0 - relativeZ;
            case WEST -> relativeZ;
            default -> 0.5;
        };
    }

    private static double getLocalZ(Direction direction, BlockPos pos, Vec3 hitPoint) {
        double relativeX = hitPoint.x - pos.getX();
        double relativeZ = hitPoint.z - pos.getZ();
        return switch (direction) {
            case NORTH -> relativeZ;
            case SOUTH -> 1.0 - relativeZ;
            case EAST -> 1.0 - relativeX;
            case WEST -> relativeX;
            default -> 0.5;
        };
    }

    private static Direction getHitFace(Vec3 point, BlockPos pos) {
        double relativeX = point.x - pos.getX();
        double relativeY = point.y - pos.getY();
        double relativeZ = point.z - pos.getZ();
        if (relativeX <= EPSILON) {
            return Direction.WEST;
        }
        if (relativeX >= 1.0 - EPSILON) {
            return Direction.EAST;
        }
        if (relativeY <= EPSILON) {
            return Direction.DOWN;
        }
        if (relativeY >= 1.0 - EPSILON) {
            return Direction.UP;
        }
        if (relativeZ <= EPSILON) {
            return Direction.NORTH;
        }
        if (relativeZ >= 1.0 - EPSILON) {
            return Direction.SOUTH;
        }

        double xDistance = Math.min(relativeX, 1.0 - relativeX);
        double yDistance = Math.min(relativeY, 1.0 - relativeY);
        double zDistance = Math.min(relativeZ, 1.0 - relativeZ);
        if (xDistance <= yDistance && xDistance <= zDistance) {
            return relativeX < 0.5 ? Direction.WEST : Direction.EAST;
        }
        if (yDistance <= zDistance) {
            return relativeY < 0.5 ? Direction.DOWN : Direction.UP;
        }
        return relativeZ < 0.5 ? Direction.NORTH : Direction.SOUTH;
    }

    static final class Hit {
        private final Vec3 point;
        private final Direction face;

        private Hit(Vec3 point, Direction face) {
            this.point = point;
            this.face = face;
        }

        Vec3 point() {
            return point;
        }

        Direction face() {
            return face;
        }
    }
}
