package com.bcpme.gestion_reglementaire.config;

import com.bcpme.gestion_reglementaire.security.handler.CustomAuthFailureHandler;
import com.bcpme.gestion_reglementaire.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.bcpme.gestion_reglementaire.security.handler.CustomAuthSuccessHandler;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthFailureHandler customAuthFailureHandler;
    private final CustomAuthSuccessHandler customAuthSuccessHandler;
    private final NoCacheFilter noCacheFilter;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            CustomAuthFailureHandler customAuthFailureHandler,
            CustomAuthSuccessHandler customAuthSuccessHandler,
            NoCacheFilter noCacheFilter) {

        this.userDetailsService = userDetailsService;
        this.customAuthFailureHandler = customAuthFailureHandler;
        this.customAuthSuccessHandler = customAuthSuccessHandler;
        this.noCacheFilter = noCacheFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {

        DaoAuthenticationProvider auth =
                new DaoAuthenticationProvider();

        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder());

        return auth;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .addFilterAfter(noCacheFilter, UsernamePasswordAuthenticationFilter.class)

            .authenticationProvider(authProvider())

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                        "/",
                        "/login",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                ).permitAll()

                .requestMatchers("/utilisateurs/**")
                .hasAuthority("DSIO")

                .anyRequest()
                .authenticated()
            )

            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .successHandler(customAuthSuccessHandler)
                    .failureHandler(customAuthFailureHandler)
                    .permitAll()
            )

            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .clearAuthentication(true)
                    .permitAll()
            )

            .headers(headers -> headers
                    .cacheControl(cache -> cache.disable())
                    .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }
}