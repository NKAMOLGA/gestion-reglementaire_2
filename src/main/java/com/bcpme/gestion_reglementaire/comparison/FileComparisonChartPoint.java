package com.bcpme.gestion_reglementaire.comparison;

import java.math.BigDecimal;

public record FileComparisonChartPoint(
		String code,
		String libelle,
		BigDecimal montantA,
		BigDecimal montantB
) {
}
