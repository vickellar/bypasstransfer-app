package com.bypass.bypasstransers.model;

import com.bypass.bypasstransers.enums.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "account")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency")
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(name = "balance")
    private double balance;

    @Column(name = "name")
    private String accountType;

    @Column(name = "locked", nullable = false, columnDefinition = "boolean default false")
    private boolean locked = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "transfer_fee", columnDefinition = "double precision default 0.0")
    private double transferFee = 0.0;

    @Column(name = "low_balance_threshold", columnDefinition = "double precision default 50.0")
    private double lowBalanceThreshold = 50.0;

    @Column(name = "low_balance_alert_sent", nullable = false, columnDefinition = "boolean default false")
    private boolean lowBalanceAlertSent = false;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    // debit reduces balance; throws if insufficient
    public void debit(double amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be positive");
        if (this.balance < amount) throw new IllegalArgumentException("insufficient wallet balance");
        this.balance -= amount;
    }

    // credit increases balance
    public void credit(double amount) {
        if (amount < 0) throw new IllegalArgumentException("amount must be positive");
        this.balance += amount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public double getLowBalanceThreshold() {
        return lowBalanceThreshold;
    }

    public void setLowBalanceThreshold(double lowBalanceThreshold) {
        this.lowBalanceThreshold = lowBalanceThreshold;
    }

    public boolean isLowBalanceAlertSent() {
        return lowBalanceAlertSent;
    }

    public void setLowBalanceAlertSent(boolean lowBalanceAlertSent) {
        this.lowBalanceAlertSent = lowBalanceAlertSent;
    }

    public double getTransferFee() {
        return transferFee;
    }

    public void setTransferFee(double transferFee) {
        this.transferFee = transferFee;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }
}