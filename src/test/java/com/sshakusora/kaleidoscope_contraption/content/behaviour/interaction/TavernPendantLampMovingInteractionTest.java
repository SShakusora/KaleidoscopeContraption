package com.sshakusora.kaleidoscope_contraption.content.behaviour.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.junit.Test;

import static org.junit.Assert.*;

public class TavernPendantLampMovingInteractionTest {
    private static final BlockPos ORIGIN = new BlockPos(3, 7, -2);

    @Test
    public void upperHalfFindsLowerHalfBelow() {
        assertEquals(ORIGIN.below(), TavernPendantLampMovingInteraction.getOtherHalfPos(
                ORIGIN, DoubleBlockHalf.UPPER));
    }

    @Test
    public void lowerHalfFindsUpperHalfAbove() {
        assertEquals(ORIGIN.above(), TavernPendantLampMovingInteraction.getOtherHalfPos(
                ORIGIN, DoubleBlockHalf.LOWER));
    }

    @Test
    public void onlyLowerHalfDropsWhenPairIsBroken() {
        assertFalse(TavernPendantLampMovingInteraction.shouldReturnItem(
                DoubleBlockHalf.UPPER, false));
        assertTrue(TavernPendantLampMovingInteraction.shouldReturnItem(
                DoubleBlockHalf.LOWER, false));
        assertTrue(TavernPendantLampMovingInteraction.shouldReturnItem(
                DoubleBlockHalf.UPPER, true));
    }
}
