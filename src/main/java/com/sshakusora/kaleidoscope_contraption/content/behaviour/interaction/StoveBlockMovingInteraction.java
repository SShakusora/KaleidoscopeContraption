package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.advancements.critereon.ModEventTriggerType;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.StoveBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.api.placement.ContraptionPlacementManager;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import com.sshakusora.kaleidoscope_contraption.registry.KCContraptionPlacements;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;

import static com.github.ysbbbbbb.kaleidoscopecookery.item.KitchenShovelItem.hasOil;
import static com.github.ysbbbbbb.kaleidoscopecookery.item.KitchenShovelItem.setHasOil;

public class StoveBlockMovingInteraction extends SyncedMovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return false;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof StoveBlock)) {
            return false;
        }

        ItemStack itemInHand = player.getItemInHand(activeHand);

        if (!KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())
                && ContraptionPlacementManager.tryPlace(KCContraptionPlacements.STOVE_TOP,
                player, activeHand, localPos, contraptionEntity)) {
            return true;
        }

        // 点燃炉灶
        if (!state.getValue(BlockStateProperties.LIT) && itemInHand.is(TagMod.LIT_STOVE)) {
            // 在服务端更新方块状态
            if (!contraptionEntity.level().isClientSide) {
                BlockState newState = state.setValue(BlockStateProperties.LIT, true);
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(info.pos(), newState, info.nbt());
                setContraptionBlockData(contraptionEntity, localPos, newInfo);

                // 查找并更新actor数据
                var actors = contraptionEntity.getContraption().getActors();
                for (int i = 0; i < actors.size(); i++) {
                    MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor = actors.get(i);
                    if (actor.getLeft().pos().equals(localPos)) {
                        setContraptionActorData(contraptionEntity, i, newInfo, actor.getRight());
                        break;
                    }
                }

                // 播放音效
                Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
                BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);

                if (itemInHand.is(Items.FIRE_CHARGE)) {
                    contraptionEntity.level().playSound(null, soundPos,
                            SoundEvents.FIRECHARGE_USE,
                            SoundSource.BLOCKS, 1.0F,
                            contraptionEntity.level().getRandom().nextFloat() * 0.4F + 0.8F);
                    itemInHand.shrink(1);
                } else {
                    contraptionEntity.level().playSound(null, soundPos,
                            SoundEvents.FLINTANDSTEEL_USE,
                            SoundSource.BLOCKS, 1.0F,
                            contraptionEntity.level().getRandom().nextFloat() * 0.4F + 0.8F);
                    itemInHand.hurtAndBreak(1, player, activeHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                }
                ModTrigger.EVENT.get().trigger(player, ModEventTriggerType.LIT_THE_STOVE);
            }
            return true;
        }

        // 熄灭炉灶
        if (state.getValue(BlockStateProperties.LIT) && itemInHand.is(TagMod.EXTINGUISH_STOVE)) {
            // 在服务端更新方块状态
            if (!contraptionEntity.level().isClientSide) {
                // 处理锅铲的特殊逻辑：如果锅铲有油，清除油状态
                if (itemInHand.is(ModItems.KITCHEN_SHOVEL.get()) && hasOil(itemInHand)) {
                    setHasOil(itemInHand, false);
                }

                BlockState newState = state.setValue(BlockStateProperties.LIT, false);
                StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(info.pos(), newState, info.nbt());
                setContraptionBlockData(contraptionEntity, localPos, newInfo);

                // 查找并更新actor数据
                var actors = contraptionEntity.getContraption().getActors();
                for (int i = 0; i < actors.size(); i++) {
                    MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor = actors.get(i);
                    if (actor.getLeft().pos().equals(localPos)) {
                        setContraptionActorData(contraptionEntity, i, newInfo, actor.getRight());
                        break;
                    }
                }

                // 播放音效
                Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(localPos), 1.0f);
                BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);

                contraptionEntity.level().playSound(null, soundPos,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS, 0.5F,
                        2.6F + (contraptionEntity.level().random.nextFloat() - contraptionEntity.level().random.nextFloat()) * 0.8F);

                itemInHand.hurtAndBreak(1, player, activeHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            return true;
        }

        return false;
    }

    @Override
    public void handleEntityCollision(Entity entity, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
        //TODO 由于ContraptionCollider和AbstractContraptionEntity的canCollideWith方法的限制，此方法无法捕捉到Projectile弹射物
        if (!(entity instanceof Projectile projectile)) {
            return;
        }

        StructureTemplate.StructureBlockInfo info = contraptionEntity.getContraption().getBlocks().get(localPos);
        if (info == null) {
            return;
        }

        BlockState state = info.state();
        if (!(state.getBlock() instanceof StoveBlock)) {
            return;
        }

        // 检查弹射物是否着火、是否可以交互、以及炉灶是否未点燃
        if (!contraptionEntity.level().isClientSide
                && projectile.isOnFire()
                && projectile.mayInteract(contraptionEntity.level(), localPos)
                && !state.getValue(BlockStateProperties.LIT)) {

            BlockState newState = state.setValue(BlockStateProperties.LIT, true);
            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(info.pos(), newState, info.nbt());
            setContraptionBlockData(contraptionEntity, localPos, newInfo);
            if (projectile.getOwner() instanceof Player player) {
                ModTrigger.EVENT.get().trigger(player, ModEventTriggerType.LIT_THE_STOVE);
            }

            // 查找并更新actor数据
            var actors = contraptionEntity.getContraption().getActors();
            for (int i = 0; i < actors.size(); i++) {
                MutablePair<StructureTemplate.StructureBlockInfo, MovementContext> actor = actors.get(i);
                if (actor.getLeft().pos().equals(localPos)) {
                    setContraptionActorData(contraptionEntity, i, newInfo, actor.getRight());
                    break;
                }
            }
        }
    }
}
