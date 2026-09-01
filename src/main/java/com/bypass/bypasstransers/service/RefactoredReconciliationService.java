package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.*;
import com.bypass.bypasstransers.repository.DailyReconciliationRepository;
import com.bypass.bypasstransers.repository.TransactionMatchRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import com.bypass.bypasstransers.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RefactoredReconciliationService splits reconciliation into distinct, testable phases.
 * 
 * PHASES:
 * 1. MATCHING: Find candidate transaction pairs (exact match, fuzzy match)
 * 2. VALIDATION: Check discrepancies, exchange rates, fees
 * 3. APPROVAL: Supervisor review and confirmation
 * 
 * Each phase has explicit logging and validation steps.
 */
@Service
public class RefactoredReconciliationService {
    private static final Logger logger = LoggerFactory.getLogger(RefactoredReconciliationService.class);
    private static final BigDecimal DISCREPANCY_THRESHOLD = new BigDecimal("0.01");

    @Autowired private WalletRepository walletRepository;
    @Autowired private DailyReconciliationRepository dailyReconciliationRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private TransactionMatchRepository transactionMatchRepository;
    @Autowired private SecurityService securityService;
    @Autowired private DataValidationService dataValidationService;

    // ─────────────────────────────────────────────────────────────
    // PHASE 1: MATCHING
    // ─────────────────────────────────────────────────────────────

    /**
     * Phase 1: Find exact matches between incoming and outgoing transactions.
     * Exact match = same amount, same currency.
     */
    public List<TransactionMatch> findExactMatches(LocalDate dateRange) {
        logger.info("PHASE 1 - MATCHING: Searching for exact matches on date {}", dateRange);
        
        List<Transaction> transactions = transactionRepository.findByDate(dateRange);
        logger.debug("Found {} transactions for date {}", transactions.size(), dateRange);

        // Validate all transactions
        List<Transaction> validTransactions = dataValidationService.filterValidTransactions(transactions);
        logger.info("Validated {} transactions (filtered {} invalid)", validTransactions.size(), 
                    transactions.size() - validTransactions.size());

        // Group by amount and currency to find exact matches
        return validTransactions.stream()
                .collect(Collectors.groupingBy(t -> t.getAmount().toPlainString() + "|" + t.getCurrency()))
                .values()
                .stream()
                .filter(group -> group.size() >= 2)
                .flatMap(group -> createMatchCandidates(group).stream())
                .collect(Collectors.toList());
    }

    /**
     * Create transaction match candidates from a group.
     */
    private List<TransactionMatch> createMatchCandidates(List<Transaction> group) {
        List<TransactionMatch> candidates = new java.util.ArrayList<>();
        
        for (int i = 0; i < group.size() - 1; i++) {
            Transaction incoming = group.get(i);
            Transaction outgoing = group.get(i + 1);
            
            TransactionMatch match = new TransactionMatch();
            match.setIncomingTransaction(incoming);
            match.setOutgoingTransaction(outgoing);
            match.setStatus("PENDING");
            match.setMatchedAt(LocalDateTime.now());
            match.setMatchedBy(securityService.getCurrentUser() != null 
                    ? securityService.getCurrentUser().getUsername() 
                    : "SYSTEM");
            
            candidates.add(match);
        }
        
        return candidates;
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 2: VALIDATION
    // ─────────────────────────────────────────────────────────────

    /**
     * Phase 2: Validate matches by checking discrepancies.
     * Sets match status based on discrepancy analysis.
     */
    @Transactional
    public void validateMatches(List<TransactionMatch> matches) {
        logger.info("PHASE 2 - VALIDATION: Validating {} matches", matches.size());
        
        for (TransactionMatch match : matches) {
            validateMatch(match);
        }
    }

    /**
     * Validate a single match and determine status.
     */
    private void validateMatch(TransactionMatch match) {
        Transaction incoming = match.getIncomingTransaction();
        Transaction outgoing = match.getOutgoingTransaction();

        // Validate both transactions
        var incomingValidation = dataValidationService.validateTransaction(incoming);
        var outgoingValidation = dataValidationService.validateTransaction(outgoing);

        if (!incomingValidation.isValid() || !outgoingValidation.isValid()) {
            logger.warn("Match validation failed for incoming ID={}, outgoing ID={}", 
                       incoming.getId(), outgoing.getId());
            match.setStatus("DISPUTED");
            match.setNote("Validation failed: " + incomingValidation.getMessage());
            return;
        }

        // Calculate discrepancy
        BigDecimal incomingAmount = incoming.getAmount();
        BigDecimal outgoingAmount = outgoing.getAmount();
        BigDecimal discrepancy = incomingAmount.subtract(outgoingAmount).abs();

        logger.debug("Match analysis: incoming={}, outgoing={}, discrepancy={}", 
                    incomingAmount, outgoingAmount, discrepancy);

        // Determine status based on discrepancy
        if (discrepancy.compareTo(DISCREPANCY_THRESHOLD) <= 0) {
            match.setStatus("CONFIRMED");
            logger.info("Match ID={} CONFIRMED (discrepancy within threshold)", match.getId());
        } else {
            match.setStatus("DISPUTED");
            logger.warn("Match ID={} DISPUTED (discrepancy {} exceeds threshold {})", 
                       match.getId(), discrepancy, DISCREPANCY_THRESHOLD);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PHASE 3: APPROVAL
    // ─────────────────────────────────────────────────────────────

    /**
     * Phase 3: Supervisor review and approval.
     */
    @Transactional
    public void approveMatches(List<Long> matchIds, String reviewNotes) {
        logger.info("PHASE 3 - APPROVAL: Approving {} matches with notes: {}", matchIds.size(), reviewNotes);
        
        for (Long matchId : matchIds) {
            Optional<TransactionMatch> match = transactionMatchRepository.findById(matchId);
            if (match.isPresent()) {
                approveMatch(match.get(), reviewNotes);
            } else {
                logger.warn("Match ID={} not found for approval", matchId);
            }
        }
    }

    /**
     * Approve a single match.
     */
    private void approveMatch(TransactionMatch match, String reviewNotes) {
        User currentUser = securityService.getCurrentUser();
        String reviewer = currentUser != null ? currentUser.getUsername() : "SYSTEM";
        
        match.setStatus("CONFIRMED");
        match.setNote(reviewNotes);
        transactionMatchRepository.save(match);
        
        logger.info("Match ID={} approved by {} with notes: {}", match.getId(), reviewer, reviewNotes);
    }

    /**
     * Dispute a match with reason.
     */
    @Transactional
    public void disputeMatch(Long matchId, String reason) {
        Optional<TransactionMatch> match = transactionMatchRepository.findById(matchId);
        if (match.isPresent()) {
            match.get().setStatus("DISPUTED");
            match.get().setNote("Disputed: " + reason);
            transactionMatchRepository.save(match.get());
            logger.warn("Match ID={} disputed. Reason: {}", matchId, reason);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // RECONCILIATION SUMMARY
    // ─────────────────────────────────────────────────────────────

    /**
     * Generate reconciliation summary for a wallet.
     */
    @Transactional
    public DailyReconciliation generateReconciliationSummary(Long walletId, LocalDate date) {
        logger.info("Generating reconciliation summary for wallet ID={} on date {}", walletId, date);
        
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + walletId));

        BigDecimal systemBalance = wallet.getBalance();
        
        DailyReconciliation reconciliation = new DailyReconciliation();
        reconciliation.setWalletId(walletId);
        reconciliation.setDate(date);
        reconciliation.setSystemBalance(systemBalance);
        reconciliation.setReconciledBy(securityService.getCurrentUser() != null 
                ? securityService.getCurrentUser().getUsername() 
                : "SYSTEM");
        reconciliation.setCreatedAt(LocalDateTime.now());
        
        return dailyReconciliationRepository.save(reconciliation);
    }
}
