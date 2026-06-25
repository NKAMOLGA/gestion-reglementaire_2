package com.bcpme.gestion_reglementaire.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "col_comparison_history")
@Getter
@Setter
public class ColComparisonHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "date_comparaison")
	private LocalDateTime dateComparaison;

	private String utilisateur;

	@Column(name = "fichier_a")
	private String fichierA;

	@Column(name = "fichier_b")
	private String fichierB;

	@Column(name = "date_fichier_a")
	private LocalDate dateFichierA;

	@Column(name = "date_fichier_b")
	private LocalDate dateFichierB;

	@Column(name = "comptes_a")
	private Integer comptesA;

	@Column(name = "comptes_b")
	private Integer comptesB;

	private String mode;

	@Column(name = "comparison_schedule_id")
	private Long comparisonScheduleId;

	@Column(name = "generation_schedule_id")
	private Long generationScheduleId;

	@Column(name = "added_count")
	private Integer addedCount;

	@Column(name = "removed_count")
	private Integer removedCount;

	@Column(name = "modified_count")
	private Integer modifiedCount;

	@Column(name = "unchanged_count")
	private Integer unchangedCount;

	@Column(name = "total_delta")
	private BigDecimal totalDelta;

	@Column(name = "details_json", columnDefinition = "TEXT")
	private String detailsJson;
}
