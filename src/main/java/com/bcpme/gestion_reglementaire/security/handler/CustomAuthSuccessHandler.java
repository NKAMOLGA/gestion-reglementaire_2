package com.bcpme.gestion_reglementaire.security.handler;

import com.bcpme.gestion_reglementaire.service.CustomUserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthSuccessHandler
        implements AuthenticationSuccessHandler {

    private final CustomUserDetailsService userDetailsService;

    public CustomAuthSuccessHandler(
            CustomUserDetailsService userDetailsService) {

        this.userDetailsService = userDetailsService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        userDetailsService.resetTentatives(
                authentication.getName());

        response.sendRedirect("/dashboard");
    }
}