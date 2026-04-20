package com.bypass.bypasstransers.model;

import com.bypass.bypasstransers.enums.TransactionType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "offline_transactions")
public class OfflineTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "fee", precision = 19, scale = 4)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "net_amount", precision = 19, scale = 4)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType type;

    @Column(name = "from_account")
    private String fromAccount;

    @Column(name = "to_account")
    private String toAccount;

    @Column(name = "sync_status", nullable = false)
    private String syncStatus = "PENDING"; // PENDING, SYNCED, FAILED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "offline_recorded_at", nullable = false)
    private LocalDateTime offlineRecordedAt;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "location")
    private String location;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "transaction_ref")
    private String transactionRef;

    @Column(name = "reference_balance", precision = 19, scale = 4)
    private BigDecimal referenceBalance = BigDecimal.ZERO;

    // Constructors
    public OfflineTransaction() {
        this.createdAt = LocalDateTime.now();
        this.offlineRecordedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        if (amount != null) {
            this.netAmount = amount;
        }
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee != null ? fee : BigDecimal.ZERO;
        if (this.amount != null) {
            this.netAmount = this.amount;
        }
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public void setToAccount(String toAccount) {
        this.toAccount = toAccount;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(LocalDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }

    public LocalDateTime getOfflineRecordedAt() {
        return offlineRecordedAt;
    }

    public void setOfflineRecordedAt(LocalDateTime offlineRecordedAt) {
        this.offlineRecordedAt = offlineRecordedAt;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public BigDecimal getReferenceBalance() {
        return referenceBalance;
    }

    public void setReferenceBalance(BigDecimal referenceBalance) {
        this.referenceBalance = referenceBalance;
    }
}
