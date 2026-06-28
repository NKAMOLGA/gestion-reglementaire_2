package com.bcpme.gestion_reglementaire.scheduler;

import com.bcpme.gestion_reglementaire.entity.GenerationSchedule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component("scheduleHelper")
public class ScheduleRecurrenceHelper {

	private final ScheduleNextRunService nextRunService;

	public ScheduleRecurrenceHelper(ScheduleNextRunService nextRunService) {
		this.nextRunService = nextRunService;
	}

	public String format(GenerationSchedule schedule) {
		if (schedule == null) {
			return "";
		}
		if (nextRunService.isDaily(schedule)) {
			int days = schedule.getFrequenceJours() != null ? schedule.getFrequenceJours() : 1;
			return days == 1 ? "Chaque jour" : "Tous les " + days + " jours";
		}

		List<Integer> days = nextRunService.parseDays(schedule.getJoursDuMois());
		List<Integer> months = nextRunService.resolveMonths(schedule.getMois());
		String daysLabel = days.stream().map(String::valueOf).collect(Collectors.joining(", "));
		String monthsLabel = months.size() == 12
				? "tous les mois"
				: months.stream().map(this::monthName).collect(Collectors.joining(", "));
		return "Chaque mois — jours " + daysLabel + " — " + monthsLabel;
	}

	public String formatEn(GenerationSchedule schedule) {
		if (schedule == null) {
			return "";
		}
		if (nextRunService.isDaily(schedule)) {
			int days = schedule.getFrequenceJours() != null ? schedule.getFrequenceJours() : 1;
			return days == 1 ? "Daily" : "Every " + days + " days";
		}

		List<Integer> days = nextRunService.parseDays(schedule.getJoursDuMois());
		List<Integer> months = nextRunService.resolveMonths(schedule.getMois());
		String daysLabel = days.stream().map(String::valueOf).collect(Collectors.joining(", "));
		String monthsLabel = months.size() == 12
				? "every month"
				: months.stream().map(this::monthNameEn).collect(Collectors.joining(", "));
		return "Monthly — days " + daysLabel + " — " + monthsLabel;
	}

	private String monthName(int month) {
		return switch (month) {
			case 1 -> "janv.";
			case 2 -> "févr.";
			case 3 -> "mars";
			case 4 -> "avr.";
			case 5 -> "mai";
			case 6 -> "juin";
			case 7 -> "juil.";
			case 8 -> "août";
			case 9 -> "sept.";
			case 10 -> "oct.";
			case 11 -> "nov.";
			case 12 -> "déc.";
			default -> String.valueOf(month);
		};
	}

	private String monthNameEn(int month) {
		return switch (month) {
			case 1 -> "Jan";
			case 2 -> "Feb";
			case 3 -> "Mar";
			case 4 -> "Apr";
			case 5 -> "May";
			case 6 -> "Jun";
			case 7 -> "Jul";
			case 8 -> "Aug";
			case 9 -> "Sep";
			case 10 -> "Oct";
			case 11 -> "Nov";
			case 12 -> "Dec";
			default -> String.valueOf(month);
		};
	}
}
