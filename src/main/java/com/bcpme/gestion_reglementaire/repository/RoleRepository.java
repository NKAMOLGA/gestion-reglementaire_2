package com.bcpme.gestion_reglementaire.repository;

import com.bcpme.gestion_reglementaire.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository
        extends JpaRepository<Role, Long> {
}