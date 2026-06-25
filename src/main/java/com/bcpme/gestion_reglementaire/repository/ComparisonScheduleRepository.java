package com.bcpme.gestion_reglementaire.repository;

import com.bcpme.gestion_reglementaire.entity.ComparisonSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComparisonScheduleRepository extends JpaRepository<ComparisonSchedule, Long> {

	List<ComparisonSchedule> findByActiveTrue();
}
