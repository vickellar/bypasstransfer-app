package com.bypass.bypasstransers.model;

import com.bypass.bypasstransers.enums.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column
    private double balance;

    @Column(name = "name")
    private String accountType;

    @Column(name = "locked", nullable = false, columnDefinition = "boolean default false")
    private boolean locked = false;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

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
    
    
}