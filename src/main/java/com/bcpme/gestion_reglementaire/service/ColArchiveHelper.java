package com.bcpme.gestion_reglementaire.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ColArchiveHelper {

	public static final String INSTITUTION_CODE_DEFAULT = "10036";

	/**
	 * Format : COLLECTE-CM-10036-20260615-143052-10.Col
	 */
	public String buildArchiveFileName(String institutionCode, LocalDate businessDate, LocalDateTime generationTime) {
		String code = institutionCode != null && !institutionCode.isBlank()
				? institutionCode
				: INSTITUTION_CODE_DEFAULT;
		String datePart = businessDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String timePart = generationTime.format(DateTimeFormatter.ofPattern("HHmmss"));
		return String.format("COLLECTE-CM-%s-%s-%s-10.Col", code, datePart, timePart);
	}

	public List<String> readLines(Path path) throws IOException {
		return Files.readAllLines(path, StandardCharsets.ISO_8859_1);
	}

	public void writeLines(Path path, List<String> lines) throws IOException {
		Files.write(path, lines, StandardCharsets.ISO_8859_1);
	}

	public String extractInstitutionCode(List<String> lines) {
		if (lines.isEmpty()) {
			return INSTITUTION_CODE_DEFAULT;
		}
		String[] parts = lines.getFirst().split(";", -1);
		if (parts.length > 0 && !parts[0].isBlank()) {
			return parts[0].trim();
		}
		return INSTITUTION_CODE_DEFAULT;
	}

	/**
	 * Met à jour la date d'entête (ligne 1) avec la date métier (J-1).
	 */
	public void applyBusinessDateHeader(List<String> lines, LocalDate businessDate) {
		if (lines.isEmpty()) {
			return;
		}
		String[] parts = lines.getFirst().split(";", -1);
		if (parts.length >= 4) {
			String institution = parts[0].trim();
			lines.set(0, String.format("%s;%d;%02d;%02d",
					institution,
					businessDate.getYear(),
					businessDate.getMonthValue(),
					businessDate.getDayOfMonth()));
		}
	}

	public List<String> prepareArchiveContent(List<String> originalLines, LocalDate businessDate) {
		List<String> lines = new ArrayList<>(originalLines);
		applyBusinessDateHeader(lines, businessDate);
		return lines;
	}
}
