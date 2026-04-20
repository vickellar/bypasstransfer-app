package com.bypass.bypasstransers.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
public class Account {

    public Account() {
        this.lowBalanceAlertSent = false; // ensure default value
    }

    public Account(Long id, String name, BigDecimal balance, BigDecimal transferFee) {
        this.id = id;
        this.name = name;
        this.balance = (balance != null) ? balance : BigDecimal.ZERO;
        this.transferFee = (transferFee != null) ? transferFee : BigDecimal.ZERO;
        this.lowBalanceThreshold = new BigDecimal("50"); // default
        this.lowBalanceAlertSent = false; // default
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;
    @Column(name = "balance", precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;
    @Column(name = "transfer_fee", precision = 19, scale = 4)
    private BigDecimal transferFee = BigDecimal.ZERO;

    @ManyToOne
    private User owner;
    
    @Column(name = "low_balance_threshold", precision = 19, scale = 4)
    private BigDecimal lowBalanceThreshold = new BigDecimal("50"); // default
    
    @Column(name = "low_balance_alert_sent", nullable = false, columnDefinition = "boolean default false")
    private boolean lowBalanceAlertSent = false; // default

    @Version
    private Long version;

    // getters & setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getTransferFee() {
        return transferFee;
    }

    public void setTransferFee(BigDecimal transferFee) {
        this.transferFee = transferFee;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public BigDecimal getLowBalanceThreshold() {
        return lowBalanceThreshold;
    }

    public void setLowBalanceThreshold(BigDecimal lowBalanceThreshold) {
        this.lowBalanceThreshold = lowBalanceThreshold;
    }

    public boolean isLowBalanceAlertSent() {
        return lowBalanceAlertSent;
    }

    public void setLowBalanceAlertSent(boolean lowBalanceAlertSent) {
        this.lowBalanceAlertSent = lowBalanceAlertSent;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}