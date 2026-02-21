package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    EmailVerificationToken findByToken(String token);
    void deleteByUser(com.bypass.bypasstransers.model.User user);
}
