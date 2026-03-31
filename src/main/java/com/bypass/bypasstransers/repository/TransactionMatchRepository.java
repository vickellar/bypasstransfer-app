package com.bypass.bypasstransers.repository;

import com.bypass.bypasstransers.model.Transaction;
import com.bypass.bypasstransers.model.TransactionMatch;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionMatchRepository extends JpaRepository<TransactionMatch, Long> {

    /** Check if a transaction is already part of any match (as incoming or outgoing). */
    boolean existsByIncomingTransactionOrOutgoingTransaction(
            Transaction incoming, Transaction outgoing);

    /** Find all matches in sort order. */
    List<TransactionMatch> findAll(Sort sort);

    /** Find matches where a specific transaction is the incoming side. */
    List<TransactionMatch> findByIncomingTransaction(Transaction incoming);

    /** Find matches where a specific transaction is the outgoing side. */
    List<TransactionMatch> findByOutgoingTransaction(Transaction outgoing);

    /** Get all matched incoming transaction IDs. */
    @Query("SELECT m.incomingTransaction.id FROM TransactionMatch m")
    List<Long> findAllMatchedIncomingIds();

    /** Get all matched outgoing transaction IDs. */
    @Query("SELECT m.outgoingTransaction.id FROM TransactionMatch m")
    List<Long> findAllMatchedOutgoingIds();

    /** Check if this exact pair already exists. */
    boolean existsByIncomingTransactionIdAndOutgoingTransactionId(
            @Param("incomingId") Long incomingId,
            @Param("outgoingId") Long outgoingId);
}
