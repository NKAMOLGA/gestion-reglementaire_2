package com.bcpme.gestion_reglementaire.controller;

import com.bcpme.gestion_reglementaire.dto.ArchiveFileDto;
import com.bcpme.gestion_reglementaire.service.ArchiveService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
public class DownloadController {

    private final ArchiveService archiveService;

    private static final String DOSSIER_ARCHIVES =
            "C:\\Users\\toses\\OneDrive\\Desktop\\Dossiers\\NK\\archives_col";

    public DownloadController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    // Liste des fichiers archivés
    @GetMapping("/downloads")
    public String downloads(Model model) {

        List<ArchiveFileDto> fichiers = archiveService.getAllArchives();

        model.addAttribute("fichiers", fichiers);

        return "downloads";
    }

    // Télécharger un fichier
    @GetMapping("/downloads/file")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filename) throws Exception {

        Path path = Paths.get(DOSSIER_ARCHIVES, filename);

        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    // Voir contenu du fichier
    @GetMapping("/downloads/view")
    public String viewFile(@RequestParam String filename, Model model) {

        try {
            Path path = Paths.get(DOSSIER_ARCHIVES, filename);

            if (!Files.exists(path)) {
                model.addAttribute("filename", filename);
                model.addAttribute("content", "❌ Fichier introuvable");
                return "file-view";
            }

            byte[] bytes = Files.readAllBytes(path);

            String content = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);

            model.addAttribute("filename", filename);
            model.addAttribute("content", content);

        } catch (Exception e) {

            model.addAttribute("filename", filename);
            model.addAttribute("content", "❌ Erreur : " + e.getMessage());
        }

        return "file-view";
    }
}