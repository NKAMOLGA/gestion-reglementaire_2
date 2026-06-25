package com.bcpme.gestion_reglementaire.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "generation_schedule")
@Getter
@Setter
public class GenerationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @Column(name = "frequence_jours")
    private Integer frequenceJours;

    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "heure_execution")
    private LocalTime heureExecution;

    private Boolean active;

    @Column(name = "derniere_execution")
    private LocalDateTime derniereExecution;

    /**
     * 🔥 PROCHAIN LANCEMENT (AFFICHAGE UI)
     */
    @Column(name = "next_run_date")
    private LocalDateTime nextRunDate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}