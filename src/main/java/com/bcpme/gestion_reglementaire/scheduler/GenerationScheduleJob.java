package com.bcpme.gestion_reglementaire.scheduler;

import com.bcpme.gestion_reglementaire.entity.GenerationSchedule;
import com.bcpme.gestion_reglementaire.repository.GenerationScheduleRepository;
import com.bcpme.gestion_reglementaire.service.GenerationColService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GenerationScheduleJob {

	private final GenerationScheduleRepository repository;
	private final GenerationColService generationColService;
	private final ScheduleNextRunService nextRunService;

	@PostConstruct
	public void catchMissedExecutions() {
		System.out.println("=== Vérification des exécutions manquées ===");

		LocalDateTime now = LocalDateTime.now();
		List<GenerationSchedule> schedules = repository.findByActiveTrue();

		for (GenerationSchedule schedule : schedules) {
			try {
				ensureNextRunDate(schedule);

				List<LocalDateTime> missed = nextRunService.findMissedRuns(schedule, now);
				for (LocalDateTime executionAt : missed) {
					if (schedule.getDateFin() != null && executionAt.isAfter(schedule.getDateFin())) {
						break;
					}

					System.out.println("RATTRAPAGE : " + schedule.getNom() + " @ " + executionAt);
					Integer plannerDay = nextRunService.resolvePlannerDaySuffix(schedule, executionAt);
					LocalDate businessDate = nextRunService.resolveBusinessDate(schedule, executionAt);
					generationColService.lancerGeneration("PLANIFICATEUR", schedule.getId(), plannerDay, businessDate);
					schedule.setDerniereExecution(executionAt);
					schedule.setNextRunDate(nextRunService.computeNextRun(schedule, executionAt));
					repository.save(schedule);
				}
			} catch (Exception e) {
				System.err.println("Erreur rattrapage : " + schedule.getNom());
				e.printStackTrace();
			}
		}
	}

	@Scheduled(fixedRate = 60000)
	public void executeSchedules() {
		LocalDateTime now = LocalDateTime.now();
		List<GenerationSchedule> schedules = repository.findByActiveTrue();

		for (GenerationSchedule schedule : schedules) {
			try {
				if (schedule.getDateDebut() != null && now.isBefore(schedule.getDateDebut())) {
					continue;
				}
				if (schedule.getDateFin() != null && now.isAfter(schedule.getDateFin())) {
					continue;
				}

				ensureNextRunDate(schedule);

				LocalDateTime nextRun = schedule.getNextRunDate();
				if (nextRun == null || now.isBefore(nextRun)) {
					continue;
				}

				// Fenêtre d'une minute (comme le poll toutes les 60 s)
				if (now.isAfter(nextRun.plusMinutes(1))) {
					List<LocalDateTime> missed = nextRunService.findMissedRuns(schedule, now);
					if (missed.isEmpty()) {
						schedule.setNextRunDate(nextRunService.computeNextRun(schedule, now));
						repository.save(schedule);
						continue;
					}
					nextRun = missed.getFirst();
				}

				System.out.println("EXÉCUTION : " + schedule.getNom() + " @ " + nextRun);
				Integer plannerDay = nextRunService.resolvePlannerDaySuffix(schedule, nextRun);
				LocalDate businessDate = nextRunService.resolveBusinessDate(schedule, nextRun);
				generationColService.lancerGeneration("PLANIFICATEUR", schedule.getId(), plannerDay, businessDate);

				schedule.setDerniereExecution(nextRun);
				schedule.setNextRunDate(nextRunService.computeNextRun(schedule, nextRun));
				repository.save(schedule);

			} catch (Exception e) {
				System.err.println("Erreur exécution : " + schedule.getNom());
				e.printStackTrace();
			}
		}
	}

	private void ensureNextRunDate(GenerationSchedule schedule) {
		if (schedule.getNextRunDate() == null) {
			LocalDateTime after = schedule.getDerniereExecution();
			schedule.setNextRunDate(nextRunService.computeNextRun(schedule, after));
			repository.save(schedule);
		}
	}
}
