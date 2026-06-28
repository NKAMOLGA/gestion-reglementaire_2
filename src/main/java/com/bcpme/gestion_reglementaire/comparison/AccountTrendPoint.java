package com.bcpme.gestion_reglementaire.comparison;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountTrendPoint(
		LocalDate businessDate,
		BigDecimal montant
) {
}
