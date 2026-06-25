package com.bcpme.gestion_reglementaire.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "comparison_schedule")
@Getter
@Setter
public class ComparisonSchedule {

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

	private Boolean active;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
}
