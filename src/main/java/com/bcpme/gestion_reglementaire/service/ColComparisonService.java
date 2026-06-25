package com.bcpme.gestion_reglementaire.service;

import com.bcpme.gestion_reglementaire.comparison.*;
import com.bcpme.gestion_reglementaire.entity.ComparisonSchedule;
import com.bcpme.gestion_reglementaire.entity.GenerationColHistory;
import com.bcpme.gestion_reglementaire.entity.GenerationSchedule;
import com.bcpme.gestion_reglementaire.repository.GenerationColHistoryRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ColComparisonService {

	private final ColFileService colFileService;
	private final GenerationColHistoryRepository historyRepository;
	private final ColComparisonHistoryService comparisonHistoryService;

	public ColComparisonService(ColFileService colFileService,
								GenerationColHistoryRepository historyRepository,
								ColComparisonHistoryService comparisonHistoryService) {
		this.colFileService = colFileService;
		this.historyRepository = historyRepository;
		this.comparisonHistoryService = comparisonHistoryService;
	}

	public ColComparisonResult compare(String filenameA, String filenameB) throws IOException {
		ColFile fileA = colFileService.parse(filenameA);
		ColFile fileB = colFileService.parse(filenameB);
		return compare(fileA, fileB);
	}

	public ColComparisonResult compare(ColFile fileA, ColFile fileB) {
		List<ColLineDiff> diffs = new ArrayList<>();
		int added = 0;
		int removed = 0;
		int modified = 0;
		int unchanged = 0;
		BigDecimal totalDelta = BigDecimal.ZERO;

		Set<String> allCodes = new HashSet<>();
		allCodes.addAll(fileA.lines().keySet());
		allCodes.addAll(fileB.lines().keySet());

		List<String> sortedCodes = allCodes.stream().sorted().toList();

		for (String code : sortedCodes) {
			ColLine lineA = fileA.lines().get(code);
			ColLine lineB = fileB.lines().get(code);

			if (lineA == null && lineB != null) {
				added++;
				diffs.add(new ColLineDiff(code, null, lineB.libelle(), null, lineB.montant(),
						lineB.montant(), ChangeType.ADDED));
			} else if (lineA != null && lineB == null) {
				removed++;
				diffs.add(new ColLineDiff(code, lineA.libelle(), null, lineA.montant(), null,
						lineA.montant().negate(), ChangeType.REMOVED));
			} else if (lineA != null) {
				BigDecimal delta = lineB.montant().subtract(lineA.montant());
				boolean sameLibelle = lineA.libelle().equals(lineB.libelle());
				boolean sameMontant = delta.compareTo(BigDecimal.ZERO) == 0;

				if (sameLibelle && sameMontant) {
					unchanged++;
					diffs.add(new ColLineDiff(code, lineA.libelle(), lineB.libelle(),
							lineA.montant(), lineB.montant(), delta, ChangeType.UNCHANGED));
				} else {
					modified++;
					totalDelta = totalDelta.add(delta);
					diffs.add(new ColLineDiff(code, lineA.libelle(), lineB.libelle(),
							lineA.montant(), lineB.montant(), delta, ChangeType.MODIFIED));
				}
			}
		}

		return new ColComparisonResult(fileA, fileB, diffs, added, removed, modified, unchanged, totalDelta);
	}

	public List<GenerationColHistory> filesForSchedule(GenerationSchedule schedule) {
		return filesForPlanificateur(schedule);
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

	public List<ComparisonTrendPoint> syncAndGetTrendForPlanificateur(GenerationSchedule schedule,
																	  String utilisateur) throws IOException {
		syncPlanificateurComparisons(schedule, utilisateur);
		return comparisonHistoryService.findByPlanificateurTrendAsc(schedule.getId()).stream()
				.map(comparisonHistoryService::toTrendPoint)
				.toList();
	}

	private void syncPlanificateurComparisons(GenerationSchedule schedule, String utilisateur) throws IOException {
		List<GenerationColHistory> files = filesForPlanificateur(schedule);

		if (files.size() < 2) {
			return;
		}

		java.util.Set<String> existingPairs = comparisonHistoryService.findByPlanificateurTrendAsc(schedule.getId()).stream()
				.map(h -> h.getFichierA() + "|" + h.getFichierB())
				.collect(java.util.stream.Collectors.toSet());

		for (int i = 1; i < files.size(); i++) {
			String fichierA = files.get(i - 1).getNomFichier();
			String fichierB = files.get(i).getNomFichier();
			String pairKey = fichierA + "|" + fichierB;

			if (!existingPairs.contains(pairKey)) {
				ColComparisonResult result = compare(fichierA, fichierB);
				comparisonHistoryService.save(result, "TENDANCE", utilisateur, null, schedule.getId());
				existingPairs.add(pairKey);
			}
		}
	}

	public ColComparisonResult latestPairComparisonForPlanificateur(GenerationSchedule schedule) throws IOException {
		List<GenerationColHistory> files = filesForPlanificateur(schedule);

		if (files.size() < 2) {
			throw new IllegalStateException("Au moins deux fichiers sont nécessaires pour une comparaison");
		}

		GenerationColHistory older = files.get(files.size() - 2);
		GenerationColHistory newer = files.get(files.size() - 1);
		return compare(older.getNomFichier(), newer.getNomFichier());
	}

	public List<ComparisonTrendPoint> syncAndGetTrend(ComparisonSchedule schedule, String utilisateur)
			throws IOException {
		syncSuiviComparisons(schedule, utilisateur);
		return comparisonHistoryService.findBySuiviAsc(schedule.getId()).stream()
				.map(comparisonHistoryService::toTrendPoint)
				.toList();
	}

	private void syncSuiviComparisons(ComparisonSchedule schedule, String utilisateur) throws IOException {
		LocalDateTime debut = schedule.getDateDebut() != null
				? schedule.getDateDebut()
				: LocalDateTime.now().minusMonths(3);
		LocalDateTime fin = schedule.getDateFin() != null
				? schedule.getDateFin()
				: LocalDateTime.now().plusDays(1);

		List<GenerationColHistory> files = historyRepository
				.findByStatutAndDateGenerationBetweenAndNomFichierIsNotNullOrderByDateGenerationAsc(
						"SUCCESS", debut, fin);

		if (files.size() < 2) {
			return;
		}

		int step = Math.max(1, schedule.getFrequenceJours() != null ? schedule.getFrequenceJours() : 1);

		java.util.Set<String> existingPairs = comparisonHistoryService.findBySuiviAsc(schedule.getId()).stream()
				.map(h -> h.getFichierA() + "|" + h.getFichierB())
				.collect(java.util.stream.Collectors.toSet());

		for (int i = step; i < files.size(); i += step) {
			String fichierA = files.get(i - step).getNomFichier();
			String fichierB = files.get(i).getNomFichier();
			String pairKey = fichierA + "|" + fichierB;

			if (!existingPairs.contains(pairKey)) {
				ColComparisonResult result = compare(fichierA, fichierB);
				comparisonHistoryService.save(result, "SUIVI", utilisateur, schedule.getId(), null);
				existingPairs.add(pairKey);
			}
		}
	}

	public List<ComparisonTrendPoint> buildTrend(ComparisonSchedule schedule, String utilisateur)
			throws IOException {
		return syncAndGetTrend(schedule, utilisateur);
	}

	public List<ComparisonTrendPoint> buildTrendFromFiles(List<GenerationColHistory> files,
															Integer frequenceJours,
															Long comparisonScheduleId,
															String utilisateur) throws IOException {
		List<ComparisonTrendPoint> points = new ArrayList<>();
		if (files.size() < 2) {
			return points;
		}

		int step = Math.max(1, frequenceJours != null ? frequenceJours : 1);

		for (int i = step; i < files.size(); i += step) {
			GenerationColHistory previous = files.get(i - step);
			GenerationColHistory current = files.get(i);

			ColComparisonResult result = compare(previous.getNomFichier(), current.getNomFichier());
			var saved = comparisonHistoryService.save(
					result, "SUIVI", utilisateur, comparisonScheduleId, null);

			points.add(new ComparisonTrendPoint(
					current.getDateGeneration(),
					previous.getNomFichier(),
					current.getNomFichier(),
					result.fileA().accountCount(),
					result.fileB().accountCount(),
					result.addedCount(),
					result.removedCount(),
					result.modifiedCount(),
					result.totalDelta(),
					saved.getId()
			));
		}

		return points;
	}

	public ColComparisonResult latestPairComparison(ComparisonSchedule schedule) throws IOException {
		List<GenerationColHistory> files = historyRepository
				.findByStatutAndNomFichierIsNotNullOrderByDateGenerationDesc("SUCCESS");

		if (files.size() < 2) {
			throw new IllegalStateException("Au moins deux fichiers sont nécessaires pour une comparaison");
		}

		int step = Math.max(1, schedule.getFrequenceJours() != null ? schedule.getFrequenceJours() : 1);
		int previousIndex = Math.min(step, files.size() - 1);
		return compare(files.get(previousIndex).getNomFichier(), files.get(0).getNomFichier());
	}

	public List<FileComparisonChartPoint> buildChartData(String fichierA, String fichierB) throws IOException {
		ColComparisonResult result = compare(fichierA, fichierB);
		return result.diffs().stream()
				.filter(d -> d.changeType() != ChangeType.UNCHANGED)
				.map(d -> new FileComparisonChartPoint(
						d.code(),
						d.libelleB() != null ? d.libelleB() : (d.libelleA() != null ? d.libelleA() : d.code()),
						d.montantA() != null ? d.montantA() : BigDecimal.ZERO,
						d.montantB() != null ? d.montantB() : BigDecimal.ZERO
				))
				.toList();
	}
}
