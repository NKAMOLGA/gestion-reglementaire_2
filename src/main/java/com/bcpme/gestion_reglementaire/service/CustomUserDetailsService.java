package com.bcpme.gestion_reglementaire.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bcpme.gestion_reglementaire.entity.Utilisateur;
import com.bcpme.gestion_reglementaire.repository.UtilisateurRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    public CustomUserDetailsService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Utilisateur utilisateur =
                utilisateurRepository.findByUsername(username);

        if (utilisateur == null) {
            throw new UsernameNotFoundException("Utilisateur introuvable");
        }

        return User.builder()
                .username(utilisateur.getUsername())
                .password(utilisateur.getPassword())
                .authorities(utilisateur.getRole().getName())
                .disabled(!utilisateur.getActif())
                .build();
    }

    public void resetTentatives(String username) {

        Utilisateur utilisateur =
                utilisateurRepository.findByUsername(username);

        if (utilisateur != null) {

            utilisateur.setTentativesConnexion(0);

            utilisateurRepository.save(utilisateur);
        }
    }
}