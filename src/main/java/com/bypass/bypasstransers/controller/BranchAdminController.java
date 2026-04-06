package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.Branch;
import com.bypass.bypasstransers.service.BranchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/branches")
public class BranchAdminController {

    @Autowired
    private BranchService branchService;

    /**
     * Show branch management page (Thymeleaf template)
     */
    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public String showBranchManagement(Model model) {
        model.addAttribute("title", "Branch Management");
        return "admin-branches";
    }

    /**
     * Get all active branches (REST API)
     */
    @GetMapping("/api")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAllBranches() {
        try {
            List<Branch> branches = branchService.getAllActiveBranches();
            return ResponseEntity.ok(branches);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching branches: " + e.getMessage());
        }
    }

    /**
     * Get all branches (including inactive)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getAllBranchesIncludingInactive() {
        try {
            List<Branch> branches = branchService.getAllBranches();
            return ResponseEntity.ok(branches);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching branches: " + e.getMessage());
        }
    }

    /**
     * Get branch by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> getBranchById(@PathVariable Long id) {
        try {
            return branchService.getBranchById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error fetching branch: " + e.getMessage());
        }
    }

    /**
     * Create new branch
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> createBranch(@RequestBody Branch branch) {
        try {
            if (branchService.branchNameExists(branch.getName())) {
                return ResponseEntity.badRequest().body("Branch with this name already exists");
            }
            
            Branch created = branchService.createBranch(branch);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating branch: " + e.getMessage());
        }
    }

    /**
     * Update existing branch
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> updateBranch(@PathVariable Long id, @RequestBody Branch branchDetails) {
        try {
            Branch updated = branchService.updateBranch(id, branchDetails);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error updating branch: " + e.getMessage());
        }
    }

    /**
     * Deactivate branch
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> deactivateBranch(@PathVariable Long id) {
        try {
            branchService.deactivateBranch(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Branch deactivated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deactivating branch: " + e.getMessage());
        }
    }

    /**
     * Activate branch
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> activateBranch(@PathVariable Long id) {
        try {
            branchService.activateBranch(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Branch activated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error activating branch: " + e.getMessage());
        }
    }
}
