package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // Find a user by username
    User findByUsername(String username);

    // Case-insensitive username lookup (PostgreSQL is case-sensitive by default)
    User findByUsernameIgnoreCase(String username);

    // Find a user by email (used for password reset)
    User findByEmail(String email);

    // Case-insensitive email lookup
    User findByEmailIgnoreCase(String email);
}