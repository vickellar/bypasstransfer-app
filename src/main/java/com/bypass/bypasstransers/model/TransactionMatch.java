package com.bypass.bypasstransers.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents a matched pair: one incoming transaction (e.g., RUB received)
 * linked to one outgoing transaction (e.g., USD sent). Used by admins/supervisors
 * to reconcile end-of-day flows.
 */
@Entity
@Table(name = "transaction_match")
public class TransactionMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The incoming side – money received (e.g., RUB received by Russian agent). */
    @ManyToOne
    @JoinColumn(name = "incoming_transaction_id")
    private Transaction incomingTransaction;

    /** The outgoing side – money sent out (e.g., USD disbursed). */
    @ManyToOne
    @JoinColumn(name = "outgoing_transaction_id")
    private Transaction outgoingTransaction;

    /** Username of the person who created this match. */
    private String matchedBy;

    /** When the match was created. */
    private LocalDateTime matchedAt;

    /** Free-text description explaining the purpose or context of this match. */
    @Column(length = 1000)
    private String description;

    /** Short internal note (e.g., "verified by manager"). */
    @Column(length = 500)
    private String note;

    /**
     * Match status: PENDING (just created), CONFIRMED (reviewed and confirmed),
     * DISPUTED (amounts or accounts don't add up).
     */
    private String status = "PENDING";

    // ─── Getters / Setters ────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Transaction getIncomingTransaction() { return incomingTransaction; }
    public void setIncomingTransaction(Transaction incomingTransaction) {
        this.incomingTransaction = incomingTransaction;
    }

    public Transaction getOutgoingTransaction() { return outgoingTransaction; }
    public void setOutgoingTransaction(Transaction outgoingTransaction) {
        this.outgoingTransaction = outgoingTransaction;
    }

    public String getMatchedBy() { return matchedBy; }
    public void setMatchedBy(String matchedBy) { this.matchedBy = matchedBy; }

    public LocalDateTime getMatchedAt() { return matchedAt; }
    public void setMatchedAt(LocalDateTime matchedAt) { this.matchedAt = matchedAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
