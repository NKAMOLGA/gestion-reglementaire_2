package com.bcpme.gestion_reglementaire.controller;

import com.bcpme.gestion_reglementaire.entity.GenerationSchedule;
import com.bcpme.gestion_reglementaire.repository.GenerationScheduleRepository;
import com.bcpme.gestion_reglementaire.scheduler.ScheduleNextRunService;
import com.bcpme.gestion_reglementaire.scheduler.ScheduleRecurrenceType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/schedules")
public class GenerationScheduleController {

	private final GenerationScheduleRepository repository;
	private final ScheduleNextRunService nextRunService;

	public GenerationScheduleController(GenerationScheduleRepository repository,
										ScheduleNextRunService nextRunService) {
		this.repository = repository;
		this.nextRunService = nextRunService;
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("schedules", repository.findAll());
		return "schedules/list";
	}

	@GetMapping("/view/{id}")
	public String view(@PathVariable Long id, Model model) {
		GenerationSchedule schedule = repository.findById(id)
				.orElseThrow(() -> new RuntimeException("Planification introuvable : " + id));
		model.addAttribute("schedule", schedule);
		return "schedules/view";
	}

	@GetMapping("/new")
	public String createForm(Model model) {
		GenerationSchedule schedule = new GenerationSchedule();
		schedule.setTypeRecurrence(ScheduleRecurrenceType.MONTHLY.name());
		schedule.setActive(false);
		model.addAttribute("schedule", schedule);
		model.addAttribute("isNewSchedule", true);
		populateFormSelections(model, schedule, List.of(), List.of());
		return "schedules/form";
	}

	@PostMapping("/save")
	public String save(@ModelAttribute GenerationSchedule schedule,
					   @RequestParam(required = false) List<Integer> selectedDays,
					   @RequestParam(required = false) List<Integer> selectedMonths) {

		if (ScheduleRecurrenceType.MONTHLY.name().equalsIgnoreCase(schedule.getTypeRecurrence())) {
			schedule.setJoursDuMois(nextRunService.joinInts(selectedDays));
			schedule.setMois(nextRunService.joinInts(selectedMonths));
			schedule.setFrequenceJours(null);
		} else {
			schedule.setJoursDuMois(null);
			schedule.setMois(null);
			if (schedule.getFrequenceJours() == null || schedule.getFrequenceJours() < 1) {
				schedule.setFrequenceJours(1);
			}
		}

		if (schedule.getId() != null) {
			GenerationSchedule existing = repository.findById(schedule.getId())
					.orElseThrow();
			schedule.setDerniereExecution(existing.getDerniereExecution());
			schedule.setCreatedBy(existing.getCreatedBy());
			schedule.setCreatedAt(existing.getCreatedAt());
		} else {
			schedule.setCreatedAt(LocalDateTime.now());
		}
		schedule.setUpdatedAt(LocalDateTime.now());

		LocalDateTime after = schedule.getDerniereExecution();
		schedule.setNextRunDate(nextRunService.computeNextRun(schedule, after));

		repository.save(schedule);
		return "redirect:/schedules";
	}

	@GetMapping("/edit/{id}")
	public String editForm(@PathVariable Long id, Model model) {
		GenerationSchedule schedule = repository.findById(id)
				.orElseThrow(() -> new RuntimeException("Planification introuvable : " + id));

		if (schedule.getTypeRecurrence() == null) {
			schedule.setTypeRecurrence(nextRunService.isDaily(schedule)
					? ScheduleRecurrenceType.DAILY.name()
					: ScheduleRecurrenceType.MONTHLY.name());
		}

		model.addAttribute("schedule", schedule);
		model.addAttribute("isNewSchedule", false);
		populateFormSelections(
				model,
				schedule,
				nextRunService.parseDays(schedule.getJoursDuMois()),
				nextRunService.parseMonthsSelection(schedule.getMois())
		);
		return "schedules/form";
	}

	@GetMapping("/delete/{id}")
	public String delete(@PathVariable Long id) {
		repository.deleteById(id);
		return "redirect:/schedules";
	}

	@GetMapping("/toggle/{id}")
	public String toggle(@PathVariable Long id) {
		GenerationSchedule schedule = repository.findById(id)
				.orElseThrow(() -> new RuntimeException("Planification introuvable : " + id));
		schedule.setActive(!Boolean.TRUE.equals(schedule.getActive()));
		repository.save(schedule);
		return "redirect:/schedules";
	}

	private void populateFormSelections(Model model,
									  GenerationSchedule schedule,
									  List<Integer> selectedDays,
									  List<Integer> selectedMonths) {
		model.addAttribute("selectedDays", selectedDays);
		model.addAttribute("selectedMonths", selectedMonths);
		model.addAttribute("allMonths", nextRunService.allMonths());
	}
}
