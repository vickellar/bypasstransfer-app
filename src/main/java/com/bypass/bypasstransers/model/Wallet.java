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

@Entity
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column
    private double balance;

    @ManyToOne
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