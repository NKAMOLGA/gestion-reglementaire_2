package com.bcpme.gestion_reglementaire.repository;

import com.bcpme.gestion_reglementaire.entity.ColComparisonHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ColComparisonHistoryRepository extends JpaRepository<ColComparisonHistory, Long> {

	List<ColComparisonHistory> findAllByOrderByDateComparaisonDesc();

	List<ColComparisonHistory> findByComparisonScheduleIdOrderByDateComparaisonDesc(Long comparisonScheduleId);

	List<ColComparisonHistory> findByComparisonScheduleIdOrderByDateComparaisonAsc(Long comparisonScheduleId);

	List<ColComparisonHistory> findByGenerationScheduleIdAndModeOrderByDateComparaisonAsc(
			Long generationScheduleId, String mode);

	List<ColComparisonHistory> findByGenerationScheduleIdAndModeOrderByDateComparaisonDesc(
			Long generationScheduleId, String mode);
}
