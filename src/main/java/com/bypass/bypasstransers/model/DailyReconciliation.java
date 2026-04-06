package com.bypass.bypasstransers.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class DailyReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "daily_recon_seq")
    @SequenceGenerator(name = "daily_recon_seq", sequenceName = "daily_reconciliation_id_seq", allocationSize = 1)
    private Long id;

    private LocalDate date;
    private double systemBalance;
    private double actualBalance;
    private double difference;
    private String accountName;
    private Long walletId;

    // --- New fields for weekly reconciliation workflow ---

    /** Username of the staff who performed the reconciliation */
    @Column(name = "reconciled_by")
    private String reconciledBy;

    /** Status: PENDING, APPROVED, FLAGGED, REVIEWED */
    @Column(name = "status", length = 20)
    private String status = "PENDING";

    /** Notes explaining discrepancies */
    @Column(name = "notes", length = 1000)
    private String notes;

    /** Username of the supervisor/admin who reviewed */
    @Column(name = "reviewed_by")
    private String reviewedBy;

    /** Review notes from supervisor */
    @Column(name = "review_notes", length = 1000)
    private String reviewNotes;

    /** When the reconciliation was created */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** ISO week number (1-53) for weekly cycle */
    @Column(name = "week_number")
    private Integer weekNumber;

    /** Year for the week-based reconciliation */
    @Column(name = "recon_year")
    private Integer year;

    /** When the reconciliation was reviewed */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getSystemBalance() {
        return systemBalance;
    }

    public void setSystemBalance(double systemBalance) {
        this.systemBalance = systemBalance;
    }

    public double getActualBalance() {
        return actualBalance;
    }

    public void setActualBalance(double actualBalance) {
        this.actualBalance = actualBalance;
    }

    public double getDifference() {
        return difference;
    }

    public void setDifference(double difference) {
        this.difference = difference;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public String getReconciledBy() {
        return reconciledBy;
    }

    public void setReconciledBy(String reconciledBy) {
        this.reconciledBy = reconciledBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
