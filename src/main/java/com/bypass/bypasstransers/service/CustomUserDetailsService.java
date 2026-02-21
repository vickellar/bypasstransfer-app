package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || (username = username.trim()).isEmpty()) {
            throw new UsernameNotFoundException("Username cannot be empty");
        }
        // Try username first (case-insensitive), then email (allows login with either)
        User user = userRepository.findByUsernameIgnoreCase(username);
        if (user == null) {
            user = userRepository.findByEmailIgnoreCase(username);
        }
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        String role = user.getRole() != null ? user.getRole().name() : "STAFF";
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}
