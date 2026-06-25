package com.bcpme.gestion_reglementaire.repository;

import com.bcpme.gestion_reglementaire.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilisateurRepository
        extends JpaRepository<Utilisateur, Long> {

    Utilisateur findByUsername(String username);
}