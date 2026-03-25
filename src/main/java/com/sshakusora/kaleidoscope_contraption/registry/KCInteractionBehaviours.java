package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopecookery.init.ModBlocks;
import com.github.ysbbbbbb.kaleidoscopecookery.init.registry.FoodBiteRegistry;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.FoodBiteBlockMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.PotBlockMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.StoveBlockMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.TableBlockMovingInteraction;

public class KCInteractionBehaviours {
    public static void registerDefaults() {
        // 注册食物方块的交互行为
        FoodBiteBlockMovingInteraction foodInteraction = new FoodBiteBlockMovingInteraction();
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.DARK_CUISINE), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SUSPICIOUS_STIR_FRY), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SLIME_BALL_MEAL), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FONDANT_PIE), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.DONGPO_PORK), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FONDANT_SPIDER_EYE), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.CHORUS_FRIED_EGG), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.BRAISED_FISH), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.GOLDEN_SALAD), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SPICY_CHICKEN), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.YAKITORI), foodInteraction);
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
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SHENGJIAN_MANTOU), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.CANDIED_POTATO), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.DOUGH_DROP_SOUP), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.STUFFED_TIGER_SKIN_PEPPER), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SPICY_RABBIT_HEAD), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FOUR_JOY_MEATBALL_SOUP), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.NUMBING_SPICY_CHICKEN), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FRIED_CATERPILLAR), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FRIED_SPRING_ROLL), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.SPICY_BLOOD_STEW), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.FRUIT_PLATTER), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.BRAISED_PORK_RIBS), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.COLD_ROASTED_MEAT), foodInteraction);
        MovingInteractionBehaviour.REGISTRY.register(FoodBiteRegistry.getBlock(FoodBiteRegistry.OIL_SPLASHED_FISH), foodInteraction);
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
    }
}
