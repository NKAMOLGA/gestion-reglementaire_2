package com.bcpme.gestion_reglementaire.comparison;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ComparisonTrendPoint(
		LocalDateTime date,
		String filePrevious,
		String fileCurrent,
		int accountsPrevious,
		int accountsCurrent,
		int added,
		int removed,
		int modified,
		BigDecimal totalDelta,
		Long comparisonId
) {
}
