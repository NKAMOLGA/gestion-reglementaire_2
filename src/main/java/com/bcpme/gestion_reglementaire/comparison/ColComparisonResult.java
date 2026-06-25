package com.bcpme.gestion_reglementaire.comparison;

import java.math.BigDecimal;
import java.util.List;

public record ColComparisonResult(
		ColFile fileA,
		ColFile fileB,
		List<ColLineDiff> diffs,
		int addedCount,
		int removedCount,
		int modifiedCount,
		int unchangedCount,
		BigDecimal totalDelta
) {
	public List<ColLineDiff> significantDiffs() {
		return diffs.stream()
				.filter(d -> d.changeType() != ChangeType.UNCHANGED)
				.toList();
	}
}
