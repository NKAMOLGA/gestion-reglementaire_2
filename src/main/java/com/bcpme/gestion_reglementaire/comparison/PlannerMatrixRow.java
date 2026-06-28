package com.bcpme.gestion_reglementaire.comparison;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record PlannerMatrixRow(
		LocalDate businessDate,
		String filename,
		Map<String, BigDecimal> amounts
) {
}
