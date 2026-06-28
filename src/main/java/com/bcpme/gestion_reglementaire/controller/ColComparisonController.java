package com.bcpme.gestion_reglementaire.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bcpme.gestion_reglementaire.comparison.AccountTrendView;
import com.bcpme.gestion_reglementaire.comparison.PlannerMatrixView;
import com.bcpme.gestion_reglementaire.entity.GenerationSchedule;
import com.bcpme.gestion_reglementaire.repository.GenerationScheduleRepository;
import com.bcpme.gestion_reglementaire.service.ColComparisonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@RequestMapping("/comparaison")
public class ColComparisonController {

	private final ColComparisonService comparisonService;
	private final GenerationScheduleRepository generationScheduleRepository;
	private final ObjectMapper objectMapper;

	public ColComparisonController(ColComparisonService comparisonService,
								   GenerationScheduleRepository generationScheduleRepository,
								   ObjectMapper objectMapper) {
		this.comparisonService = comparisonService;
		this.generationScheduleRepository = generationScheduleRepository;
		this.objectMapper = objectMapper;
	}

	@GetMapping
	public String index() {
		return "redirect:/comparaison/planificateur";
	}

	@GetMapping("/planificateur")
	public String planificateur(Model model) {
		model.addAttribute("schedules", generationScheduleRepository.findAll());
		return "comparaison/planificateur";
	}

	@GetMapping("/planificateur/{scheduleId}")
	public String planificateurMatrix(@PathVariable Long scheduleId, Model model) throws IOException {
		GenerationSchedule schedule = requireSchedule(scheduleId);
		PlannerMatrixView matrix = comparisonService.buildPlannerMatrix(schedule);

		model.addAttribute("schedule", schedule);
		model.addAttribute("matrix", matrix);
		return "comparaison/planificateur-matrix";
	}

	@GetMapping("/planificateur/{scheduleId}/compte/{accountCode}")
	public String accountTrend(@PathVariable Long scheduleId,
							   @PathVariable String accountCode,
							   Model model) throws IOException {
		GenerationSchedule schedule = requireSchedule(scheduleId);

		if (!comparisonService.accountExistsInMatrix(schedule, accountCode)) {
			throw new IllegalArgumentException("Compte introuvable pour ce planificateur : " + accountCode);
		}

		AccountTrendView trend = comparisonService.buildAccountTrend(schedule, accountCode);

		model.addAttribute("schedule", schedule);
		model.addAttribute("trend", trend);
		model.addAttribute("trendJson", toJson(trend.points()));
		return "comparaison/compte-chart";
	}

	private GenerationSchedule requireSchedule(Long scheduleId) {
		return generationScheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new RuntimeException("Planificateur introuvable"));
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Erreur de sérialisation JSON", e);
		}
	}
}
