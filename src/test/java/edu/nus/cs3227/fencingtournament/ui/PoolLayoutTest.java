package edu.nus.cs3227.fencingtournament.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolLayoutTest {
    @Test
    void liveViewportDeterminesHowManyPoolsFitAcross() {
        PoolLayout.BoardLayout twoFive = PoolLayout.calculate(List.of(5, 5), 1_193, PoolLayout.ORGANISER);
        PoolLayout.BoardLayout twoSeven = PoolLayout.calculate(List.of(7, 7), 1_193, PoolLayout.ORGANISER);
        PoolLayout.BoardLayout twoEight = PoolLayout.calculate(List.of(8, 8), 1_193, PoolLayout.ORGANISER);
        PoolLayout.BoardLayout threeFive = PoolLayout.calculate(List.of(5, 5, 5), 1_193, PoolLayout.ORGANISER);
        PoolLayout.BoardLayout sixFive = PoolLayout.calculate(List.of(5, 5, 5, 5, 5, 5), 1_193, PoolLayout.ORGANISER);

        assertEquals(2, twoFive.columns());
        assertEquals(2, twoSeven.columns());
        assertEquals(2, twoEight.columns());
        assertEquals(3, threeFive.columns());
        assertEquals(3, sixFive.columns());
        assertTrue(sixFive.panelWidth() >= sixFive.largestMinimumPanelWidth());
    }

    @Test
    void narrowerViewportsWrapBeforeViolatingReadablePoolWidths() {
        assertEquals(2, PoolLayout.calculate(List.of(5, 5, 5), 1_000, PoolLayout.ORGANISER).columns());
        assertEquals(1, PoolLayout.calculate(List.of(5, 5), 700, PoolLayout.ORGANISER).columns());
        assertEquals(3, PoolLayout.calculate(java.util.Collections.nCopies(30, 5), 1_193,
                PoolLayout.ORGANISER).columns());
    }
}
