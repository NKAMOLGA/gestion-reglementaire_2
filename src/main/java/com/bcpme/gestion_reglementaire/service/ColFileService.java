package com.bcpme.gestion_reglementaire.service;

import com.bcpme.gestion_reglementaire.comparison.ColFile;
import com.bcpme.gestion_reglementaire.comparison.ColLine;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ColFileService {

	public static final String DOSSIER_ARCHIVES =
			"C:\\Users\\toses\\OneDrive\\Desktop\\Dossiers\\NK\\archives_col";

	public ColFile parse(String filename) throws IOException {
		validateFilename(filename);
		Path path = Paths.get(DOSSIER_ARCHIVES, filename);
		if (!Files.exists(path)) {
			throw new IOException("Fichier introuvable : " + filename);
		}
		List<String> lines = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
		return parseLines(filename, lines);
	}

	public void validateFilename(String filename) {
		if (filename == null || filename.isBlank()) {
			throw new IllegalArgumentException("Nom de fichier invalide");
		}
		if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
			throw new IllegalArgumentException("Nom de fichier non autorisé");
		}
		if (!filename.toLowerCase().endsWith(".col")) {
			throw new IllegalArgumentException("Seuls les fichiers .Col sont acceptés");
		}
	}

	ColFile parseLines(String filename, List<String> lines) {
		String institutionCode = "";
		LocalDate fileDate = null;
		Map<String, ColLine> data = new LinkedHashMap<>();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.isEmpty()) {
				continue;
			}
			String[] parts = line.split(";", -1);
			if (i == 0 && parts.length >= 4 && isNumeric(parts[0])) {
				institutionCode = parts[0];
				fileDate = LocalDate.of(
						Integer.parseInt(parts[1]),
						Integer.parseInt(parts[2]),
						Integer.parseInt(parts[3])
				);
				continue;
			}
			if (parts.length >= 3) {
				String code = parts[0].trim();
				String libelle = parts[1].trim();
				BigDecimal montant = parseMontant(parts[2]);
				data.put(code, new ColLine(code, libelle, montant));
			}
		}

		return new ColFile(filename, institutionCode, fileDate, data, data.size());
	}

	private boolean isNumeric(String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private BigDecimal parseMontant(String raw) {
		String cleaned = raw.trim().replace(" ", "");
		if (cleaned.isEmpty()) {
			return BigDecimal.ZERO;
		}
		return new BigDecimal(cleaned);
	}
}
