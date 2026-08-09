package com.sshakusora.kaleidoscope_contraption.content.behaviour.placement;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BottleBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.PotionBottleBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.IncenseBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.PendantLampBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.StringLightsBlock;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TavernSurfacePlacementRuleTest {
    @Test
    public void potionBottleIsNotMisclassifiedAsOrdinaryBottle() {
        assertEquals(TavernSurfacePlacementRule.PlacementKind.POTION_BOTTLE,
                TavernSurfacePlacementRule.getDirectBlockPlacementKind(PotionBottleBlock.class));
        assertEquals(TavernSurfacePlacementRule.PlacementKind.BOTTLE,
                TavernSurfacePlacementRule.getDirectBlockPlacementKind(BottleBlock.class));
    }

    @Test
    public void tableTopDecorationsAreClassified() {
        assertEquals(TavernSurfacePlacementRule.PlacementKind.INCENSE,
                TavernSurfacePlacementRule.getDirectBlockPlacementKind(IncenseBlock.class));
        assertEquals(TavernSurfacePlacementRule.PlacementKind.STRING_LIGHTS,
                TavernSurfacePlacementRule.getDirectBlockPlacementKind(StringLightsBlock.class));
    }

    @Test
    public void pendantLampRemainsCeilingOnly() {
        assertNull(TavernSurfacePlacementRule.getDirectBlockPlacementKind(PendantLampBlock.class));
    }
}
