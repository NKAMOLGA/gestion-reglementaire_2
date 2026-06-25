package com.bcpme.gestion_reglementaire.service;

import com.bcpme.gestion_reglementaire.entity.Utilisateur;
import com.bcpme.gestion_reglementaire.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll();
    }

    public Utilisateur save(Utilisateur utilisateur) {
        return utilisateurRepository.save(utilisateur);
    }

    public Optional<Utilisateur> findById(Long id) {
        return utilisateurRepository.findById(id);
    }

    public void delete(Long id) {
        utilisateurRepository.deleteById(id);
    }

    public Utilisateur findByUsername(String username) {
        return utilisateurRepository.findByUsername(username);
    }
}