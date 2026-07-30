package com.bypass.bypasstransers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import java.time.Duration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public LoginRateLimitFilter loginRateLimitFilter() {
        // 5 attempts per 15 minutes — same as your interceptor constants
        return new LoginRateLimitFilter(5, Duration.ofMinutes(15));
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        http.addFilterBefore(loginRateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
            .userDetailsService(userDetailsService)
            // CSRF enabled globally - Tightened for financial applications
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/actuator/health") // Allow simple health checks without CSRF
            )
            // HTTP Security Headers - Hardened
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin) // Prevent Clickjacking
                .contentTypeOptions(cto -> {}) // Prevent MIME sniffing
                .xssProtection(xss -> xss.headerValue(org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                        "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com https://cdn.jsdelivr.net data:; " +
                        "img-src 'self' data: https:; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'none';"
                    )
                )
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000) // 1 year
                    .preload(true)
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/actuator/health*")
                )
                .rememberMe(rememberMe -> rememberMe
                        .rememberMeCookieName("app-remember-me")
                        .useSecureCookie(true)
                        .alwaysRemember(false)
                        .tokenValiditySeconds(604800)    //7 day
                )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/about", "/register", "/contact", "/login",
                    "/forgot-password", "/reset", "/reset-password", "/css/**", "/js/**",
                    "/images/**", "/img/**", "/videos/**", "/error", "/favicon.ico").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("SUPER_ADMIN")
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
                    .sessionFixation(org.springframework.security.config.Customizer.withDefaults())
                    .maximumSessions(2) // Prevent multiple concurrent sessions
                    .maxSessionsPreventsLogin(false)
            );

        http.authenticationProvider(daoAuthenticationProvider(userDetailsService, passwordEncoder()));
        return http.build();
    }
}
