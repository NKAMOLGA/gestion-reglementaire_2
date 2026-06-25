package com.bcpme.gestion_reglementaire.service;

import com.bcpme.gestion_reglementaire.entity.GenerationColHistory;
import com.bcpme.gestion_reglementaire.repository.GenerationColHistoryRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class GenerationColService {

    private final GenerationColHistoryRepository repository;
    private final ColArchiveHelper colArchiveHelper;

    private static final String SCRIPT_BAT =
            "C:\\Users\\toses\\OneDrive\\Desktop\\Dossiers\\NK\\Nouveau dossier (2)\\NK\\essai_0.1\\essai\\essai_run.bat";

    private static final String DOSSIER_SOURCE =
    		"C:\\Users\\toses\\OneDrive\\Desktop\\Dossiers\\NK\\archives_col";

    private static final String EXTENSION = ".col";

    private static final String DOSSIER_ARCHIVES =
            "C:\\Users\\toses\\OneDrive\\Desktop\\Dossiers\\NK\\archives_col";

    public GenerationColService(
            GenerationColHistoryRepository repository,
            ColArchiveHelper colArchiveHelper) {

        this.repository = repository;
        this.colArchiveHelper = colArchiveHelper;
    }

    public void lancerGeneration(String utilisateur) {
        lancerGeneration(utilisateur, null);
    }

    public void lancerGeneration(String utilisateur, Long generationScheduleId) {

        GenerationColHistory history = new GenerationColHistory();

        LocalDateTime generationTime = LocalDateTime.now();
        LocalDate businessDate = generationTime.toLocalDate().minusDays(1);

        try {

            System.out.println("=================================");
            System.out.println("LANCEMENT GENERATION COL");
            System.out.println("=================================");

            // 1. Exécution du BAT Talend

            ProcessBuilder pb = new ProcessBuilder(
                    "cmd",
                    "/c",
                    SCRIPT_BAT
            );

            pb.redirectErrorStream(true);

            Process process = pb.start();

            int codeRetour = process.waitFor();

            System.out.println("Code retour Talend = " + codeRetour);

            if (codeRetour != 0) {
                throw new RuntimeException(
                        "Erreur exécution Talend (code " + codeRetour + ")"
                );
            }

            // 2. Recherche du dernier fichier COL généré

            File dossier = new File(DOSSIER_SOURCE);

            if (!dossier.exists()) {
                throw new RuntimeException(
                        "Dossier introuvable : " + DOSSIER_SOURCE
                );
            }

            File[] fichiers = dossier.listFiles((dir, name) ->
                    name.toLowerCase().endsWith(EXTENSION));

            if (fichiers == null || fichiers.length == 0) {
                throw new RuntimeException(
                        "Aucun fichier .col trouvé dans : "
                                + DOSSIER_SOURCE
                );
            }

            File source = java.util.Arrays.stream(fichiers)
                    .max(Comparator.comparingLong(File::lastModified))
                    .orElseThrow();

            System.out.println("Fichier détecté : " + source.getAbsolutePath());

            // 3. Lecture du fichier

            List<String> originalLines =
                    colArchiveHelper.readLines(source.toPath());

            if (originalLines.isEmpty()) {
                throw new RuntimeException("Fichier COL vide");
            }

            // 4. Extraction du code institution

            String institutionCode =
                    colArchiveHelper.extractInstitutionCode(originalLines);

            // 5. Préparation du contenu archive

            List<String> archiveLines =
                    colArchiveHelper.prepareArchiveContent(
                            originalLines,
                            businessDate
                    );

            // 6. Nom du fichier archive

            String nomArchive =
                    colArchiveHelper.buildArchiveFileName(
                            institutionCode,
                            businessDate,
                            generationTime
                    );

            Path archivePath =
                    Path.of(DOSSIER_ARCHIVES, nomArchive);

            System.out.println(
                    "Archive créée : "
                            + archivePath.toAbsolutePath()
            );

            // 7. Création du dossier archive

            Files.createDirectories(
                    Path.of(DOSSIER_ARCHIVES)
            );

            // 8. Écriture du fichier archive

            colArchiveHelper.writeLines(
                    archivePath,
                    archiveLines
            );

            // 9. Historique SUCCESS

            history.setNomFichier(nomArchive);
            history.setCheminFichier(
                    archivePath.toAbsolutePath().toString()
            );
            history.setDateGeneration(generationTime);
            history.setUtilisateur(utilisateur);
            history.setGenerationScheduleId(generationScheduleId);
            history.setStatut("SUCCESS");
            history.setMessage(
                    "Génération réussie - date métier : "
                            + businessDate
            );

        } catch (Exception e) {

            e.printStackTrace();

            history.setDateGeneration(generationTime);
            history.setUtilisateur(utilisateur);
            history.setGenerationScheduleId(generationScheduleId);
            history.setStatut("ERROR");
            history.setMessage(
                    e.getClass().getSimpleName()
                            + " : "
                            + e.getMessage()
            );
        }

        repository.save(history);
    }
}