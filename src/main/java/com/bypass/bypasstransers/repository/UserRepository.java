package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.User;
import com.bypass.bypasstransers.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    // Find users by username (returns list to handle potential duplicates)
    List<User> findByUsername(String username);

    // Case-insensitive username lookup (returns list to handle potential duplicates)
    List<User> findByUsernameIgnoreCase(String username);

    // Find a user by email (returns list to handle potential duplicates)
    List<User> findByEmail(String email);

    // Case-insensitive email lookup (returns list to handle potential duplicates)
    List<User> findByEmailIgnoreCase(String email);

    // Count users by role (used to prevent deleting last admin)
    long countByRole(Role role);

    // Find users by role
    List<User> findByRole(Role role);
}