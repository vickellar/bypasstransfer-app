package com.bypass.bypasstransers.service;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.TransactionMatch;
import com.bypass.bypasstransers.repository.TransactionMatchRepository;
import com.bypass.bypasstransers.repository.TransactionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class TransactionMatchService {

    @Autowired
    private TransactionMatchRepository matchRepo;

    @Autowired
    private TransactionRepository txRepo;

    /**
     * Returns all transactions that are NOT yet part of any match
     * (neither as incoming nor as outgoing).
     */
    public List<Transaction> getUnmatchedTransactions() {
        List<Long> matchedIncomingIds = matchRepo.findAllMatchedIncomingIds();
        List<Long> matchedOutgoingIds = matchRepo.findAllMatchedOutgoingIds();

        Set<Long> matched = new HashSet<>();
        matched.addAll(matchedIncomingIds);
        matched.addAll(matchedOutgoingIds);

        List<Transaction> all = txRepo.findAll(Sort.by(Sort.Direction.DESC, "date"));
        if (matched.isEmpty()) {
            return all;
        }
        return all.stream()
                .filter(t -> !matched.contains(t.getId()))
                .toList();
    }

    /**
     * Returns ALL transactions (matched and unmatched) sorted by date desc.
     * Useful for populating the match form selects so admin can manually pick any pair.
     */
    public List<Transaction> getAllTransactions() {
        return txRepo.findAll(Sort.by(Sort.Direction.DESC, "date"));
    }

    /** Returns all existing matches, newest first. */
    public List<TransactionMatch> getAllMatches() {
        return matchRepo.findAll(Sort.by(Sort.Direction.DESC, "matchedAt"));
    }

    /**
     * Creates a match between an incoming and an outgoing transaction.
     *
     * @throws IllegalArgumentException if either transaction is not found or
     *                                   the pair is already matched.
     */
    public TransactionMatch createMatch(Long incomingId, Long outgoingId,
                                       String description, String note,
                                       String operator) {

        if (incomingId.equals(outgoingId)) {
            throw new IllegalArgumentException("Incoming and outgoing transaction must be different.");
        }

        Transaction incoming = txRepo.findById(incomingId)
                .orElseThrow(() -> new IllegalArgumentException("Incoming transaction not found: " + incomingId));
        Transaction outgoing = txRepo.findById(outgoingId)
                .orElseThrow(() -> new IllegalArgumentException("Outgoing transaction not found: " + outgoingId));

        if (matchRepo.existsByIncomingTransactionOrOutgoingTransaction(incoming, outgoing)) {
            throw new IllegalArgumentException(
                    "One or both transactions are already part of an existing match.");
        }

        TransactionMatch match = new TransactionMatch();
        match.setIncomingTransaction(incoming);
        match.setOutgoingTransaction(outgoing);
        match.setDescription(description);
        match.setNote(note);
        match.setMatchedBy(operator);
        match.setMatchedAt(LocalDateTime.now());
        match.setStatus("PENDING");

        return matchRepo.save(match);
    }

    /**
     * Updates the status of a match (PENDING → CONFIRMED or DISPUTED).
     */
    public TransactionMatch updateStatus(Long matchId, String status) {
        TransactionMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
        match.setStatus(status);
        return matchRepo.save(match);
    }

    /** Deletes a match (unlinks the pair). */
    public void deleteMatch(Long matchId) {
        matchRepo.deleteById(matchId);
    }
}
