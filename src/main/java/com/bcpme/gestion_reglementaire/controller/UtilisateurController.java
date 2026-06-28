package com.bcpme.gestion_reglementaire.controller;

import com.bcpme.gestion_reglementaire.entity.Utilisateur;
import com.bcpme.gestion_reglementaire.repository.RoleRepository;
import com.bcpme.gestion_reglementaire.service.UtilisateurService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/utilisateurs")
public class UtilisateurController {

	private final UtilisateurService utilisateurService;
	private final RoleRepository roleRepository;

	public UtilisateurController(UtilisateurService utilisateurService,
								 RoleRepository roleRepository) {
		this.utilisateurService = utilisateurService;
		this.roleRepository = roleRepository;
	}

	@GetMapping
	public String liste(Model model) {
		model.addAttribute("utilisateurs", utilisateurService.findAll());
		return "utilisateurs";
	}

	@GetMapping("/nouveau")
	public String nouveau(Model model) {
		model.addAttribute("utilisateur", new Utilisateur());
		model.addAttribute("roles", roleRepository.findAll());
		return "ajouter-utilisateur";
	}

	@PostMapping("/save")
	public String save(@ModelAttribute Utilisateur utilisateur) {
		Utilisateur existing = null;

		if (utilisateur.getId() != null) {
			existing = utilisateurService.findById(utilisateur.getId()).orElse(null);
		}

		if (existing != null) {
			existing.setUsername(utilisateur.getUsername());
			existing.setPassword(utilisateur.getPassword());
			existing.setRole(utilisateur.getRole());

			Boolean nouvelEtat = utilisateur.getActif() != null ? utilisateur.getActif() : true;
			existing.setActif(nouvelEtat);

			if (Boolean.TRUE.equals(nouvelEtat)) {
				existing.setTentativesConnexion(0);
			}

			utilisateurService.save(existing);
		} else {
			utilisateur.setActif(utilisateur.getActif() != null ? utilisateur.getActif() : true);
			utilisateurService.save(utilisateur);
		}

		return "redirect:/utilisateurs";
	}

	@GetMapping("/view/{id}")
	public String view(@PathVariable Long id, Model model) {
		Utilisateur utilisateur = utilisateurService.findById(id)
				.orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));
		model.addAttribute("utilisateur", utilisateur);
		return "voir-utilisateur";
	}

	@GetMapping("/edit/{id}")
	public String edit(@PathVariable Long id, Model model) {
		Utilisateur utilisateur = utilisateurService.findById(id)
				.orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));
		model.addAttribute("utilisateur", utilisateur);
		model.addAttribute("roles", roleRepository.findAll());
		return "modifier-utilisateur";
	}

	@GetMapping("/bloquer/{id}")
	public String bloquer(@PathVariable Long id) {
		Utilisateur utilisateur = utilisateurService.findById(id)
				.orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));
		utilisateur.setActif(false);
		utilisateurService.save(utilisateur);
		return "redirect:/utilisateurs";
	}

	@GetMapping("/debloquer/{id}")
	public String debloquer(@PathVariable Long id) {
		Utilisateur utilisateur = utilisateurService.findById(id)
				.orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));
		utilisateur.setActif(true);
		utilisateur.setTentativesConnexion(0);
		utilisateurService.save(utilisateur);
		return "redirect:/utilisateurs";
	}

	@GetMapping("/delete/{id}")
	public String delete(@PathVariable Long id) {
		utilisateurService.delete(id);
		return "redirect:/utilisateurs";
	}
}
