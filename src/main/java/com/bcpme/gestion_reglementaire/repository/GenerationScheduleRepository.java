package com.bcpme.gestion_reglementaire.repository;

import com.bcpme.gestion_reglementaire.entity.GenerationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenerationScheduleRepository
        extends JpaRepository<GenerationSchedule, Long> {

    List<GenerationSchedule> findByActiveTrue();

}