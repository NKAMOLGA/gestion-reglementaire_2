package com.bcpme.gestion_reglementaire.comparison;

import java.util.List;

public record PlannerMatrixView(
		List<String> accountCodes,
		List<PlannerMatrixRow> rows
) {
}
