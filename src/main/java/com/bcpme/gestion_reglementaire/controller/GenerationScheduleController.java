package com.bcpme.gestion_reglementaire.controller;

import com.bcpme.gestion_reglementaire.entity.GenerationSchedule;
import com.bcpme.gestion_reglementaire.repository.GenerationScheduleRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/schedules")
public class GenerationScheduleController {

    private final GenerationScheduleRepository repository;

    public GenerationScheduleController(GenerationScheduleRepository repository) {
        this.repository = repository;
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
        model.addAttribute("schedule", new GenerationSchedule());
        return "schedules/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute GenerationSchedule schedule) {

        // uniquement lors de la création
        if (schedule.getId() == null) {

            if (schedule.getDateDebut() != null
                    && schedule.getHeureExecution() != null) {

                schedule.setNextRunDate(
                        LocalDateTime.of(
                                schedule.getDateDebut().toLocalDate(),
                                schedule.getHeureExecution()
                        )
                );
            }
        } else {

            // conserver les dates existantes
            GenerationSchedule existing = repository.findById(schedule.getId())
                    .orElseThrow();

            schedule.setDerniereExecution(existing.getDerniereExecution());
            schedule.setNextRunDate(existing.getNextRunDate());
        }
        if (schedule.getDateDebut() != null
                && schedule.getHeureExecution() != null) {

            schedule.setNextRunDate(
                    LocalDateTime.of(
                            schedule.getDateDebut().toLocalDate(),
                            schedule.getHeureExecution()
                    )
            );

            System.out.println("NEXT RUN = " + schedule.getNextRunDate());
        }
        repository.save(schedule);

        return "redirect:/schedules";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {

        GenerationSchedule schedule = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Planification introuvable : " + id));

        model.addAttribute("schedule", schedule);
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

        schedule.setActive(!schedule.getActive());

        repository.save(schedule);
        return "redirect:/schedules";
    }
}