package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.OfflineTransaction;
import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.service.OfflineSyncService;
import com.bypass.bypasstransers.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for sync operations
 * @author Vickeller.01
 */
@RestController
@RequestMapping("/api/sync")
public class SyncController {

    @Autowired
    private OfflineSyncService offlineSyncService;
    
    @Autowired
    private SecurityService securityService;

    /**
     * Sync offline transactions to main system
     */
    @PostMapping("/transactions")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> syncTransactions(@RequestBody List<OfflineTransaction> offlineTransactions) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            int successCount = 0;
            int failedCount = 0;
            
            for (OfflineTransaction offlineTx : offlineTransactions) {
                try {
                    offlineSyncService.syncSingleTransaction(offlineTx);
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    System.err.println("Failed to sync transaction: " + e.getMessage());
                }
            }
            
            response.put("success", true);
            response.put("successCount", successCount);
            response.put("failedCount", failedCount);
            response.put("message", "Sync completed: " + successCount + " successful, " + failedCount + " failed");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Sync failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get sync statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getSyncStats() {
        try {
            Map<String, Long> stats = offlineSyncService.getSyncStatistics();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get sync stats: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Get user's pending offline transactions
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPendingTransactions() {
        try {
            String username = securityService.getCurrentUser().getUsername();
            List<OfflineTransaction> pendingTransactions = offlineSyncService.getUserOfflineTransactions(username);
            pendingTransactions.removeIf(tx -> !"PENDING".equals(tx.getSyncStatus()));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pendingTransactions);
            response.put("count", pendingTransactions.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get pending transactions: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * Trigger sync for all pending transactions
     */
    @PostMapping("/trigger")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> triggerSync() {
        try {
            Map<String, Object> result = offlineSyncService.syncAllPendingTransactions();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", result);
            response.put("message", "Sync completed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Sync trigger failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
