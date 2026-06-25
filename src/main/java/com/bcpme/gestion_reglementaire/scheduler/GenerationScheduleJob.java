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
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class GenerationScheduleJob {

    private final GenerationScheduleRepository repository;
    private final GenerationColService generationColService;

    /**
     * 🔥 CALCUL UNIQUE DU PROCHAIN LANCEMENT
     */
    private LocalDateTime computeNextRun(GenerationSchedule s, LocalDate baseDate) {

        if (s.getHeureExecution() == null || baseDate == null) {
            return null;
        }

        return LocalDateTime.of(baseDate, s.getHeureExecution());
    }

    /**
     * 🔥 RATTRAPAGE AU DÉMARRAGE
     */
    @PostConstruct
    public void catchMissedExecutions() {

        System.out.println("=== Vérification des exécutions manquées ===");

        List<GenerationSchedule> schedules = repository.findByActiveTrue();

        for (GenerationSchedule schedule : schedules) {

            try {

                if (schedule.getDateDebut() == null || schedule.getHeureExecution() == null) {
                    continue;
                }

                int frequence = (schedule.getFrequenceJours() == null) ? 1 : schedule.getFrequenceJours();

                LocalDate prochaineDate;

                if (schedule.getDerniereExecution() == null) {
                    prochaineDate = schedule.getDateDebut().toLocalDate();
                } else {
                    prochaineDate = schedule.getDerniereExecution()
                            .toLocalDate()
                            .plusDays(frequence);
                }

                while (!prochaineDate.isAfter(LocalDate.now())) {

                    LocalDateTime execution = LocalDateTime.of(
                            prochaineDate,
                            schedule.getHeureExecution()
                    );

                    if (schedule.getDateFin() != null
                            && execution.isAfter(schedule.getDateFin())) {
                        break;
                    }

                    System.out.println("RATTRAPAGE : " + schedule.getNom());

                    generationColService.lancerGeneration("PLANIFICATEUR", schedule.getId());

                    schedule.setDerniereExecution(execution);

                    // 🔥 NEXT RUN = prochaine exécution future
                    LocalDate nextDate = prochaineDate.plusDays(frequence);

                    schedule.setNextRunDate(
                            computeNextRun(schedule, nextDate)
                    );

                    repository.save(schedule);

                    prochaineDate = nextDate;
                }

            } catch (Exception e) {
                System.err.println("Erreur rattrapage : " + schedule.getNom());
                e.printStackTrace();
            }
        }
    }

    /**
     * 🔥 EXÉCUTION PLANIFIÉE (TOUTES LES MINUTES)
     */
    @Scheduled(fixedRate = 60000)
    public void executeSchedules() {

        LocalDateTime now = LocalDateTime.now();

        List<GenerationSchedule> schedules = repository.findByActiveTrue();

        for (GenerationSchedule schedule : schedules) {

            try {

                if (schedule.getDateDebut() != null
                        && now.isBefore(schedule.getDateDebut())) {
                    continue;
                }

                if (schedule.getDateFin() != null
                        && now.isAfter(schedule.getDateFin())) {
                    continue;
                }

                LocalTime heure = schedule.getHeureExecution();

                if (heure == null) {
                    continue;
                }

                if (now.getHour() == heure.getHour()
                        && now.getMinute() == heure.getMinute()) {

                    // éviter double exécution jour même
                    if (schedule.getDerniereExecution() != null
                            && schedule.getDerniereExecution().toLocalDate().equals(LocalDate.now())) {
                        continue;
                    }

                    int frequence = (schedule.getFrequenceJours() == null) ? 1 : schedule.getFrequenceJours();

                    System.out.println("EXÉCUTION : " + schedule.getNom());

                    generationColService.lancerGeneration("PLANIFICATEUR", schedule.getId());

                    schedule.setDerniereExecution(now);

                    LocalDate nextDate = now.toLocalDate().plusDays(frequence);

                    schedule.setNextRunDate(
                            computeNextRun(schedule, nextDate)
                    );

                    repository.save(schedule);
                }

            } catch (Exception e) {
                System.err.println("Erreur exécution : " + schedule.getNom());
                e.printStackTrace();
            }
        }
    }
}