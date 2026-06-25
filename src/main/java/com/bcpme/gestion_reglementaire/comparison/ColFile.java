package com.bcpme.gestion_reglementaire.comparison;

import java.time.LocalDate;
import java.util.Map;

public record ColFile(
		String filename,
		String institutionCode,
		LocalDate fileDate,
		Map<String, ColLine> lines,
		int accountCount
) {
}
