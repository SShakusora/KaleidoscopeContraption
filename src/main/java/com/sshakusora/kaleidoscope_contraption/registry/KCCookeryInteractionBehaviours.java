package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.TeacupRegistry;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.actors.seat.SeatInteractionBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.*;

public class KCCookeryInteractionBehaviours {
    public static void registerDefaults() {
        // 注册食物方块的交互行为
        FoodBiteBlockMovingInteraction foodInteraction = new FoodBiteBlockMovingInteraction();
        FoodBiteOneByTwoBlockMovingInteraction oneByTwoInteraction = new FoodBiteOneByTwoBlockMovingInteraction();
        FoodBiteThreeByThreeBlockMovingInteraction threeByThreeInteraction = new FoodBiteThreeByThreeBlockMovingInteraction();

        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.DARK_CUISINE), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SUSPICIOUS_STIR_FRY), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SLIME_BALL_MEAL), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FONDANT_PIE), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.DONGPO_PORK), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FONDANT_SPIDER_EYE), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.CHORUS_FRIED_EGG), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.GOLDEN_SALAD), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SPICY_CHICKEN), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.PAN_SEARED_KNIGHT_STEAK), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.STARGAZY_PIE), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SWEET_AND_SOUR_ENDER_PEARLS), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.CRYSTAL_LAMB_CHOP), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.BLAZE_LAMB_CHOP), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FROST_LAMB_CHOP), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.NETHER_STYLE_SASHIMI), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.END_STYLE_SASHIMI), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.DESERT_STYLE_SASHIMI), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.TUNDRA_STYLE_SASHIMI), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.COLD_STYLE_SASHIMI), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.CANDIED_POTATO), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.DOUGH_DROP_SOUP), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.STUFFED_TIGER_SKIN_PEPPER), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SPICY_RABBIT_HEAD), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FOUR_JOY_MEATBALL_SOUP), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.NUMBING_SPICY_CHICKEN), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FRIED_CATERPILLAR), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FRIED_SPRING_ROLL), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SPICY_BLOOD_STEW), foodInteraction);
        // 1x2 食物方块使用专门的交互行为
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.BRAISED_PORK_RIBS), oneByTwoInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.COLD_ROASTED_MEAT), oneByTwoInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.OIL_SPLASHED_FISH), oneByTwoInteraction);

        // 3x3 食物方块使用专门的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COLD_CUT_HAM_SLICES.get(), threeByThreeInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.BROWN_MUSHROOM_POT_SOUP), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.RED_MUSHROOM_POT_SOUP), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.WARPED_FUNGUS_POT_SOUP), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.CRIMSON_FUNGUS_POT_SOUP), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.BUDDHA_JUMPS_OVER_THE_WALL), foodInteraction);

        // 注册桌子方块的交互行为
        TableBlockMovingInteraction tableInteraction = new TableBlockMovingInteraction();
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TABLE_OAK.get(), tableInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TABLE_SPRUCE.get(), tableInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TABLE_ACACIA.get(), tableInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TABLE_BAMBOO.get(), tableInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TABLE_BIRCH.get(), tableInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TABLE_CHERRY.get(), tableInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TABLE_CRIMSON.get(), tableInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TABLE_DARK_OAK.get(), tableInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TABLE_JUNGLE.get(), tableInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TABLE_MANGROVE.get(), tableInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TABLE_WARPED.get(), tableInteraction);

        // 注册炉灶的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.STOVE.get(), new StoveBlockMovingInteraction());

        // 注册炒锅的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.POT.get(), new PotBlockMovingInteraction());

        // 注册汤锅的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.STOCKPOT.get(), new StockpotBlockInteraction());

        // 注册沙威玛烤架的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SHAWARMA_SPIT.get(), new ShawarmaSpitBlockMovingInteraction());

        // 注册切菜板的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHOPPING_BOARD.get(), new ChoppingBoardBlockMovingInteraction());

        // 注册蒸笼的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.STEAMER.get(), new SteamerBlockMovingInteraction());

        // 注册厨具架的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.KITCHENWARE_RACKS.get(), new KitchenwareRacksBlockMovingInteraction());

        // 注册果篮的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.FRUIT_BASKET.get(), new FruitBasketBlockMovingInteraction());

        // 注册搪瓷盆子的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.ENAMEL_BASIN.get(), new EnamelBasinBlockMovingInteraction());

        // 注册油壶的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.OIL_POT.get(), new OilPotBlockMovingInteraction());

        // 注册 1.4.1 新增互动方块
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BAMBOO_TUBE_RICE.get(), new StackableFoodBlockMovingInteraction());
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TEAPOT.get(), new TeapotBlockMovingInteraction());
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.MILLSTONE.get(), new MillstoneBlockMovingInteraction());
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TRASH_CAN.get(), new TrashCanBlockMovingInteraction());

        // 注册森罗物语的成品饮品方块交互行为
        TeacupBlockMovingInteraction teacupInteraction = new TeacupBlockMovingInteraction();
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.EMPTY_CUP.get(), teacupInteraction);
        TeacupRegistry.TEACUP_DATA_MAP.keySet().forEach(id -> {
            var block = TeacupRegistry.getBlock(id);
            if (block != null) {
                MovingInteractionBehaviour.REGISTRY.register(block, teacupInteraction);
            }
        });

        // 注册椅子的交互行为
        SeatInteractionBehaviour seatInteractionBehaviour = new SeatInteractionBehaviour();
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAIR_OAK.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAIR_SPRUCE.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAIR_ACACIA.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAIR_BAMBOO.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAIR_BIRCH.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAIR_CHERRY.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAIR_CRIMSON.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAIR_DARK_OAK.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAIR_JUNGLE.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAIR_MANGROVE.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAIR_WARPED.get(), seatInteractionBehaviour);

        // 注册厨娘凳的交互行为
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_OAK.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_SPRUCE.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_ACACIA.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_BAMBOO.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_BIRCH.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_CHERRY.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_CRIMSON.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_DARK_OAK.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_JUNGLE.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_MANGROVE.get(), seatInteractionBehaviour);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.COOK_STOOL_WARPED.get(), seatInteractionBehaviour);
    }
}
