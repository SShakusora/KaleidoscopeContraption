package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.EmptyCupBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.drink.TeacupBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModParticles;
import com.github.ysbbbbbb.kaleidoscopecookery.item.TeacupItem;
import com.github.ysbbbbbb.kaleidoscopecookery.item.TeapotItem;
import com.github.ysbbbbbb.kaleidoscopecookery.util.ItemUtils;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionRemovalManager;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.util.ContraptionInteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

public class TeacupBlockMovingInteraction extends MovingInteractionBehaviour {
    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand hand, BlockPos localPos,
                                           AbstractContraptionEntity entity) {
        if (hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        StructureTemplate.StructureBlockInfo info = entity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }
        if (KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            ContraptionRemovalManager.Result result = ContraptionRemovalManager.tryRemove(player, localPos, entity);
            if (result == ContraptionRemovalManager.Result.NOT_REGISTERED) {
                return removeUnregistered(player, localPos, entity, info);
            }
            return result == ContraptionRemovalManager.Result.REMOVED;
        }
        if (info.state().getBlock() instanceof EmptyCupBlock) {
            return interactEmptyCups(player, localPos, entity, info);
        }
        if (info.state().getBlock() instanceof TeacupBlock teacup) {
            return interactTeaCups(player, localPos, entity, info, teacup);
        }
        return false;
    }

    private boolean interactEmptyCups(Player player, BlockPos pos, AbstractContraptionEntity entity,
                                      StructureTemplate.StructureBlockInfo info) {
        BlockState state = info.state();
        ItemStack held = player.getMainHandItem();

        if (held.is(ModItems.TEAPOT.get())) {
            ItemStack poured = TeapotItem.getPourOut(held, entity.level());
            if (!(poured.getItem() instanceof TeacupItem item)
                    || !(item.getBlock() instanceof TeacupBlock teacup)) {
                return true;
            }
            int cups = state.getValue(EmptyCupBlock.CUP_COUNT);
            if (!entity.level().isClientSide) {
                if (cups > teacup.getMaxCount()) {
                    ItemUtils.getItemToLivingEntity(player,
                            new ItemStack(ModItems.EMPTY_CUP.get(), cups - teacup.getMaxCount()));
                }
                BlockState newState = teacup.defaultBlockState()
                        .setValue(teacup.getCupCountProperty(), Math.min(cups, teacup.getMaxCount()))
                        .setValue(teacup.getTeaCountProperty(), 1)
                        .setValue(TeacupBlock.FACING, state.getValue(EmptyCupBlock.FACING));
                update(entity, pos, info, newState);
                TeapotItem.pourOut(held, entity.level());
                play(entity, pos, SoundEvents.BREWING_STAND_BREW);
                spawnPourParticles(entity, pos);
            }
            return true;
        }

        if (held.is(ModItems.EMPTY_CUP.get())) {
            int cups = state.getValue(EmptyCupBlock.CUP_COUNT);
            if (cups >= EmptyCupBlock.MAX_COUNT) {
                return true;
            }
            if (!entity.level().isClientSide) {
                update(entity, pos, info, state.setValue(EmptyCupBlock.CUP_COUNT, cups + 1));
                consume(player, held);
                play(entity, pos, state.getSoundType().getPlaceSound());
            }
            return true;
        }

        if (held.isEmpty()) {
            int cups = state.getValue(EmptyCupBlock.CUP_COUNT);
            if (!entity.level().isClientSide) {
                ItemUtils.getItemToLivingEntity(player, new ItemStack(ModItems.EMPTY_CUP.get()));
                if (cups == 1) {
                    remove(entity, pos);
                } else {
                    update(entity, pos, info, state.setValue(EmptyCupBlock.CUP_COUNT, cups - 1));
                }
                play(entity, pos, state.getSoundType().getBreakSound());
            }
            return true;
        }
        return false;
    }

    private boolean interactTeaCups(Player player, BlockPos pos, AbstractContraptionEntity entity,
                                    StructureTemplate.StructureBlockInfo info, TeacupBlock teacup) {
        BlockState state = info.state();
        ItemStack held = player.getMainHandItem();
        int cups = state.getValue(teacup.getCupCountProperty());
        int tea = state.getValue(teacup.getTeaCountProperty());

        if (held.is(ModItems.TEAPOT.get())) {
            ItemStack poured = TeapotItem.getPourOut(held, entity.level());
            if (poured.isEmpty() || poured.getItem() != state.getBlock().asItem()) {
                return true;
            }
            if (tea < cups && !entity.level().isClientSide) {
                update(entity, pos, info, state.setValue(teacup.getTeaCountProperty(), tea + 1));
                TeapotItem.pourOut(held, entity.level());
                play(entity, pos, SoundEvents.BREWING_STAND_BREW);
                spawnPourParticles(entity, pos);
            }
            return true;
        }

        if (held.is(ModItems.EMPTY_CUP.get())) {
            if (cups < teacup.getMaxCount() && !entity.level().isClientSide) {
                update(entity, pos, info, state.setValue(teacup.getCupCountProperty(), cups + 1));
                consume(player, held);
                play(entity, pos, state.getSoundType().getPlaceSound());
            }
            return true;
        }

        if (held.getItem() instanceof TeacupItem item) {
            if (item.getBlock() != state.getBlock()) {
                return true;
            }
            if (cups < teacup.getMaxCount() && !entity.level().isClientSide) {
                update(entity, pos, info, state
                        .setValue(teacup.getCupCountProperty(), cups + 1)
                        .setValue(teacup.getTeaCountProperty(), tea + 1));
                consume(player, held);
                play(entity, pos, state.getSoundType().getPlaceSound());
            }
            return true;
        }

        if (held.isEmpty()) {
            if (!entity.level().isClientSide) {
                if (cups > tea) {
                    ItemUtils.getItemToLivingEntity(player, new ItemStack(ModItems.EMPTY_CUP.get()));
                    if (cups == 1) {
                        remove(entity, pos);
                    } else {
                        update(entity, pos, info, state.setValue(teacup.getCupCountProperty(), cups - 1));
                    }
                } else {
                    ItemUtils.getItemToLivingEntity(player, new ItemStack(state.getBlock().asItem()));
                    if (cups == 1) {
                        remove(entity, pos);
                    } else {
                        update(entity, pos, info, state
                                .setValue(teacup.getTeaCountProperty(), tea - 1)
                                .setValue(teacup.getCupCountProperty(), cups - 1));
                    }
                }
                play(entity, pos, state.getSoundType().getBreakSound());
            }
            return true;
        }
        return false;
    }

    private void update(AbstractContraptionEntity entity, BlockPos pos,
                        StructureTemplate.StructureBlockInfo oldInfo, BlockState state) {
        ContraptionInteractionUtil.updateContraptionData(entity, pos,
                new StructureTemplate.StructureBlockInfo(pos, state, oldInfo.nbt()));
    }

    private void remove(AbstractContraptionEntity entity, BlockPos pos) {
        ContraptionInteractionUtil.removeBlockFromContraption(entity, pos);
        var bounds = ContraptionInteractionUtil.recalculateBounds(entity);
        entity.getContraption().invalidateColliders();
        ContraptionInteractionUtil.syncBlockRemoval(entity, pos, bounds);
    }

    private boolean removeUnregistered(Player player, BlockPos pos, AbstractContraptionEntity entity,
                                       StructureTemplate.StructureBlockInfo info) {
        if (!entity.level().isClientSide) {
            BlockState state = info.state();
            if (!player.isCreative()) {
                if (state.getBlock() instanceof EmptyCupBlock) {
                    ItemUtils.getItemToLivingEntity(player,
                            new ItemStack(ModItems.EMPTY_CUP.get(), state.getValue(EmptyCupBlock.CUP_COUNT)));
                } else if (state.getBlock() instanceof TeacupBlock teacup) {
                    int cups = state.getValue(teacup.getCupCountProperty());
                    int tea = state.getValue(teacup.getTeaCountProperty());
                    if (cups > tea) {
                        ItemUtils.getItemToLivingEntity(player,
                                new ItemStack(ModItems.EMPTY_CUP.get(), cups - tea));
                    }
                    if (tea > 0) {
                        ItemUtils.getItemToLivingEntity(player,
                                new ItemStack(state.getBlock().asItem(), tea));
                    }
                } else {
                    return false;
                }
            }
            remove(entity, pos);
            play(entity, pos, state.getSoundType().getBreakSound());
        }
        return true;
    }

    private void consume(Player player, ItemStack stack) {
        if (!player.isCreative()) {
            stack.shrink(1);
        }
    }

    private void play(AbstractContraptionEntity entity, BlockPos pos,
                      SoundEvent sound) {
        ContraptionInteractionUtil.playSound(entity, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void spawnPourParticles(AbstractContraptionEntity entity, BlockPos pos) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 global = ContraptionInteractionUtil.getGlobalPos(entity, pos);
        serverLevel.sendParticles(ModParticles.COOKING.get(), global.x, global.y - 0.15, global.z,
                4, 0.14, 0.08, 0.14, 0.02);
    }
}
