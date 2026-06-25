package com.bcpme.gestion_reglementaire.controller;

import com.bcpme.gestion_reglementaire.comparison.ColComparisonResult;
import com.bcpme.gestion_reglementaire.comparison.ComparisonTrendPoint;
import com.bcpme.gestion_reglementaire.comparison.FileComparisonChartPoint;
import com.bcpme.gestion_reglementaire.entity.ColComparisonHistory;
import com.bcpme.gestion_reglementaire.entity.GenerationColHistory;
import com.bcpme.gestion_reglementaire.entity.GenerationSchedule;
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
import java.util.List;

@Controller
@RequestMapping("/comparaison")
public class ColComparisonController {

	private final ArchiveService archiveService;
	private final ColComparisonService comparisonService;
	private final ColComparisonHistoryService comparisonHistoryService;
	private final GenerationScheduleRepository generationScheduleRepository;

	public ColComparisonController(ArchiveService archiveService,
								   ColComparisonService comparisonService,
								   ColComparisonHistoryService comparisonHistoryService,
								   GenerationScheduleRepository generationScheduleRepository) {
		this.archiveService = archiveService;
		this.comparisonService = comparisonService;
		this.comparisonHistoryService = comparisonHistoryService;
		this.generationScheduleRepository = generationScheduleRepository;
	}

	@GetMapping
	public String index() {
		return "comparaison/index";
	}

	@GetMapping("/historique")
	public String historique(@RequestParam(required = false) Long suiviId,
							 @RequestParam(required = false) Long planificateurId,
							 Model model) {
		List<ColComparisonHistory> entries;
		if (planificateurId != null) {
			entries = comparisonHistoryService.findByPlanificateurTrendDesc(planificateurId);
		} else if (suiviId != null) {
			entries = comparisonHistoryService.findBySuivi(suiviId);
		} else {
			entries = comparisonHistoryService.findAll();
		}
		model.addAttribute("entries", entries);
		model.addAttribute("suiviId", suiviId);
		model.addAttribute("planificateurId", planificateurId);
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
	public String resultat(@PathVariable Long id, Model model) throws IOException {
		ColComparisonHistory history = comparisonHistoryService.findById(id);
		ColComparisonResult result = comparisonHistoryService.toResult(history);

		if (result.diffs().isEmpty()
				&& history.getFichierA() != null
				&& history.getFichierB() != null) {
			result = comparisonService.compare(history.getFichierA(), history.getFichierB());
		}

		model.addAttribute("result", result);
		model.addAttribute("history", history);
		model.addAttribute("source", history.getMode());
		model.addAttribute("scheduleId", history.getGenerationScheduleId());
		model.addAttribute("suiviId", history.getComparisonScheduleId());
		return "comparaison/resultat";
	}

	// --- Cas 3 : tendances par planificateur ---

	@GetMapping("/suivi")
	public String suiviList(Model model) {
		model.addAttribute("schedules", generationScheduleRepository.findAll());
		return "comparaison/suivi-list";
	}

	@GetMapping("/suivi/{id}")
	public String suiviDashboard(@PathVariable Long id, Model model) throws IOException {
		populatePlanificateurTrendDashboard(id, model, null, null, null);
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
			populatePlanificateurTrendDashboard(id, model, fichierA, fichierB, chartData);
			return "comparaison/suivi-dashboard";
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("graphError", e.getMessage());
			return "redirect:/comparaison/suivi/" + id;
		}
	}

	private void populatePlanificateurTrendDashboard(Long id, Model model,
												   String graphFichierA, String graphFichierB,
												   List<FileComparisonChartPoint> chartData) throws IOException {
		GenerationSchedule schedule = generationScheduleRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Planificateur introuvable"));

		List<ComparisonTrendPoint> trends = comparisonService.syncAndGetTrendForPlanificateur(schedule, currentUsername());
		List<ColComparisonHistory> historiqueTrend = comparisonHistoryService.findByPlanificateurTrendDesc(id);
		List<GenerationColHistory> fichiersPlanificateur = comparisonService.filesForPlanificateur(schedule);

		ColComparisonResult latest = null;
		String latestError = null;
		Long latestId = null;

		if (!historiqueTrend.isEmpty()) {
			ColComparisonHistory last = historiqueTrend.getFirst();
			latest = comparisonHistoryService.toResult(last);
			latestId = last.getId();
		} else if (fichiersPlanificateur.size() >= 2) {
			try {
				ColComparisonResult computed = comparisonService.latestPairComparisonForPlanificateur(schedule);
				ColComparisonHistory saved = comparisonHistoryService.save(
						computed, "TENDANCE", currentUsername(), null, id);
				latest = computed;
				latestId = saved.getId();
				trends = comparisonService.syncAndGetTrendForPlanificateur(schedule, currentUsername());
			} catch (Exception e) {
				latestError = e.getMessage();
			}
		} else {
			latestError = "Au moins deux fichiers générés par ce planificateur sont nécessaires.";
		}

		model.addAttribute("schedule", schedule);
		model.addAttribute("trends", trends);
		model.addAttribute("latest", latest);
		model.addAttribute("latestId", latestId);
		model.addAttribute("latestError", latestError);
		model.addAttribute("fichiers", fichiersPlanificateur);
		model.addAttribute("graphFichierA", graphFichierA);
		model.addAttribute("graphFichierB", graphFichierB);
		model.addAttribute("chartData", chartData);
	}

	private String currentUsername() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null ? auth.getName() : "system";
	}
}
