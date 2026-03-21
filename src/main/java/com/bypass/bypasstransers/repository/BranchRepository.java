package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    List<Branch> findByIsActive(boolean active);
    List<Branch> findByCountry(String country);
    boolean existsByName(String name);
}
