package com.bcpme.gestion_reglementaire.controller;

import com.bcpme.gestion_reglementaire.comparison.ColComparisonResult;
import com.bcpme.gestion_reglementaire.comparison.ComparisonTrendPoint;
import com.bcpme.gestion_reglementaire.comparison.FileComparisonChartPoint;
import com.bcpme.gestion_reglementaire.entity.ColComparisonHistory;
import com.bcpme.gestion_reglementaire.entity.ComparisonSchedule;
import com.bcpme.gestion_reglementaire.entity.GenerationColHistory;
import com.bcpme.gestion_reglementaire.entity.GenerationSchedule;
import com.bcpme.gestion_reglementaire.repository.ComparisonScheduleRepository;
import com.bcpme.gestion_reglementaire.repository.GenerationScheduleRepository;
import com.bcpme.gestion_reglementaire.service.ArchiveService;
import com.bcpme.gestion_reglementaire.service.ColComparisonHistoryService;
import com.bcpme.gestion_reglementaire.service.ColComparisonService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/comparaison")
public class ColComparisonController {

	private final ArchiveService archiveService;
	private final ColComparisonService comparisonService;
	private final ColComparisonHistoryService comparisonHistoryService;
	private final GenerationScheduleRepository generationScheduleRepository;
	private final ComparisonScheduleRepository comparisonScheduleRepository;

	public ColComparisonController(ArchiveService archiveService,
								   ColComparisonService comparisonService,
								   ColComparisonHistoryService comparisonHistoryService,
								   GenerationScheduleRepository generationScheduleRepository,
								   ComparisonScheduleRepository comparisonScheduleRepository) {
		this.archiveService = archiveService;
		this.comparisonService = comparisonService;
		this.comparisonHistoryService = comparisonHistoryService;
		this.generationScheduleRepository = generationScheduleRepository;
		this.comparisonScheduleRepository = comparisonScheduleRepository;
	}

	@GetMapping
	public String index() {
		return "comparaison/index";
	}

	@GetMapping("/historique")
	public String historique(@RequestParam(required = false) Long suiviId, Model model) {
		List<ColComparisonHistory> entries = suiviId != null
				? comparisonHistoryService.findBySuivi(suiviId)
				: comparisonHistoryService.findAll();
		model.addAttribute("entries", entries);
		model.addAttribute("suiviId", suiviId);
		return "comparaison/historique";
	}

	// --- Cas 2 : comparaison manuelle ---

	@GetMapping("/manuelle")
	public String manuelle(Model model) {
		model.addAttribute("fichiers", archiveService.getAllArchives());
		return "comparaison/manuelle";
	}

	@PostMapping("/manuelle")
	public String manuelleCompare(@RequestParam String fichierA,
								 @RequestParam String fichierB,
								 RedirectAttributes redirectAttributes) {
		if (fichierA.equals(fichierB)) {
			redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner deux fichiers différents.");
			return "redirect:/comparaison/manuelle";
		}
		try {
			ColComparisonResult result = comparisonService.compare(fichierA, fichierB);
			ColComparisonHistory saved = comparisonHistoryService.save(
					result, "MANUELLE", currentUsername(), null, null);
			return "redirect:/comparaison/resultat/" + saved.getId();
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/comparaison/manuelle";
		}
	}

	// --- Cas 1 : depuis un planificateur ---

	@GetMapping("/planificateur")
	public String planificateur(Model model) {
		model.addAttribute("schedules", generationScheduleRepository.findAll());
		return "comparaison/planificateur";
	}

	@GetMapping("/planificateur/{scheduleId}")
	public String planificateurFiles(@PathVariable Long scheduleId, Model model) {
		GenerationSchedule schedule = generationScheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new RuntimeException("Planification introuvable"));

		List<GenerationColHistory> fichiers = comparisonService.filesForSchedule(schedule);

		model.addAttribute("schedule", schedule);
		model.addAttribute("fichiers", fichiers);
		return "comparaison/planificateur-fichiers";
	}

	@PostMapping("/planificateur/{scheduleId}")
	public String planificateurCompare(@PathVariable Long scheduleId,
									   @RequestParam String fichierA,
									   @RequestParam String fichierB,
									   RedirectAttributes redirectAttributes) {
		if (fichierA.equals(fichierB)) {
			redirectAttributes.addFlashAttribute("error", "Veuillez sélectionner deux fichiers différents.");
			return "redirect:/comparaison/planificateur/" + scheduleId;
		}
		try {
			ColComparisonResult result = comparisonService.compare(fichierA, fichierB);
			ColComparisonHistory saved = comparisonHistoryService.save(
					result, "PLANIFICATEUR", currentUsername(), null, scheduleId);
			return "redirect:/comparaison/resultat/" + saved.getId();
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/comparaison/planificateur/" + scheduleId;
		}
	}

	@GetMapping("/planificateur/{scheduleId}/derniers")
	public String planificateurDerniers(@PathVariable Long scheduleId,
										RedirectAttributes redirectAttributes) {
		GenerationSchedule schedule = generationScheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new RuntimeException("Planification introuvable"));

		List<GenerationColHistory> fichiers = comparisonService.filesForSchedule(schedule);
		if (fichiers.size() < 2) {
			redirectAttributes.addFlashAttribute("error",
					"Pas assez de fichiers générés dans la période de cette planification.");
			return "redirect:/comparaison/planificateur/" + scheduleId;
		}

		GenerationColHistory older = fichiers.get(fichiers.size() - 2);
		GenerationColHistory newer = fichiers.get(fichiers.size() - 1);

		try {
			ColComparisonResult result = comparisonService.compare(older.getNomFichier(), newer.getNomFichier());
			ColComparisonHistory saved = comparisonHistoryService.save(
					result, "PLANIFICATEUR", currentUsername(), null, scheduleId);
			return "redirect:/comparaison/resultat/" + saved.getId();
		} catch (IOException e) {
			redirectAttributes.addFlashAttribute("error", e.getMessage());
			return "redirect:/comparaison/planificateur/" + scheduleId;
		}
	}

	// --- Résultat persistant ---

	@GetMapping("/resultat/{id}")
	public String resultat(@PathVariable Long id, Model model) {
		ColComparisonHistory history = comparisonHistoryService.findById(id);
		ColComparisonResult result = comparisonHistoryService.toResult(history);

		model.addAttribute("result", result);
		model.addAttribute("history", history);
		model.addAttribute("source", history.getMode());
		model.addAttribute("scheduleId", history.getGenerationScheduleId());
		model.addAttribute("suiviId", history.getComparisonScheduleId());
		return "comparaison/resultat";
	}

	// --- Cas 3 : suivi périodique ---

	@GetMapping("/suivi")
	public String suiviList(Model model) {
		model.addAttribute("schedules", comparisonScheduleRepository.findAll());
		return "comparaison/suivi-list";
	}

	@GetMapping("/suivi/new")
	public String suiviNew(Model model) {
		ComparisonSchedule schedule = new ComparisonSchedule();
		schedule.setActive(true);
		schedule.setFrequenceJours(2);
		schedule.setDateDebut(LocalDateTime.now().minusMonths(1));
		schedule.setDateFin(LocalDateTime.now().plusMonths(6));
		model.addAttribute("schedule", schedule);
		return "comparaison/suivi-form";
	}

	@PostMapping("/suivi/save")
	public String suiviSave(@ModelAttribute ComparisonSchedule schedule) {
		if (schedule.getCreatedAt() == null) {
			schedule.setCreatedAt(LocalDateTime.now());
		}
		schedule.setUpdatedAt(LocalDateTime.now());
		comparisonScheduleRepository.save(schedule);
		return "redirect:/comparaison/suivi";
	}

	@GetMapping("/suivi/edit/{id}")
	public String suiviEdit(@PathVariable Long id, Model model) {
		ComparisonSchedule schedule = comparisonScheduleRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Suivi introuvable"));
		model.addAttribute("schedule", schedule);
		return "comparaison/suivi-form";
	}

	@GetMapping("/suivi/delete/{id}")
	public String suiviDelete(@PathVariable Long id) {
		comparisonScheduleRepository.deleteById(id);
		return "redirect:/comparaison/suivi";
	}

	@GetMapping("/suivi/{id}")
	public String suiviDashboard(@PathVariable Long id, Model model) throws IOException {
		populateSuiviDashboard(id, model, null, null, null);
		return "comparaison/suivi-dashboard";
	}

	@PostMapping("/suivi/{id}/graphique")
	public String suiviGraphique(@PathVariable Long id,
								 @RequestParam String fichierA,
								 @RequestParam String fichierB,
								 Model model,
								 RedirectAttributes redirectAttributes) throws IOException {
		if (fichierA.equals(fichierB)) {
			redirectAttributes.addFlashAttribute("graphError", "Choisissez deux fichiers différents.");
			return "redirect:/comparaison/suivi/" + id;
		}
		try {
			List<FileComparisonChartPoint> chartData = comparisonService.buildChartData(fichierA, fichierB);
			populateSuiviDashboard(id, model, fichierA, fichierB, chartData);
			return "comparaison/suivi-dashboard";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("graphError", e.getMessage());
			return "redirect:/comparaison/suivi/" + id;
		}
	}

	private void populateSuiviDashboard(Long id, Model model,
										String graphFichierA, String graphFichierB,
										List<FileComparisonChartPoint> chartData) throws IOException {
		ComparisonSchedule schedule = comparisonScheduleRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Suivi introuvable"));

		List<ComparisonTrendPoint> trends = comparisonService.syncAndGetTrend(schedule, currentUsername());
		List<ColComparisonHistory> historiqueSuivi = comparisonHistoryService.findBySuivi(id);

		ColComparisonResult latest = null;
		String latestError = null;
		Long latestId = null;

		if (!historiqueSuivi.isEmpty()) {
			ColComparisonHistory last = historiqueSuivi.getFirst();
			latest = comparisonHistoryService.toResult(last);
			latestId = last.getId();
		} else {
			try {
				ColComparisonResult computed = comparisonService.latestPairComparison(schedule);
				ColComparisonHistory saved = comparisonHistoryService.save(
						computed, "SUIVI", currentUsername(), id, null);
				latest = computed;
				latestId = saved.getId();
			} catch (Exception e) {
				latestError = e.getMessage();
			}
		}

		model.addAttribute("schedule", schedule);
		model.addAttribute("trends", trends);
		model.addAttribute("latest", latest);
		model.addAttribute("latestId", latestId);
		model.addAttribute("latestError", latestError);
		model.addAttribute("fichiers", archiveService.getAllArchives());
		model.addAttribute("graphFichierA", graphFichierA);
		model.addAttribute("graphFichierB", graphFichierB);
		model.addAttribute("chartData", chartData);
	}

	private String currentUsername() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null ? auth.getName() : "system";
	}
}
