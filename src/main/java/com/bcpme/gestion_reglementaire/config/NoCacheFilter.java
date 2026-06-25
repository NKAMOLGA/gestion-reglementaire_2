package com.bcpme.gestion_reglementaire.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Empêche la mise en cache des pages authentifiées dans le navigateur
 * (le bouton Retour ne doit pas réafficher une page protégée après déconnexion).
 */
@Component
public class NoCacheFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		boolean authenticated = auth != null
				&& auth.isAuthenticated()
				&& !"anonymousUser".equals(auth.getPrincipal());

		if (authenticated) {
			response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);
		}

		filterChain.doFilter(request, response);
	}
}
