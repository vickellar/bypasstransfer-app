package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DataValidationService provides data integrity checks before analytics calculations.
 * Ensures that calculations are based on valid, consistent data.
 */
@Service
public class DataValidationService {
    private static final Logger logger = LoggerFactory.getLogger(DataValidationService.class);

    /**
     * Validates a transaction for analytics use.
     * Checks: amount > 0, fees >= 0, amount >= fees, date is not null
     */
    public ValidationResult validateTransaction(Transaction tx) {
        if (tx == null) {
            return new ValidationResult(false, "Transaction is null");
        }

        if (tx.getAmount() == null || tx.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return new ValidationResult(false, "Invalid transaction amount: " + tx.getAmount());
        }

        BigDecimal fees = tx.getFee() != null ? tx.getFee() : BigDecimal.ZERO;
        if (fees.compareTo(BigDecimal.ZERO) < 0) {
            return new ValidationResult(false, "Negative fees not allowed: " + fees);
        }

        if (tx.getAmount().compareTo(fees) < 0) {
            return new ValidationResult(false, "Fees exceed transaction amount");
        }

        if (tx.getDate() == null) {
            return new ValidationResult(false, "Transaction date is null");
        }

        return new ValidationResult(true, "Valid");
    }

    /**
     * Validates a wallet for analytics use.
     * Checks: balance >= 0, balance is not null
     */
    public ValidationResult validateWallet(Wallet wallet) {
        if (wallet == null) {
            return new ValidationResult(false, "Wallet is null");
        }

        if (wallet.getBalance() == null) {
            return new ValidationResult(false, "Wallet balance is null");
        }

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            return new ValidationResult(false, "Negative wallet balance: " + wallet.getBalance());
        }

        return new ValidationResult(true, "Valid");
    }

    /**
     * Filters out invalid transactions from a list.
     * Logs invalid entries for debugging.
     */
    public List<Transaction> filterValidTransactions(List<Transaction> transactions) {
        List<Transaction> validTransactions = new ArrayList<>();
        int invalidCount = 0;

        for (Transaction tx : transactions) {
            ValidationResult result = validateTransaction(tx);
            if (result.isValid()) {
                validTransactions.add(tx);
            } else {
                invalidCount++;
                logger.warn("Filtered out invalid transaction ID={}: {}", tx.getId(), result.getMessage());
            }
        }

        if (invalidCount > 0) {
            logger.info("Filtered {} invalid transactions out of {}", invalidCount, transactions.size());
        }

        return validTransactions;
    }

    /**
     * Validates that a collection of transactions has consistent data.
     */
    public ValidationResult validateTransactionCollection(List<Transaction> transactions) {
        if (transactions == null) {
            return new ValidationResult(false, "Transaction list is null");
        }

        if (transactions.isEmpty()) {
            return new ValidationResult(true, "Empty collection is valid (no data to validate)");
        }

        for (Transaction tx : transactions) {
            ValidationResult result = validateTransaction(tx);
            if (!result.isValid()) {
                return result;
            }
        }

        return new ValidationResult(true, "Valid");
    }

    /**
     * Checks for NaN or infinite values in BigDecimal calculations.
     */
    public ValidationResult validateCalculatedAmount(BigDecimal amount, String context) {
        if (amount == null) {
            return new ValidationResult(false, "Calculated amount is null (" + context + ")");
        }

        try {
            // Check for NaN representation
            if (amount.toString().contains("NaN") || amount.toString().contains("Infinity")) {
                return new ValidationResult(false, "Invalid numeric value: " + amount);
            }
            return new ValidationResult(true, "Valid");
        } catch (Exception e) {
            return new ValidationResult(false, "Error validating amount: " + e.getMessage());
        }
    }

    /**
     * Inner class for validation results.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
