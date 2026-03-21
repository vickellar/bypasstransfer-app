package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Branch;
import com.bypass.bypasstransers.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class BranchService {

    @Autowired
    private BranchRepository branchRepository;

    /**
     * Get all active branches
     */
    public List<Branch> getAllActiveBranches() {
        return branchRepository.findByIsActive(true);
    }

    /**
     * Get all branches (including inactive)
     */
    public List<Branch> getAllBranches() {
        return branchRepository.findAll();
    }

    /**
     * Get branch by ID
     */
    public Optional<Branch> getBranchById(Long id) {
        return branchRepository.findById(id);
    }

    /**
     * Get branches by country
     */
    public List<Branch> getBranchesByCountry(String country) {
        return branchRepository.findByCountry(country);
    }

    /**
     * Create new branch
     */
    @Transactional
    public Branch createBranch(Branch branch) {
        if (branchRepository.existsByName(branch.getName())) {
            throw new RuntimeException("Branch with name already exists");
        }
        branch.setActive(true);
        branch.setCreatedAt(java.time.LocalDateTime.now());
        return branchRepository.save(branch);
    }

    /**
     * Update existing branch
     */
    @Transactional
    public Branch updateBranch(Long id, Branch branchDetails) {
        Branch branch = branchRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Branch not found"));

        branch.setName(branchDetails.getName());
        branch.setCountry(branchDetails.getCountry());
        branch.setCurrency(branchDetails.getCurrency());
        branch.setAddress(branchDetails.getAddress());
        branch.setContactEmail(branchDetails.getContactEmail());
        branch.setContactPhone(branchDetails.getContactPhone());
        branch.setUpdatedAt(java.time.LocalDateTime.now());

        return branchRepository.save(branch);
    }

    /**
     * Deactivate branch (soft delete)
     */
    @Transactional
    public void deactivateBranch(Long id) {
        Branch branch = branchRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Branch not found"));
        
        branch.setActive(false);
        branch.setUpdatedAt(java.time.LocalDateTime.now());
        branchRepository.save(branch);
    }

    /**
     * Activate branch
     */
    @Transactional
    public void activateBranch(Long id) {
        Branch branch = branchRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Branch not found"));
        
        branch.setActive(true);
        branch.setUpdatedAt(java.time.LocalDateTime.now());
        branchRepository.save(branch);
    }

    /**
     * Check if branch name exists
     */
    public boolean branchNameExists(String name) {
        return branchRepository.existsByName(name);
    }
}
