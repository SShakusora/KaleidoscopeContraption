package com.sshakusora.kaleidoscope_contraption.registry;

import com.github.ysbbbbbb.kaleidoscopetavern.init.ModBlocks;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.DrinkBlockMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.PotionBottleBlockMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.ShakerBlockMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.SignatureCocktailBlockMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.TavernBarCabinetMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.TavernGlasswareHolderMovingInteraction;
import com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction.TavernStorageBlockMovingInteraction;

/** Registrations that require Kaleidoscope Tavern. */
public final class KCTavernInteractionBehaviours {
    private KCTavernInteractionBehaviours() {
    }

    public static void registerDefaults() {
        DrinkBlockMovingInteraction drinkInteraction = new DrinkBlockMovingInteraction();
        registerDrinkBlocks(drinkInteraction);

        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.SIGNATURE_COCKTAIL.get(), new SignatureCocktailBlockMovingInteraction());
        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.SHAKER.get(), new ShakerBlockMovingInteraction());
        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.POTION_BOTTLE.get(), new PotionBottleBlockMovingInteraction());

        TavernStorageBlockMovingInteraction storageInteraction = new TavernStorageBlockMovingInteraction();
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.HOLDER.get(), storageInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.TILTED_RACK.get(), storageInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CIRCULAR_RACK.get(), storageInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CELLAR_CABINET.get(), storageInteraction);

        TavernBarCabinetMovingInteraction cabinetInteraction = new TavernBarCabinetMovingInteraction();
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BAR_CABINET.get(), cabinetInteraction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GLASS_BAR_CABINET.get(), cabinetInteraction);

        MovingInteractionBehaviour.REGISTRY.register(
                ModBlocks.GLASSWARE_HOLDER.get(), new TavernGlasswareHolderMovingInteraction());
    }

    private static void registerDrinkBlocks(DrinkBlockMovingInteraction interaction) {
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CHAMPAGNE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.VODKA.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.BRANDY.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.CARIGNAN.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SAKURA_WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.PLUM_WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.WHISKEY.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.ICE_WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.POLARIS_SWEET_WHITE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.HONEY_WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.RED_QUEEN.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.MINERS_STAR.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.RUM.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.RIESLING_DRY_WHITE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SUNSET_GLOW.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.MADAME_SHEXIANG.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SWEET_BERRY_WINE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SHERRY.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.MOTHER_SNOW.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.LUMINOUS_BRIDE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.GLOWFLOWER_BREW.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.SAUVIGNON_BLANC_DRY_WHITE.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.VINEGAR.get(), interaction);
        MovingInteractionBehaviour.REGISTRY.register(ModBlocks.WATERMELON_JUICE.get(), interaction);
    }
}
