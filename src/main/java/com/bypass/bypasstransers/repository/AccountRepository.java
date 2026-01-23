
package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findByName(String name);
    
}

