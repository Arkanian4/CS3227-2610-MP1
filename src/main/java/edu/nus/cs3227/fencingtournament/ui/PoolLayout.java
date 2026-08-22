package edu.nus.cs3227.fencingtournament.ui;

import java.util.List;

/**
 * Calculates readable pool-board geometry from the live viewport and pool content.
 * Presentation-specific callers supply their readability bounds instead of branching on pool counts.
 */
final class PoolLayout {
    static final Parameters ORGANISER = new Parameters(118, 132, 46, 52, 30, 34, 12, 18, 4);

    private PoolLayout() { }

    static BoardLayout calculate(List<Integer> fencerCounts, double viewportWidth, Parameters parameters) {
        double largestMinimumPanelWidth = fencerCounts.stream()
                .mapToDouble(count -> minimumPanelWidth(count, parameters))
                .max()
                .orElse(parameters.panelHorizontalPadding());
        double usableWidth = viewportWidth > 0
                ? Math.max(1, viewportWidth - parameters.dashboardHorizontalPadding())
                : largestMinimumPanelWidth;
        int columns = columnCount(fencerCounts.size(), usableWidth, largestMinimumPanelWidth, parameters.horizontalGap());
        double panelWidth = Math.floor((usableWidth - parameters.horizontalGap() * (columns - 1)) / columns);
        return new BoardLayout(usableWidth, largestMinimumPanelWidth, columns, panelWidth);
    }

    static int columnCount(int poolCount, double usableWidth, double poolRequiredWidth, double gap) {
        if (poolCount <= 0) return 1;
        int fittingColumns = (int) Math.floor((usableWidth + gap) / (poolRequiredWidth + gap));
        return Math.max(1, Math.min(poolCount, fittingColumns));
    }

    static double minimumPanelWidth(int fencerCount, Parameters parameters) {
        return parameters.minimumNameColumnWidth() + parameters.minimumScoreCellWidth() * fencerCount
                + parameters.panelHorizontalPadding();
    }

    static MatrixMetrics matrixMetrics(int fencerCount, double panelWidth, Parameters parameters) {
        double minimumMatrixWidth = minimumPanelWidth(fencerCount, parameters) - parameters.panelHorizontalPadding();
        double comfortableMatrixWidth = parameters.comfortableNameColumnWidth()
                + parameters.comfortableScoreCellWidth() * fencerCount;
        double availableMatrixWidth = Math.max(minimumMatrixWidth,
                Math.min(comfortableMatrixWidth, panelWidth - parameters.panelHorizontalPadding()));
        double range = comfortableMatrixWidth - minimumMatrixWidth;
        double comfort = range == 0 ? 1 : (availableMatrixWidth - minimumMatrixWidth) / range;
        return new MatrixMetrics(
                interpolate(parameters.minimumNameColumnWidth(), parameters.comfortableNameColumnWidth(), comfort),
                interpolate(parameters.minimumScoreCellWidth(), parameters.comfortableScoreCellWidth(), comfort),
                interpolate(parameters.minimumRowHeight(), parameters.comfortableRowHeight(), comfort));
    }

    private static double interpolate(double minimum, double comfortable, double proportion) {
        return minimum + (comfortable - minimum) * proportion;
    }

    record Parameters(double minimumNameColumnWidth, double comfortableNameColumnWidth,
                      double minimumScoreCellWidth, double comfortableScoreCellWidth,
                      double minimumRowHeight, double comfortableRowHeight,
                      double panelHorizontalPadding, double horizontalGap,
                      double dashboardHorizontalPadding) { }

    record BoardLayout(double usableWidth, double largestMinimumPanelWidth, int columns, double panelWidth) { }

    record MatrixMetrics(double nameWidth, double scoreWidth, double rowHeight) { }
}
