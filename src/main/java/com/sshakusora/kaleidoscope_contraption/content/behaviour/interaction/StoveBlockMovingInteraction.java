package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import com.github.ysbbbbbb.kaleidoscopecookery.advancements.critereon.ModEventTriggerType;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.PotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.SteamerBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.StockpotBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.block.kitchen.StoveBlock;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.PotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.crafting.recipe.StockpotRecipe;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModItems;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModSoupBases;
import com.github.ysbbbbbb.kaleidoscopecookery.init.ModTrigger;
import com.github.ysbbbbbb.kaleidoscopecookery.init.tag.TagMod;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.sshakusora.kaleidoscope_contraption.network.KCContraptionChangedPacket;
import com.sshakusora.kaleidoscope_contraption.network.KCPacketHandler;
import com.sshakusora.kaleidoscope_contraption.network.KCRemoveBlockHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.MutablePair;

import static com.github.ysbbbbbb.kaleidoscopecookery.item.KitchenShovelItem.hasOil;
import static com.github.ysbbbbbb.kaleidoscopecookery.item.KitchenShovelItem.setHasOil;

public class StoveBlockMovingInteraction extends MovingInteractionBehaviour {

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

        if (!KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
            // 处理放置PotBlock
            if (handlePotBlockPlacement(player, activeHand, localPos, contraptionEntity, info, itemInHand)) {
                if (state.getValue(BlockStateProperties.LIT))
                    ModTrigger.EVENT.trigger(player, ModEventTriggerType.PLACE_POT_ON_HEAT_SOURCE);
                return true;
            }


            // 处理放置StockPotBlock
            if (handleStockpotBlockPlacement(player, activeHand, localPos, contraptionEntity, info, itemInHand)
                    && !KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
                if (state.getValue(BlockStateProperties.LIT))
                    ModTrigger.EVENT.trigger(player, ModEventTriggerType.PLACE_STOCKPOT_ON_HEAT_SOURCE);
                return true;
            }

            // 处理放置SteamerBlock
            if (handleSteamerBlockPlacement(player, activeHand, localPos, contraptionEntity, info, itemInHand)
                    && !KCRemoveBlockHandler.isRemoveKeyPressed(player.getUUID())) {
                return true;
            }
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
                    itemInHand.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(activeHand));
                }
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

                itemInHand.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(activeHand));
            }
            return true;
        }

        return false;
    }

    /**
     * 处理手持PotBlock放置到炉灶上方
     */
    private boolean handlePotBlockPlacement(Player player, InteractionHand activeHand, BlockPos localPos,
                                            AbstractContraptionEntity contraptionEntity, StructureTemplate.StructureBlockInfo info,
                                            ItemStack itemInHand) {
        // 获取手持的Block
        Block heldBlock = Block.byItem(itemInHand.getItem());
        if (!(heldBlock instanceof PotBlock potBlock)) {
            return false;
        }

        BlockPos abovePos = localPos.above();

        // 检查正上方是否已经有方块
        StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(abovePos);
        if (aboveInfo != null && !aboveInfo.state().isAir()) {
            // 上方有方块，无法放置
            return false;
        }

        // 在服务端执行放置逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 创建新的PotBlock状态
            BlockState newState = potBlock.defaultBlockState()
                    .setValue(PotBlock.FACING, player.getDirection().getOpposite());

            // 检查炉灶上方是否需要基座（炉灶是完整方块，不需要基座）
            // 但由于PotBlock在Contraption中，我们需要根据实际情况判断
            // 这里设置为不需要基座，因为炉灶是完整方块
            newState = newState.setValue(PotBlock.HAS_BASE, false);

            // 初始化NBT数据 - PotBlockMovementBehaviour需要NBT来执行tick逻辑
            CompoundTag nbt = new CompoundTag();
            nbt.put("Inputs", ContainerHelper.saveAllItems(new CompoundTag(),
                    NonNullList.withSize(PotRecipe.RECIPES_SIZE, ItemStack.EMPTY)));
            nbt.putString("Carrier", Ingredient.EMPTY.toJson().toString());
            nbt.put("Result", ItemStack.EMPTY.serializeNBT());
            nbt.putInt("Status", 0); // PUT_INGREDIENT
            nbt.putInt("CurrentTick", 0);
            nbt.putInt("StirFryCount", 0);
            nbt.putLong("Seed", System.currentTimeMillis());
            // 写入id，防止重进存档时渲染的物品消失
            nbt.putString("id", "kaleidoscope_cookery:pot");

            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    abovePos, newState, nbt);

            // 使用setContraptionBlockData来更新方块数据
            setContraptionBlockData(contraptionEntity, abovePos, newInfo);

            // 注册交互行为到interactors
            MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(newState);
            if (interactionBehaviour != null) {
                contraptionEntity.getContraption().getInteractors().put(abovePos, interactionBehaviour);
            }

            // 注册MovementBehaviour到actors列表，使tick逻辑可以执行
            MovementBehaviour movementBehaviour = MovementBehaviour.REGISTRY.get(newState);
            if (movementBehaviour != null) {
                var actors = contraptionEntity.getContraption().getActors();
                // 检查是否已存在该位置的actor
                boolean exists = false;
                for (var actor : actors) {
                    if (actor.getLeft().pos().equals(abovePos)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    MovementContext context = new MovementContext(
                            contraptionEntity.level(), newInfo, contraptionEntity.getContraption());
                    actors.add(MutablePair.of(newInfo, context));
                }
            }

            // 更新Contraption的bounds - 参考Contraption.addBlock()的实现
            AABB updatedBounds = contraptionEntity.getContraption().bounds.minmax(new AABB(abovePos));
            contraptionEntity.getContraption().bounds = updatedBounds;

            // 通知客户端重新渲染Contraption（同步bounds）
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            abovePos,
                            newInfo.state(),
                            newInfo.nbt(),
                            updatedBounds
                    ),
                    contraptionEntity
            );

            // 消耗玩家手持的一个物品
            if (!player.isCreative()) {
                itemInHand.shrink(1);
            }

            // 播放放置音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(abovePos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos, newState.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        return true;
    }

    /**
     * 处理手持StockpotBlock放置到炉灶上方
     */
    private boolean handleStockpotBlockPlacement(Player player, InteractionHand activeHand, BlockPos localPos,
                                                  AbstractContraptionEntity contraptionEntity, StructureTemplate.StructureBlockInfo info,
                                                  ItemStack itemInHand) {
        // 获取手持的Block
        Block heldBlock = Block.byItem(itemInHand.getItem());
        if (!(heldBlock instanceof StockpotBlock stockpotBlock)) {
            return false;
        }

        BlockPos abovePos = localPos.above();

        // 检查正上方是否已经有方块
        StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(abovePos);
        if (aboveInfo != null && !aboveInfo.state().isAir()) {
            // 上方有方块，无法放置
            return false;
        }

        // 在服务端执行放置逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 创建新的StockpotBlock状态
            BlockState newState = stockpotBlock.defaultBlockState()
                    .setValue(StockpotBlock.FACING, player.getDirection().getOpposite());

            // 检查炉灶上方是否需要基座（炉灶是完整方块，不需要基座）
            newState = newState.setValue(StockpotBlock.HAS_BASE, false);

            // 初始化NBT数据 - StockpotBlockMovementBehaviour需要NBT来执行tick逻辑
            CompoundTag nbt = new CompoundTag();
            nbt.put("Inputs", ContainerHelper.saveAllItems(new CompoundTag(),
                    NonNullList.withSize(StockpotRecipe.RECIPES_SIZE, ItemStack.EMPTY)));
            nbt.putString("RecipeId", "kaleidoscope_cookery:stockpot/empty");
            nbt.putString("SoupBaseId", ModSoupBases.WATER.toString());
            nbt.put("Result", ItemStack.EMPTY.serializeNBT());
            nbt.putInt("Status", 0); // PUT_SOUP_BASE
            nbt.putInt("CurrentTick", -1);
            nbt.putInt("TakeoutCount", 0);
            // 写入id，防止重进存档时渲染的物品消失
            nbt.putString("id", "kaleidoscope_cookery:stockpot");

            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    abovePos, newState, nbt);

            // 使用setContraptionBlockData来更新方块数据
            setContraptionBlockData(contraptionEntity, abovePos, newInfo);

            // 注册交互行为到interactors
            MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(newState);
            if (interactionBehaviour != null) {
                contraptionEntity.getContraption().getInteractors().put(abovePos, interactionBehaviour);
            }

            // 注册MovementBehaviour到actors列表，使tick逻辑可以执行
            MovementBehaviour movementBehaviour = MovementBehaviour.REGISTRY.get(newState);
            if (movementBehaviour != null) {
                var actors = contraptionEntity.getContraption().getActors();
                // 检查是否已存在该位置的actor
                boolean exists = false;
                for (var actor : actors) {
                    if (actor.getLeft().pos().equals(abovePos)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    MovementContext context = new MovementContext(
                            contraptionEntity.level(), newInfo, contraptionEntity.getContraption());
                    actors.add(MutablePair.of(newInfo, context));
                }
            }

            // 更新Contraption的bounds - 参考Contraption.addBlock()的实现
            AABB updatedBounds = contraptionEntity.getContraption().bounds.minmax(new AABB(abovePos));
            contraptionEntity.getContraption().bounds = updatedBounds;

            // 通知客户端重新渲染Contraption（同步bounds）
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            abovePos,
                            newInfo.state(),
                            newInfo.nbt(),
                            updatedBounds
                    ),
                    contraptionEntity
            );

            // 消耗玩家手持的一个物品
            if (!player.isCreative()) {
                itemInHand.shrink(1);
            }

            // 播放放置音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(abovePos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos, newState.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        return true;
    }

    /**
     * 处理手持SteamerBlock放置到炉灶上方
     */
    private boolean handleSteamerBlockPlacement(Player player, InteractionHand activeHand, BlockPos localPos,
                                                 AbstractContraptionEntity contraptionEntity, StructureTemplate.StructureBlockInfo info,
                                                 ItemStack itemInHand) {
        // 获取手持的Block
        Block heldBlock = Block.byItem(itemInHand.getItem());
        if (!(heldBlock instanceof SteamerBlock steamerBlock)) {
            return false;
        }

        BlockPos abovePos = localPos.above();

        // 检查正上方是否已经有方块
        StructureTemplate.StructureBlockInfo aboveInfo = contraptionEntity.getContraption().getBlocks().get(abovePos);
        if (aboveInfo != null && !aboveInfo.state().isAir()) {
            // 上方有方块，无法放置
            return false;
        }

        // 在服务端执行放置逻辑
        if (!contraptionEntity.level().isClientSide) {
            // 创建新的SteamerBlock状态
            BlockState newState = steamerBlock.defaultBlockState()
                    .setValue(SteamerBlock.FACING, player.getDirection().getOpposite())
                    .setValue(SteamerBlock.HALF, true)  // 初始为单层
                    .setValue(SteamerBlock.HAS_LID, false)
                    .setValue(SteamerBlock.HAS_BASE, false)  // 炉灶是完整方块，不需要基座
                    .setValue(SteamerBlock.WATERLOGGED, false);

            // 从手持物品中读取NBT数据（如果有）
            CompoundTag nbt = new CompoundTag();
            CompoundTag handData = net.minecraft.world.item.BlockItem.getBlockEntityData(itemInHand);
            if (handData != null) {
                // 手持物品有数据，读取物品和进度
                NonNullList<ItemStack> handItems = NonNullList.withSize(4, ItemStack.EMPTY);
                ContainerHelper.loadAllItems(handData, handItems);
                int[] handProgress = handData.getIntArray("CookingProgress");
                int[] handTime = handData.getIntArray("CookingTime");

                // 保存到NBT（单层只有0-3槽位）
                NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
                for (int i = 0; i < 4; i++) {
                    items.set(i, handItems.get(i));
                }
                ContainerHelper.saveAllItems(nbt, items, true);

                int[] progress = new int[8];
                int[] time = new int[8];
                if (handProgress.length >= 4) {
                    System.arraycopy(handProgress, 0, progress, 0, 4);
                }
                if (handTime.length >= 4) {
                    System.arraycopy(handTime, 0, time, 0, 4);
                }
                nbt.putIntArray("CookingProgress", progress);
                nbt.putIntArray("CookingTime", time);
            } else {
                // 空手物品，初始化空NBT
                NonNullList<ItemStack> items = NonNullList.withSize(8, ItemStack.EMPTY);
                ContainerHelper.saveAllItems(nbt, items, true);
                nbt.putIntArray("CookingProgress", new int[8]);
                nbt.putIntArray("CookingTime", new int[8]);
            }

            // 写入id，防止重进存档时渲染的物品消失
            nbt.putString("id", "kaleidoscope_cookery:steamer");

            StructureTemplate.StructureBlockInfo newInfo = new StructureTemplate.StructureBlockInfo(
                    abovePos, newState, nbt);

            // 使用setContraptionBlockData来更新方块数据
            setContraptionBlockData(contraptionEntity, abovePos, newInfo);

            // 注册交互行为到interactors
            MovingInteractionBehaviour interactionBehaviour = MovingInteractionBehaviour.REGISTRY.get(newState);
            if (interactionBehaviour != null) {
                contraptionEntity.getContraption().getInteractors().put(abovePos, interactionBehaviour);
            }

            // 注册MovementBehaviour到actors列表，使tick逻辑可以执行
            MovementBehaviour movementBehaviour = MovementBehaviour.REGISTRY.get(newState);
            if (movementBehaviour != null) {
                var actors = contraptionEntity.getContraption().getActors();
                // 检查是否已存在该位置的actor
                boolean exists = false;
                for (var actor : actors) {
                    if (actor.getLeft().pos().equals(abovePos)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    MovementContext context = new MovementContext(
                            contraptionEntity.level(), newInfo, contraptionEntity.getContraption());
                    actors.add(MutablePair.of(newInfo, context));
                }
            }

            // 更新Contraption的bounds - 参考Contraption.addBlock()的实现
            AABB updatedBounds = contraptionEntity.getContraption().bounds.minmax(new AABB(abovePos));
            contraptionEntity.getContraption().bounds = updatedBounds;

            // 通知客户端重新渲染Contraption（同步bounds）
            KCPacketHandler.sendToTracking(
                    new KCContraptionChangedPacket(
                            contraptionEntity.getId(),
                            abovePos,
                            newInfo.state(),
                            newInfo.nbt(),
                            updatedBounds
                    ),
                    contraptionEntity
            );

            // 消耗玩家手持的一个物品
            if (!player.isCreative()) {
                itemInHand.shrink(1);
            }

            // 播放放置音效
            Vec3 globalPos = contraptionEntity.toGlobalVector(Vec3.atCenterOf(abovePos), 1.0f);
            BlockPos soundPos = new BlockPos((int) globalPos.x, (int) globalPos.y, (int) globalPos.z);
            contraptionEntity.level().playSound(null, soundPos, newState.getSoundType().getPlaceSound(),
                    SoundSource.BLOCKS, 1.0F, 0.8F);
        }

        return true;
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
