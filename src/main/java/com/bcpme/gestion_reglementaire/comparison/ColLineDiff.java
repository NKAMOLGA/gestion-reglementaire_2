package com.bcpme.gestion_reglementaire.comparison;

import java.math.BigDecimal;

public record ColLineDiff(
		String code,
		String libelleA,
		String libelleB,
		BigDecimal montantA,
		BigDecimal montantB,
		BigDecimal delta,
		ChangeType changeType
) {
}
