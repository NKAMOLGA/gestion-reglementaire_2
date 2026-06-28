package com.bcpme.gestion_reglementaire.service;

import com.bcpme.gestion_reglementaire.comparison.*;
import com.bcpme.gestion_reglementaire.entity.GenerationColHistory;
import com.bcpme.gestion_reglementaire.entity.GenerationSchedule;
import com.bcpme.gestion_reglementaire.repository.GenerationColHistoryRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Service
public class ColComparisonService {

	private static final DateTimeFormatter ARCHIVE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final ColFileService colFileService;
	private final GenerationColHistoryRepository historyRepository;

	public ColComparisonService(ColFileService colFileService,
								GenerationColHistoryRepository historyRepository) {
		this.colFileService = colFileService;
		this.historyRepository = historyRepository;
	}

	public List<GenerationColHistory> filesForPlanificateur(GenerationSchedule schedule) {
		List<GenerationColHistory> linked = historyRepository
				.findByStatutAndGenerationScheduleIdAndNomFichierIsNotNullOrderByDateGenerationAsc(
						"SUCCESS", schedule.getId());

		if (!linked.isEmpty()) {
			return linked;
		}

		LocalDateTime debut = schedule.getDateDebut() != null
				? schedule.getDateDebut()
				: LocalDateTime.now().minusYears(1);
		LocalDateTime fin = schedule.getDateFin() != null
				? schedule.getDateFin()
				: LocalDateTime.now().plusDays(1);

		return historyRepository
				.findByStatutAndDateGenerationBetweenAndNomFichierIsNotNullOrderByDateGenerationAsc(
						"SUCCESS", debut, fin);
	}

	public PlannerMatrixView buildPlannerMatrix(GenerationSchedule schedule) throws IOException {
		List<GenerationColHistory> files = filesForPlanificateur(schedule);
		TreeSet<String> allCodes = new TreeSet<>();
		List<PlannerMatrixRow> rows = new ArrayList<>();

		for (GenerationColHistory file : files) {
			ColFile colFile = colFileService.parse(file.getNomFichier());
			allCodes.addAll(colFile.lines().keySet());

			LocalDate businessDate = resolveBusinessDate(colFile, file.getNomFichier());
			Map<String, BigDecimal> amounts = new LinkedHashMap<>();
			for (ColLine line : colFile.lines().values()) {
				amounts.put(line.code(), line.montant());
			}

			rows.add(new PlannerMatrixRow(businessDate, file.getNomFichier(), amounts));
		}

		rows.sort(Comparator.comparing(PlannerMatrixRow::businessDate));
		return new PlannerMatrixView(new ArrayList<>(allCodes), rows);
	}

	public AccountTrendView buildAccountTrend(GenerationSchedule schedule, String accountCode) throws IOException {
		if (accountCode == null || accountCode.isBlank()
				|| accountCode.contains("..") || accountCode.contains("/") || accountCode.contains("\\")) {
			throw new IllegalArgumentException("Code compte invalide");
		}

		List<GenerationColHistory> files = filesForPlanificateur(schedule);
		List<AccountTrendPoint> points = new ArrayList<>();
		String libelle = null;

		for (GenerationColHistory file : files) {
			ColFile colFile = colFileService.parse(file.getNomFichier());
			ColLine line = colFile.lines().get(accountCode);
			if (line == null) {
				continue;
			}

			if (libelle == null) {
				libelle = line.libelle();
			}

			LocalDate businessDate = resolveBusinessDate(colFile, file.getNomFichier());
			points.add(new AccountTrendPoint(businessDate, line.montant()));
		}

		points.sort(Comparator.comparing(AccountTrendPoint::businessDate));
		return new AccountTrendView(accountCode, libelle != null ? libelle : accountCode, points);
	}

	public boolean accountExistsInMatrix(GenerationSchedule schedule, String accountCode) throws IOException {
		return buildPlannerMatrix(schedule).accountCodes().contains(accountCode);
	}

	private void validateAccountCode(String accountCode) {
		if (accountCode == null || accountCode.isBlank()) {
			throw new IllegalArgumentException("Code compte invalide");
		}
		if (accountCode.contains("..") || accountCode.contains("/") || accountCode.contains("\\")) {
			throw new IllegalArgumentException("Code compte invalide");
		}
	}

	private LocalDate resolveBusinessDate(ColFile colFile, String filename) {
		if (colFile.fileDate() != null) {
			return colFile.fileDate();
		}
		LocalDate fromFilename = parseBusinessDateFromFilename(filename);
		if (fromFilename != null) {
			return fromFilename;
		}
		return LocalDate.now();
	}

	LocalDate parseBusinessDateFromFilename(String filename) {
		if (filename == null || filename.isBlank()) {
			return null;
		}
		String base = filename;
		int dot = base.lastIndexOf('.');
		if (dot > 0) {
			base = base.substring(0, dot);
		}
		String[] parts = base.split("-");
		if (parts.length < 4) {
			return null;
		}
		try {
			return LocalDate.parse(parts[3], ARCHIVE_DATE);
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
