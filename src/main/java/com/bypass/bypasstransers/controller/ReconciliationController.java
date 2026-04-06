package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.DailyReconciliation;
import com.bypass.bypasstransers.service.ReconsiliationService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for reconciliation operations
 */
@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    @Autowired
    private ReconsiliationService service;

    @PostMapping("/daily")
    public DailyReconciliation reconcile(@RequestBody ReconcileRequest req) {
        LocalDate date = req.getDate() != null ? req.getDate() : LocalDate.now();
        return service.reconcile(date, req.getActualBalance());
    }

    @PostMapping("/wallet")
    public ResponseEntity<?> reconcileWallet(@RequestBody ReconcileWalletRequest req) {
        try {
            DailyReconciliation result = service.reconcileWallet(
                    req.getWalletId(), req.getActualBalance(), req.getNotes());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public static class ReconcileWalletRequest {
        private Long walletId;
        private double actualBalance;
        private String notes;

        public Long getWalletId() {
            return walletId;
        }

        public void setWalletId(Long walletId) {
            this.walletId = walletId;
        }

        public double getActualBalance() {
            return actualBalance;
        }

        public void setActualBalance(double actualBalance) {
            this.actualBalance = actualBalance;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public static class ReconcileRequest {
        private LocalDate date;
        private double actualBalance;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public double getActualBalance() {
            return actualBalance;
        }

        public void setActualBalance(double actualBalance) {
            this.actualBalance = actualBalance;
        }
    }
}
