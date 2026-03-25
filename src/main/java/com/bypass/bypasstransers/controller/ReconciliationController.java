package com.bypass.bypasstransers.controller;

import com.bypass.bypasstransers.model.DailyReconciliation;
import com.bypass.bypasstransers.service.ReconsiliationService;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to trigger reconciliation operations
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
    public DailyReconciliation reconcileWallet(@RequestBody ReconcileWalletRequest req) {
        return service.reconcileWallet(req.getWalletId(), req.getActualBalance());
    }

    public static class ReconcileWalletRequest {
        private Long walletId;
        private double actualBalance;

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
