package com.bypass.bypasstransers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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
        // In Spring Security 6 / Boot 4 the DaoAuthenticationProvider constructor accepts
        // a UserDetailsService. Use that constructor and set the password encoder.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        http
            .userDetailsService(userDetailsService)
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize
                // Only permit the login page, password reset pages, static resources and public pages.
                .requestMatchers("/", "/about", "/register", "/contact", "/login", "/forgot-password", "/reset", "/css/**", "/js/**", "/images/**", "/error", "/debug/**").permitAll()
                // Role-based endpoints examples
                // allow ADMIN, SUPER_ADMIN and SUPERVISOR to access /admin/** so senior staff can manage users
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "SUPERVISOR")
                .requestMatchers("/users/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/super/**").hasRole("SUPER_ADMIN")
                // management endpoints accessible to ADMIN, SUPER_ADMIN and SUPERVISOR
                .requestMatchers("/manage/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "SUPERVISOR")
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/app", true) // After login redirect to dashboard
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        // Ensure the DaoAuthenticationProvider with our PasswordEncoder is used
        http.authenticationProvider(daoAuthenticationProvider(userDetailsService, passwordEncoder()));
        return http.build();
    }

}