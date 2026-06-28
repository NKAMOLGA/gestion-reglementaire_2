package com.bcpme.gestion_reglementaire.comparison;

import java.util.List;

public record AccountTrendView(
		String accountCode,
		String libelle,
		List<AccountTrendPoint> points
) {
}
