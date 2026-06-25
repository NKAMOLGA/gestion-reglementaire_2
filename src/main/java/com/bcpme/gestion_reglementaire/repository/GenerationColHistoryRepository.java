package com.bcpme.gestion_reglementaire.repository;

import com.bcpme.gestion_reglementaire.entity.GenerationColHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GenerationColHistoryRepository extends JpaRepository<GenerationColHistory, Long> {

    // DASHBOARD

    long countByStatut(String statut);

    List<GenerationColHistory> findTop10ByOrderByDateGenerationDesc();

    @Query("SELECT DISTINCT h.utilisateur FROM GenerationColHistory h")
    List<String> findDistinctUtilisateurs();


    // HISTORIQUE

    @Query(value = """
        SELECT *
        FROM generation_col_history
        WHERE
            (:utilisateur IS NULL OR utilisateur ILIKE CONCAT('%', :utilisateur, '%'))
        AND (:nomFichier IS NULL OR nom_fichier ILIKE CONCAT('%', :nomFichier, '%'))
        ORDER BY date_generation DESC
        """,
        nativeQuery = true)
    List<GenerationColHistory> searchHistory(
            @Param("utilisateur") String utilisateur,
            @Param("nomFichier") String nomFichier
    );

    List<GenerationColHistory> findByStatutAndNomFichierIsNotNullOrderByDateGenerationDesc(String statut);

    List<GenerationColHistory> findByStatutAndUtilisateurAndNomFichierIsNotNullOrderByDateGenerationDesc(
            String statut, String utilisateur);

    List<GenerationColHistory> findByStatutAndDateGenerationBetweenAndNomFichierIsNotNullOrderByDateGenerationAsc(
            String statut, LocalDateTime debut, LocalDateTime fin);
}