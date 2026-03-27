package com.bypass.bypasstransers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        http
            .userDetailsService(userDetailsService)
            // CSRF enabled - Thymeleaf automatically injects CSRF tokens in forms
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**") // Ignore for REST API endpoints only
            )
            // HTTP Security Headers
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()) // Allow iframes from same origin only
                .xssProtection(xss -> xss.disable()) // Disable XSS protection (handled by CSP)
                .contentTypeOptions(cto -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000) // 1 year
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/about", "/register", "/contact", "/login",
                    "/forgot-password", "/reset", "/css/**", "/js/**",
                    "/images/**", "/img/**", "/videos/**", "/error").permitAll()
                // Actuator health endpoint public, others require auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("SUPER_ADMIN")
                // Debug endpoints disabled in production
                .requestMatchers("/debug/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "SUPERVISOR")
                .requestMatchers("/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/super/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/manage/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "SUPERVISOR")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/app", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1) // Prevent multiple concurrent sessions per user
                .maxSessionsPreventsLogin(false) // Kick old session when new one starts
            );

        http.authenticationProvider(daoAuthenticationProvider(userDetailsService, passwordEncoder()));
        return http.build();
    }

}
