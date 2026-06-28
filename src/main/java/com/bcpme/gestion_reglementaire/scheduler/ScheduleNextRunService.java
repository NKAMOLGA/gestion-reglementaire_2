package com.bcpme.gestion_reglementaire.scheduler;

import com.bcpme.gestion_reglementaire.entity.GenerationSchedule;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ScheduleNextRunService {

	public LocalDateTime computeNextRun(GenerationSchedule schedule, LocalDateTime after) {
		if (schedule.getHeureExecution() == null) {
			return null;
		}

		LocalDateTime effectiveAfter = resolveEffectiveAfter(schedule, after);
		LocalDateTime next = isDaily(schedule)
				? computeNextDaily(schedule, effectiveAfter)
				: computeNextMonthly(schedule, effectiveAfter);

		if (next != null && schedule.getDateFin() != null && next.isAfter(schedule.getDateFin())) {
			return null;
		}
		return next;
	}

	public List<LocalDateTime> findMissedRuns(GenerationSchedule schedule, LocalDateTime until) {
		List<LocalDateTime> missed = new ArrayList<>();
		LocalDateTime cursor = schedule.getDerniereExecution();
		if (cursor == null) {
			cursor = schedule.getDateDebut() != null
					? schedule.getDateDebut().minusSeconds(1)
					: until.minusYears(1);
		}

		while (true) {
			LocalDateTime next = computeNextRun(schedule, cursor);
			if (next == null || next.isAfter(until)) {
				break;
			}
			missed.add(next);
			cursor = next;
		}
		return missed;
	}

	public boolean isDaily(GenerationSchedule schedule) {
		if (schedule.getTypeRecurrence() != null) {
			return ScheduleRecurrenceType.DAILY.name().equalsIgnoreCase(schedule.getTypeRecurrence());
		}
		return schedule.getJoursDuMois() == null || schedule.getJoursDuMois().isBlank();
	}

	public List<Integer> parseDays(String csv) {
		return parseCsvInts(csv).stream()
				.filter(day -> day >= 1 && day <= 31)
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}

	public List<Integer> parseMonthsSelection(String csv) {
		return parseCsvInts(csv).stream()
				.filter(month -> month >= 1 && month <= 12)
				.distinct()
				.sorted()
				.collect(Collectors.toList());
	}

	/** Pour l'exécution : mois vide = tous les mois (legacy). */
	public List<Integer> resolveMonths(String csv) {
		List<Integer> months = parseMonthsSelection(csv);
		return months.isEmpty() ? allMonths() : months;
	}

	public List<Integer> allMonths() {
		return IntStream.rangeClosed(1, 12).boxed().collect(Collectors.toList());
	}

	public String joinInts(List<Integer> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		return values.stream()
				.distinct()
				.sorted()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
	}

	/**
	 * Date d'exécution pour un jour planifié.
	 * - Règle générale : jour D → exécution D+1 (ex. 10 → 11).
	 * - Mois à 30 jours + jour 30 : exécution le dernier jour du mois suivant
	 *   (ex. 30 avril → 31 mai), date métier = 30 avril.
	 */
	public LocalDate executionDateForPlannerDay(YearMonth plannerMonth, int plannerDay) {
		if (plannerDay > plannerMonth.lengthOfMonth()) {
			return null;
		}
		if (plannerDay == 30 && plannerMonth.lengthOfMonth() == 30) {
			return plannerMonth.plusMonths(1).atEndOfMonth();
		}
		return plannerMonth.atDay(plannerDay).plusDays(1);
	}

	/**
	 * Retourne le jour planifié (10, 20, 30…) correspondant à une exécution planificateur.
	 */
	public Integer resolvePlannerDaySuffix(GenerationSchedule schedule, LocalDateTime executionAt) {
		PlannerRun match = findPlannerRun(schedule, executionAt);
		return match != null ? match.plannerDay() : null;
	}

	/** Date métier (jour planifié dans son mois). Ex. exécution 31/05 → 30/04 si jour planifié = 30 avril. */
	public LocalDate resolveBusinessDate(GenerationSchedule schedule, LocalDateTime executionAt) {
		PlannerRun match = findPlannerRun(schedule, executionAt);
		return match != null ? match.businessDate() : null;
	}

	private PlannerRun findPlannerRun(GenerationSchedule schedule, LocalDateTime executionAt) {
		if (isDaily(schedule) || executionAt == null) {
			return null;
		}

		List<Integer> plannerDays = parseDays(schedule.getJoursDuMois());
		List<Integer> months = parseMonthsSelection(schedule.getMois());
		if (plannerDays.isEmpty() || months.isEmpty()) {
			return null;
		}

		LocalDate execDate = executionAt.toLocalDate();
		YearMonth execMonth = YearMonth.from(execDate);

		for (int monthOffset = -2; monthOffset <= 0; monthOffset++) {
			YearMonth plannerMonth = execMonth.plusMonths(monthOffset);
			if (!months.contains(plannerMonth.getMonthValue())) {
				continue;
			}
			for (int plannerDay : plannerDays) {
				LocalDate expectedExecution = executionDateForPlannerDay(plannerMonth, plannerDay);
				if (execDate.equals(expectedExecution)) {
					return new PlannerRun(plannerMonth, plannerDay);
				}
			}
		}
		return null;
	}

	private record PlannerRun(YearMonth plannerMonth, int plannerDay) {
		LocalDate businessDate() {
			return plannerMonth.atDay(plannerDay);
		}
	}

	private LocalDateTime resolveEffectiveAfter(GenerationSchedule schedule, LocalDateTime after) {
		if (after != null) {
			return after;
		}
		if (schedule.getDateDebut() != null) {
			return schedule.getDateDebut().minusSeconds(1);
		}
		return LocalDateTime.now().minusSeconds(1);
	}

	private LocalDateTime computeNextMonthly(GenerationSchedule schedule, LocalDateTime after) {
		List<Integer> days = parseDays(schedule.getJoursDuMois());
		List<Integer> months = parseMonthsSelection(schedule.getMois());
		if (days.isEmpty() || months.isEmpty()) {
			return null;
		}

		LocalTime time = schedule.getHeureExecution();
		YearMonth startMonth = YearMonth.from(after.toLocalDate());

		for (int monthOffset = 0; monthOffset < 36; monthOffset++) {
			YearMonth candidateMonth = startMonth.plusMonths(monthOffset);
			if (!months.contains(candidateMonth.getMonthValue())) {
				continue;
			}

			for (int plannerDay : days) {
				LocalDate executionDate = executionDateForPlannerDay(candidateMonth, plannerDay);
				if (executionDate == null) {
					continue;
				}
				LocalDateTime candidate = LocalDateTime.of(executionDate, time);
				if (!candidate.isAfter(after)) {
					continue;
				}
				if (schedule.getDateDebut() != null && candidate.isBefore(schedule.getDateDebut())) {
					continue;
				}
				return candidate;
			}
		}
		return null;
	}

	private LocalDateTime computeNextDaily(GenerationSchedule schedule, LocalDateTime after) {
		int frequency = schedule.getFrequenceJours() != null ? schedule.getFrequenceJours() : 1;
		LocalTime time = schedule.getHeureExecution();
		LocalDate anchor = schedule.getDateDebut() != null
				? schedule.getDateDebut().toLocalDate()
				: after.toLocalDate();

		LocalDate cursor = anchor;
		if (!after.toLocalDate().isBefore(anchor)) {
			long daysBetween = ChronoUnit.DAYS.between(anchor, after.toLocalDate());
			long completedPeriods = daysBetween / frequency;
			cursor = anchor.plusDays(completedPeriods * frequency);
			LocalDateTime candidate = LocalDateTime.of(cursor, time);
			if (!candidate.isAfter(after)) {
				cursor = cursor.plusDays(frequency);
			}
		}

		for (int i = 0; i < 366 * 10; i++) {
			LocalDateTime candidate = LocalDateTime.of(cursor, time);
			if (candidate.isAfter(after)) {
				if (schedule.getDateDebut() == null || !candidate.isBefore(schedule.getDateDebut())) {
					return candidate;
				}
			}
			cursor = cursor.plusDays(frequency);
		}
		return null;
	}

	private List<Integer> parseCsvInts(String csv) {
		if (csv == null || csv.isBlank()) {
			return Collections.emptyList();
		}
		return Arrays.stream(csv.split(","))
				.map(String::trim)
				.filter(value -> !value.isEmpty())
				.map(Integer::parseInt)
				.collect(Collectors.toList());
	}
}
