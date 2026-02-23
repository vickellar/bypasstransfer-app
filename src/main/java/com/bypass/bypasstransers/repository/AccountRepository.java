
package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByName(String name);
    
    List<Account> findByOwnerId(Long ownerId);
    
    void deleteByOwnerId(Long ownerId);
}

