package com.bcpme.gestion_reglementaire.service;

import com.bcpme.gestion_reglementaire.dto.ArchiveFileDto;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ArchiveService {

    private static final String DOSSIER_ARCHIVES =
            "C:\\Users\\toses\\OneDrive\\Desktop\\Dossiers\\NK\\archives_col";

    public List<ArchiveFileDto> getAllArchives() {

        File folder = new File(DOSSIER_ARCHIVES);

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".col"));

        if (files == null) return new ArrayList<>();

        return Arrays.stream(files)
                .sorted((a, b) -> Long.compare(b.lastModified(), a.lastModified()))
                .map(f -> new ArchiveFileDto(f.getName(), f.length()))
                .toList();
    }
}